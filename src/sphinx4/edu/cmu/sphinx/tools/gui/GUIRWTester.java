/*
 * GUIRWTester.java
 *
 * Created on November 8, 2006, 1:51 PM
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

import edu.cmu.sphinx.tools.gui.reader.*;
import edu.cmu.sphinx.tools.gui.writer.*;
import java.lang.Exception;
import java.io.File;

/**
 * This is the main driver for GUI Read-Write test
 * The main purpose is to verify that read and write operations are 
 * working 
 *
 * @author Ariani
 */
public class GUIRWTester {
    
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            //reading operation
            XMLConfigReader xmlReader = XMLConfigReader.getInstance();
            ConfigProperties cp = xmlReader.read(new File("helloworld.config.xml"));
            
            //writing operation
            XMLConfigWriter xmlWriter = XMLConfigWriter.getInstance();
            xmlWriter.writeOutput(cp,new File("hello1.config.xml"));
            
            // the output file should have exact same data as the input
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        
    }
    
}
