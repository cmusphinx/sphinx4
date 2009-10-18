/*
 * GUIMainDriver.java
 *
 * Created on October 30, 2006, 4:28 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui;

/**
 * Main driver for GUI class
 *
 * @author Ariani
 */
public class GUIMainDriver {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // the main coordinator is GUIMediator
                    new GUIMediator().execute();
                }catch(Exception e){
                    System.out.println("Error : " + e.getMessage());
                }
            }
        });
    }
}
