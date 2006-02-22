package edu.cmu.sphinx.tools.corpusEditor;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.tools.audio.AudioDataInputStream;
import edu.cmu.sphinx.tools.audio.SpectrogramPanel;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ReplicateScaleFilter;
import java.util.ArrayList;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Jan 24, 2006
 * Time: 9:53:23 PM
 */
public class WordSpectrogramPanel extends SpectrogramPanel {

    private Word word;
    private double maxIntensity;
    private double minIntensity;
    private double [] pitch;
    private double [] energy;

    public void setWord(ConfigurationManager cm, Word word) {
        try {
            this.word = word;
            //zoomSet(20.0f);

            frontEnd = (FrontEnd) cm.lookup("spectrogramFrontEnd");
            dataSource = (StreamDataSource) cm.lookup("streamDataSource");

            this.audio = word.getAudio();
            this.pitch = word.getPitch();
            this.energy = word.getEnergy();

            audio.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    computeSpectrogram();
                }
            });

            setOffset(0.0);

        } catch (PropertyException e) {
            throw new Error(e);
        } catch (InstantiationException e) {
            throw new Error(e);
        }
    }

    public void setZoom(double zoom) {
        zoomSet((float) zoom);
    }


    public void setOffset(double v) {
        setOffsetFactor(((maxIntensity - minIntensity) * v) * 100); //+ minIntensity );
    }

    protected void computeSpectrogram() {
        try {
            AudioDataInputStream is = new AudioDataInputStream(audio);
            dataSource.setInputStream(is, "live audio");

            /* Run through all the spectra one at a time and convert
            * them to an log intensity value.
            */
            ArrayList intensitiesList = new ArrayList();
            maxIntensity = Double.MIN_VALUE;
            minIntensity = Double.MAX_VALUE;
            Data spectrum = frontEnd.getData();

            while (!(spectrum instanceof DataEndSignal)) {
                if (spectrum instanceof DoubleData) {
                    double[] spectrumData = ((DoubleData) spectrum).getValues();
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
                        if (intensities[i] < minIntensity) {
                            minIntensity = intensities[i];
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
                int p;
                int e;

                int m = i % 10;
                p = (int)((10-m)*(pitch[i/10]/2) + m*pitch[(i/10)+1]/2)/10;
                e = (int)((10-m)*(energy[i/10]/100000000) + m*energy[(i/10)+1]/100000000)/10;

                //p = (int)pitch[i]/2;
                //e = (int)energy[i]/10000000;
                
                spectrogram.setRGB(i, Math.min(maxYIndex - p,maxYIndex), 0xff);
                spectrogram.setRGB(i, Math.min(maxYIndex - e,maxYIndex), 0xff0000);
            }
            ImageFilter scaleFilter =
                    new ReplicateScaleFilter((int) (zoom * width), height);
            scaledSpectrogram =
                    createImage(new FilteredImageSource(spectrogram.getSource(),
                            scaleFilter));
            Dimension sz = getSize();
            repaint(0, 0, 0, sz.width - 1, sz.height - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    protected void computeSpectrogram() {
        try {
            AudioDataInputStream is = new AudioDataInputStream(audio);
            dataSource.setInputStream(is, "live audio");

            // Run through all the spectra one at a time and convert
            // them to an log intensity value.
            ArrayList intensitiesList = new ArrayList();
            double maxIntensity = Double.MIN_VALUE;
            double minIntensity = Double.MAX_VALUE;
            Data spectrum = frontEnd.getData();

            while (!(spectrum instanceof DataEndSignal)) {
                if (spectrum instanceof DoubleData) {
                    double[] spectrumData = ((DoubleData) spectrum).getValues();
                    double[] intensities = new double[spectrumData.length];
                    for (int i = 0; i < intensities.length; i++) {
                        // A very small intensity is, for all intents
                        // and purposes, the same as 0.
                        intensities[i] = Math.max(Math.log(spectrumData[i]),
                                0.0);
                        if (intensities[i] > maxIntensity) {
                            maxIntensity = intensities[i];
                        }
                       if (intensities[i] < minIntensity) {
                            minIntensity = intensities[i];
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

            // Create the image for displaying the data.
            spectrogram = new BufferedImage(width,
                    height,
                    BufferedImage.TYPE_INT_RGB);

            // Set scaleFactor so that the maximum value, after removing
            // the offset, will be 0xff.
            double scaleFactor = ((0xff + offsetFactor) / maxIntensity);

            int half = (int)(maxIntensity+minIntensity)/2;

            for (int i = 0; i < width; i++) {
                double[] intensities = (double[]) intensitiesList.get(i);
                for (int j = maxYIndex; j >= 0; j--) {

                    // Adjust the grey value to make a value of 0 to mean
                    // white and a value of 0xff to mean black.
                    int mag = (int) (intensities[j] * scaleFactor - offsetFactor);

                    int red = mag;
                    int blue = 0; // (int)maxIntensity - mag;
                    int green;

                    if( mag < half ) {
                        red = 0;
                        green = (int)maxIntensity - (2*mag);
                    } else {
                        green = (2*(mag-half));
                        blue = 0;
                    }


                    // Turn the grey into a pixel value.
                    int pixel = ((red << 16) & 0xff0000)
                            | ((green << 8) & 0xff00)
                            | (blue & 0xff);

                    spectrogram.setRGB(i, maxYIndex - j, pixel);
                }
            }
            ImageFilter scaleFilter =
                    new ReplicateScaleFilter((int) (zoom * width), height);
            scaledSpectrogram =
                    createImage(new FilteredImageSource(spectrogram.getSource(),
                            scaleFilter));
            Dimension sz = getSize();
            repaint(0, 0, 0, sz.width - 1, sz.height - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
