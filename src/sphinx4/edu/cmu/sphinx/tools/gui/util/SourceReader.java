/*
 * SourceReader.java
 *
 * Created on February 14, 2007, 11:25 AM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * It is a test class for reading source code
 *
 * @author Ariani
 */
public class SourceReader {
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       
        // this part is used to test the system properties and display them all
        Properties props = System.getProperties();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String param = (String) e.nextElement();
            String value = props.getProperty(param);
            System.out.println("["+param+"]="+value);
        }
        System.out.println("### path value is "+ props.getProperty("java.class.path"));
                    
        try {
            
            String classname = new String("C:/project/source/JavaApplication2/sphinx4-1.0beta/edu/cmu/sphinx/tools/gui/guibuild.xml");   
            BufferedReader br = new BufferedReader(new FileReader(classname));
                 System.out.println("File found");
            String thisline;
            while ( (thisline = br.readLine()) != null ){
                System.out.println(thisline);
            }
            br.close();
        }catch(IOException e){
            System.err.println("IO error :"+e.getMessage());
        }
    }
    
}
