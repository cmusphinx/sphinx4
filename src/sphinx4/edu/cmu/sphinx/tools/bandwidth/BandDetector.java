/*
 * Copyright 1999-2013 Carnegie Mellon University.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.bandwidth;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

/**
 * A simple energy-based detector for upsampled audio. Could be used to detect
 * bandwidth issues leading to the accuracy issues.
 * 
 * The detector simply looks for energies in different mel bands and using the
 * threshold it decides if we have high-energy signal.
 * 
 * It doesn't make a decision on files less than 2 seconds because this seems to
 * be unreliable.
 * 
 */
public class BandDetector {

    static final int bands = 40;
    // Filter 35 starts from 5531 Hz
    static final int lowEdge = 35;
    // Main threshold, selected during the experiments
    static final double noSignalCutoff = 2e-2;
    // Don't expect to detect that small
    static final int minLength = 150;
    // Don't care if intensity is very low
    static final double lowIntensity = 1e+5;
    // To avoid zero energy
    static final double lowestIntensity = 1e-5;

    private FrontEnd frontend;
    private AudioFileDataSource source;

    public BandDetector() {

        source = new AudioFileDataSource(320, null);
        RaisedCosineWindower windower = new RaisedCosineWindower(0.97f,
                25.625f, 10.0f);
        DiscreteFourierTransform fft = new DiscreteFourierTransform(512, false);
        MelFrequencyFilterBank filterbank = new MelFrequencyFilterBank(200,
                8000, bands);

        ArrayList<DataProcessor> list = new ArrayList<DataProcessor>();
        list.add(source);
        list.add(windower);
        list.add(fft);
        list.add(filterbank);

        frontend = new FrontEnd(list);
    }

    public static void main(String args[]) throws FileNotFoundException {

        if (args.length < 1) {
            System.out
                    .println("Usage: Detector <filename.wav> or Detector <filelist>");
            return;
        }

        if (args[0].endsWith(".wav")) {
            BandDetector detector = new BandDetector();
            System.out.println("Bandwidth for " + args[0] + " is "
                    + detector.bandwidth(args[0]));
        } else {
            BandDetector detector = new BandDetector();
            Scanner s = new Scanner(new File(args[0]));
            while (s.hasNextLine()) {
                String line = s.nextLine().trim();
                if (detector.bandwidth(line))
                    System.out.println("Bandwidth for " + line + " is low");
            }
            s.close();
        }
        return;
    }

    public boolean bandwidth(String file) {

        source.setAudioFile(new File(file), "");

        Data data;
        int count = 0;
        double energy[] = new double[bands];

        while ((data = frontend.getData()) != null) {
            if (data instanceof DoubleData) {

                double maxIntensity = lowestIntensity;
                double[] frame = ((DoubleData) data).getValues();

                for (int i = 0; i < bands; i++)
                    maxIntensity = Math.max(maxIntensity, frame[i]);

                if (maxIntensity < lowIntensity) {
                    continue;
                }

                for (int i = 0; i < bands; i++) {
                    energy[i] = Math.max(frame[i] / maxIntensity, energy[i]);
                }
                count++;
            }
        }

        boolean lowBandwidth = count > minLength ? true : false;
        for (int i = lowEdge; i < bands; i++) {
            if (energy[i] > noSignalCutoff) {
                lowBandwidth = false;
                break;
            }
        }
        return lowBandwidth;
    }
}
