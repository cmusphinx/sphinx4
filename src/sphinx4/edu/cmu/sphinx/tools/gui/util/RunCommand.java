/*
 * RunCommand.java
 *
 * Created on February 5, 2007, 2:49 PM
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
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * This class is used to test the javadoc doclet feature
 *
 * @author Ariani
 */
public class RunCommand {
    
    public static BufferedReader runcommand(String command)throws IOException{        
            Process ls_proc = Runtime.getRuntime().exec(command);
            
            // get its output (your input) stream
            BufferedReader d = new BufferedReader(new InputStreamReader(ls_proc.getInputStream()));
            return d;
    }
    
    /** the main will print the javadoc of a specific class */
    public static void main (String[] args) {
        BufferedReader br = null;
        try {
            //br = runcommand("javadoc -doclet edu.cmu.sphinx.tools.gui.util.ExtractJavaDoc -sourcepath C:/project/source/JavaApplication2/sphinx4-1.0beta  edu.cmu.sphinx.linguist");
            //String commandLine = new String("C:/Program Files/Java/jdk1.5.0_09/bin/javadoc -doclet edu.cmu.sphinx.tools.gui.util.ExtractJavaDoc -classpath C:/project/source/JavaApplication2/sphinx4-1.0beta/build/classes/ -sourcepath C:/project/source/JavaApplication2/sphinx4-1.0beta  edu.cmu.sphinx.linguist");
            //String commandLine = new String("\"C:/Program Files/Java/jdk1.5.0_09/bin/javadoc\" -docletpath C:/project/source/JavaApplication2/sphinx4-1.0beta/build/classes/ -doclet edu.cmu.sphinx.tools.gui.util.ExtractJavaDoc  -sourcepath C:/project/source/JavaApplication2/sphinx4-1.0beta edu.cmu.sphinx.linguist");
            String commandLine = new String("javadoc -docletpath C:/project/source/latest/bld/classes/ -doclet edu.cmu.sphinx.tools.gui.util.DocletHelper  C:/project/source/latest/src/sphinx4/edu/cmu/sphinx/linguist/LinguistProcessor.java");
            //br = runcommand(commandLine);
            
            SysCommandExecutor cmdExecutor = SysCommandExecutor.getInstance(); 		   		
            int exitStatus = cmdExecutor.runCommand(commandLine);

            String cmdError = cmdExecutor.getCommandError();
            String cmdOutput = cmdExecutor.getCommandOutput(); 

           // System.out.println(cmdOutput);
           // System.err.println(cmdError);
            
             System.out.println(JavadocExtractor.getJavadocComment("edu.cmu.sphinx.instrumentation.ConfigMonitor",
                    "C:/project/source/latest/bld/classes/",
                     "C:/project/source/latest/src/sphinx4/",
                     "PROP_SHOW_CONFIG_AS_HTML"
                    ));
           
                                 
            }catch(Exception e){
               System.err.println("Exception "+e.getMessage());
            }
//        
//        if ( br != null){
//            try {
//                String retstring;
//                while ((retstring = br.readLine()) != null) {
//                     System.out.println(retstring);
//                } 
//            }catch(IOException e){
//                System.err.println("IOException 2nd part"+e.getMessage());
//            }
//        }
    }
}

