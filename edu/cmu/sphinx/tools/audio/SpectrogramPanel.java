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

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.util.SphinxProperties;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Converts a set of log magnitude Spectrum data into a graphical
 * representation.
 */
public class SpectrogramPanel extends JPanel {
    /**
     * Where the spectrogram will live.
     */
    private BufferedImage spectrogram = null;

    /**
     * Offset factor - what will be subtracted from the image to
     * adjust for noise level.
     */
    private double offsetFactor;

    /**
     * The audio data.
     */
    private AudioData audio;
    
    /**
     * The SphinxProperties.
     */
    private SphinxProperties props;
    
    /**
     * Creates a new SpectrogramPanel for the given AudioData.
     *
     * @param props the SphinxProperties for setting up the front end
     * @param audio the AudioData
     */
    public SpectrogramPanel(SphinxProperties sphinxProps,
                            AudioData audioData) {
        audio = audioData;
        props = sphinxProps;
	audio.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent event) {
                    computeSpectrogram();
                }
	    });
        computeSpectrogram();
    }

    /**
     * Actually creates the Spectrogram image.
     */
    private void computeSpectrogram() {
        try {
            AudioDataInputStream is = new AudioDataInputStream(audio);
            StreamDataSource audioSource = new StreamDataSource();
	    audioSource.initialize("StreamDataSource", null, props, null);
	    audioSource.setInputStream(is, "live audio");

	    Collection names = FrontEndFactory.getNames(props);
	    assert (names.size() == 1);
	    String feName = (String) names.iterator().next();

            FrontEnd frontEnd = FrontEndFactory.getFrontEnd(props, feName);
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

            int width = intensitiesList.size();
            int height = ((double[]) intensitiesList.get(0)).length;
            int maxYIndex = height - 1;
            Dimension d = new Dimension(width, height);
        
            setMinimumSize(d);
            setMaximumSize(d);        
            setPreferredSize(d);

            /* Create the image for displaying the data.
             */
            spectrogram = new BufferedImage(width,
                                            height,
                                            BufferedImage.TYPE_INT_RGB);
            
            /* Set scaleFactor so that the maximum value, after removing
             * the offset, will be 0xff.
             */
            double scaleFactor = ((0xff + offsetFactor) / maxIntensity);
            
            for (int i = 0; i < width; i++) {                
                double[] intensities = (double[]) intensitiesList.get(i);
                for (int j = maxYIndex; j >= 0; j--) {
                    
                    /* Adjust the grey value to make a value of 0 to mean
                     * white and a value of 0xff to mean black.
                     */
                    int grey = (int) (intensities[j] * scaleFactor
                                      - offsetFactor);
                    grey = Math.max(grey, 0);
                    grey = 0xff - grey; 
                    
                    /* Turn the grey into a pixel value.
                     */
                    int pixel = ((grey << 16) & 0xff0000)
                        | ((grey << 8) & 0xff00)
                        | (grey & 0xff);
                    
                    spectrogram.setRGB(i, maxYIndex - j, pixel);
                }
            }
            repaint(new Rectangle(width, height));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the offset factor used to calculate the greyscale
     * values from the intensities.  This also calculates and
     * populates all the greyscale values in the image.
     *
     * @param offsetFactor the offset factor used to calculate the
     * greyscale values from the intensities; this is used to adjust
     * the level of background noise that shows up in the image
     */
    public void setOffsetFactor(double offsetFactor) {
        this.offsetFactor = offsetFactor;
        computeSpectrogram();
    }
    
    /** 
     * Paint the component.  This will be called by AWT/Swing.
     * 
     * @param g The <code>Graphics</code> to draw on.
     */
    public void paint(Graphics g) {
        if(spectrogram != null) {
            g.drawImage(spectrogram, 0, 0, (ImageObserver) null);
        }
    }
}
