
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.util.Arrays;


/**
 * Plots energy values of Cepstrum to stdout. 
 * The plots look like the following, one line per Cepstrum. The
 * energy value for that particular Cepstrum is printed at the end of
 * the line.
 * <p>
 * <code>
<br>......7
<br>......7
<br>Cepstrum: SPEECH_START
<br>......7
<br>.......8
<br>......7
<br>.......8
<br>.......8
<br>........9
<br>............14
<br>...........13
<br>...........13
<br>...........13
<br>.............15
<br>.............15
<br>..............16
<br>..............16
<br>..............16
<br>.............15
<br>............14
<br>............14
<br>............14
<br>............14
<br>.............15
<br>..............16
<br>...............17
<br>...............17
<br>...............17
<br>...............17
<br>...............17
<br>...............17
<br>..............16
<br>.............15
<br>............14
<br>............14
<br>............14
<br>...........13
<br>........9
<br>.......8
<br>......7
<br>......7
<br>......7
<br>Cepstrum: SPEECH_END
<br>......7
</code>
 */
public class EnergyPlotter {

    private String[] plots;


    /**
     * Constructs an EnergyPlotter.
     *
     * @param maxEnergy the maximum energy value
     */
    public EnergyPlotter(int maxEnergy) {
        buildPlots(maxEnergy);
    }


    /**
     * Builds the strings for the plots.
     *
     * @param maxEnergy the maximum energy value
     */
    private void buildPlots(int maxEnergy) {
        plots = new String[maxEnergy+1];
        for (int i = 0; i < maxEnergy+1; i++) {
            char[] plot = new char[i];
            Arrays.fill(plot, '.');
            if (i > 0) {
                if (i < 10) {
                    plot[plot.length - 1] = (char) ('0' + i);
                } else {
                    plot[plot.length - 2] = '1';
                    plot[plot.length - 1] = (char) ('0' + (i - 10));
                }
            }
            plots[i] = new String(plot);
        }
    }

    
    /**
     * Plots the energy values of the given Cepstrum to System.out.
     * If the Cepstrum contains a signal, it prints the signal.
     *
     * @param cepstrum the Cepstrum to plot
     */
    public void plot(Cepstrum cepstrum) {
        if (cepstrum != null) {
            if (cepstrum.hasContent()) {
                int energy = (int) cepstrum.getEnergy();
                System.out.println(getPlot(energy));
            } else {
                System.out.println(cepstrum.toString());
            }
        }
    }


    /**
     * Returns the corresponding plot String for the given energy value.
     *
     * @return energy the energy value
     */
    private String getPlot(int energy) {
        return plots[energy];
    }
}
