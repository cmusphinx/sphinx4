/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.audio;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import edu.cmu.sphinx.util.SphinxProperties;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * Records and displays the waveform and spectrogram of an audio signal.
 */
public class AudioTool {
    static final String CONTEXT = "AudioTool";
    static SphinxProperties props;
    static float windowSizeInMs;
    static float windowShiftInMs;
    static float windowShiftInSamples;
    static AudioData audio = null;
    static SpectrogramPanel spectrogram;

    /**
     * Attempts to read an audio file using the Java Sound APIs.  If
     * this file isn't a typical audio file, then this returns a
     * null.  Otherwise, it converts the data into a 16kHz 16-bit signed
     * PCM big endian clip.
     */
    static private AudioData readAudioFile(URL url)
        throws MalformedURLException, IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            AudioFormat format = ais.getFormat();
            return new AudioData(ais);
        } catch (UnsupportedAudioFileException e) {
            return null;
        }
    }

    /**
     * Reads the given stream in as 16kHz 16-bit signed PCM audio data
     * and returns an audio clip.
     */
    static private AudioData readRawFile(URL url)
        throws MalformedURLException, IOException {
        AudioFormat format = new AudioFormat(16000.0f, // sample rate
                                             16,       // sample size
                                             1,        // channels (1 == mono)
                                             true,     // signed
                                             true);    // big endian
        InputStream stream = url.openStream();
        short[] audioData = RawReader.readAudioData(stream, format);
        stream.close();
        return new AudioData(audioData, 16000.0f);
    }

    /**
     * Whenever the audio changes, this creates a new front end and
     * recreates the power spectrum for the audio.  It then manages
     * a SpectrogramPanel with this new data.
     */
    static private void updateSpectrogram() {
        try {
            /*
	     * Create a new front end and get the power spectrum.
             */
            AudioDataInputStream is = new AudioDataInputStream(audio);
            StreamDataSource audioSource = new StreamDataSource();
	    audioSource.initialize("StreamDataSource", null, props, null);
	    audioSource.setInputStream(is, "live audio");

	    Collection names = FrontEndFactory.getNames(props);
	    assert (names.size() == 1);
	    String feName = (String) names.iterator().next();

            FrontEnd frontEnd = FrontEndFactory.getFrontEnd(feName, props);
            frontEnd.setDataSource(audioSource);

            /* Run through all the spectra one at a time and convert
             * them to an log intensity value.
             */
            ArrayList intensitiesList = new ArrayList();
            double maxIntensity = Double.MIN_VALUE;
            Data spectrum = frontEnd.getData();
            
	    while (!(spectrum instanceof DataEndSignal)) {
                if (spectrum instanceof DoubleData) {
                    double[] spectrumData = ((DoubleData)spectrum).getValues();
                    double[] intensities = new double[spectrumData.length];
                    for (int i = 0; i < intensities.length; i++) {
                        /*
			 * A very small intensity is, for all intents
                         * and purposes, the same as 0.
                         */
                        intensities[i] = Math.max(Math.log(spectrumData[i]),
                                                  0.0);
                        if (intensities[i] > maxIntensity) {
                            maxIntensity = intensities[i];
                        }
                    }
                    intensitiesList.add(intensities);
		}
		spectrum = frontEnd.getData();
            }
            is.close();

            /* Now create or update the spectrogram.
             */
            if (spectrogram == null) {
                spectrogram = new SpectrogramPanel(
                    intensitiesList,
                    maxIntensity,
                    100.0);
            } else {
                spectrogram.setSpectrogram(
                    intensitiesList,
                    maxIntensity,
                    60.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Main method.
     *
     * @param argv argv[0] : SphinxProperties file
     *             argv[1] : The name of an audio file
     */
    static public void main(String[] args) {
        try {
            URL url = new File(args[0]).toURI().toURL();
            SphinxProperties.initContext(CONTEXT, url);
            props = SphinxProperties.getSphinxProperties(CONTEXT);
            windowSizeInMs = props.getFloat
		("sfe;" + RaisedCosineWindower.PROP_WINDOW_SIZE_MS,
		 RaisedCosineWindower.PROP_WINDOW_SIZE_MS_DEFAULT);
            windowShiftInMs = props.getFloat
		("sfe;" + RaisedCosineWindower.PROP_WINDOW_SHIFT_MS,
                 RaisedCosineWindower.PROP_WINDOW_SHIFT_MS_DEFAULT);
            
            if (args[0].indexOf(":") == -1) {
                url = new URL("file:" + args[1]);
            } else {
                url = new URL(args[0]);
            }
            
            audio = readAudioFile(url);
            if (audio == null) {
                audio = readRawFile(url);
            }

            /* Scale the width according to the size of the
             * spectrogram.
             */
            windowShiftInSamples = windowShiftInMs
                * audio.getAudioFormat().getSampleRate() / 1000.0f;
            AudioPanel wavePanel = new AudioPanel(
                audio, 1.0f / windowShiftInSamples, 0.004f);
            updateSpectrogram();

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(wavePanel, BorderLayout.NORTH);
            panel.add(spectrogram, BorderLayout.SOUTH);
            
	    JScrollPane scroller = new JScrollPane(panel);

            final AudioPlayer player = new AudioPlayer(audio);
            player.start();
            
            wavePanel.addKeyListener(new KeyListener() {
                RawRecorder recorder;
                public void keyPressed(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_SHIFT) {
                        try {
                            recorder = new RawRecorder(
                                new AudioFormat(16000.0f, // sample rate
                                                16,       // sample size
                                                1,        // chan. (1 == mono)
                                                true,     // signed
                                                true));   // big endian
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        recorder.start();
                    }
                }
                public void keyReleased(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_SHIFT) {
                        audio.setAudioData(recorder.stop());
                        updateSpectrogram();
                    }
                }
                public void keyTyped(KeyEvent evt) {
                    if (evt.getKeyChar() == ' ') {
                        player.play();
                    } else if (evt.getKeyChar() == 'a') {
                        System.out.println("clearing selection");
                        audio.setSelectionStart(-1);
                        audio.setSelectionEnd(-1);
                        updateSpectrogram();
                    } else if (evt.getKeyChar() == 'c') {
                        System.out.println("cropping");
                        audio.crop();
                        updateSpectrogram();
                    }
                }
            });
            
	    JFrame jframe = new JFrame("AudioTool");
            jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setContentPane(scroller);
            jframe.pack();
            jframe.setSize(640,540);
            jframe.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }       
}
