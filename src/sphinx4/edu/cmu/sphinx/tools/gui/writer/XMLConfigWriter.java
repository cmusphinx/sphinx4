/*
 * XMLConfigWriter.java
 *
 * Created on October 30, 2006, 4:14 PM
 *
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.writer;
import edu.cmu.sphinx.tools.gui.ConfigProperties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;

/**
 * This class is a type of <code>GUIWriter</code>
 * that write output to .config.xml format
 *
 * @author Ariani
 */
public class XMLConfigWriter implements GUIWriter{

     /* this class uses Singleton pattern for the constructor */
      private XMLConfigWriter() {}

      private static class XMLWriterHolder {
          private static final XMLConfigWriter instance = new XMLConfigWriter();
      }
      
      /** 
       * Singleton constructor of <code>XMLConfigWriter</code> ; 
       * only one instance is created for the whole system
       *
       * @return reference to <code>XMLConfigWriter</code>
       */
      public static XMLConfigWriter getInstance(){
          return XMLWriterHolder.instance;
      }

      /**
       * This method is inherited from <code>GUIWriter</code> interface.
       * Will write output to .config.xml format 
       *
       * @param configProp Holds the property name-values
       * @param fFile Output file
       * @throws GUIWriterException writing error
       */
    @Override
    public boolean writeOutput(ConfigProperties configProp, File fFile) throws GUIWriterException{
        if (fFile == null || fFile.getName().trim().isEmpty()) /* no filename for output */
        {
            throw new GUIWriterException
                    ("No output filename specified",GUIWriterException.EXCEPTION_NO_FILENAME);
        }
        else /* start writing to config file */
        {
            
            try{
                ConfigConverter cc = ConfigConverter.getInstance();
                StringBuilder sb = cc.writeOutput(configProp);
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fFile)));                
                pw.print(sb);
                pw.flush();
                pw.close();
                return true;
            }catch(IOException e)
            {
                throw new GUIWriterException
                        ("IOException thrown during file write",GUIWriterException.EXCEPTION_IO);
            }
            
        }        
    }
    
    /**
       * This method is inherited from <code>GUIWriter</code> interface.
       * Will return configuration values for output 
       *
       * @param configProp Holds the property name-values
       * @return String configuration value as text
       * @throws GUIWriterException writing error
       */
   @Override
   public String getOutput(ConfigProperties configProp) throws GUIWriterException{
        ConfigConverter cc = ConfigConverter.getInstance();
        StringBuilder sb = cc.writeOutput(configProp);
        return sb.toString();
   }
    
}
