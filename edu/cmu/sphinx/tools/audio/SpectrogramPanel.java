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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;

import javax.swing.JPanel;

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
     * The list of log intensities from the spectra for the signal.
     */
    List intensitiesList;
    
    /**
     * Offset factor - what will be subtracted from the image to
     * adjust for noise level.
     */
    private double offsetFactor;

    /**
     * Maximum intensity - this is the maximum value that intensities
     * have reached. We need this to determine an automatic scaling
     * factor.
     */
    private double maxIntensity;

    /**
     * Creates a new Spectrogram display with the given intensity
     * data.  The data is expected to be a List of log magnitude
     * spectrum data.  Each element in the list is an array of
     * double values that represents the log magnitude of the
     * frequency intensities for each window of analysis.  The
     * log magnitude values are as follows:
     *
     * <br>ln((real * real) + (imaginary * imaginary))
     *  
     * <br>Where real and imaginary are the frequency intensity
     * values computed by the FFT.
     *
     * @param intensitiesList the log magnitude spectra data
     * @param max the maximum intensity of all the spectra data
     * @param offsetFactor the offset factor used to calculate the
     * greyscale values from the intensities; this is used to adjust
     * the level of background noise that shows up in the image
     */
    public SpectrogramPanel(List intensitiesList,
                            double maxIntensity,
                            double offsetFactor) {
        setSpectrogram(intensitiesList,
                       maxIntensity,
                       offsetFactor);
    }

    public void setSpectrogram(List intensitiesList,
                               double maxIntensity,
                               double offsetFactor) {

        /* We'll automatically adjust our size to the spectra we're
         * getting.
         */
        this.intensitiesList = intensitiesList;
        this.maxIntensity = maxIntensity;
        this.offsetFactor = offsetFactor;
        
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

        drawImage();
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
        drawImage();
    }
    
    private void drawImage() {
        int width = spectrogram.getWidth();
        int height = spectrogram.getHeight();
        int maxYIndex = height - 1;
        
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
                int grey = (int) (intensities[j] * scaleFactor - offsetFactor);
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
