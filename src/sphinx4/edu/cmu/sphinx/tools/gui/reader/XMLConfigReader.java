/*
 * XMLConfigReader.java
 *
 * Created on October 30, 2006, 11:35 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.reader;

import edu.cmu.sphinx.tools.gui.ConfigProperties;
import edu.cmu.sphinx.tools.gui.reader.SaxLoader;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * This class implements the process of reading from XML file .config.xml
 *
 * @author Ariani
 * @see GUIReader
 */
public class XMLConfigReader implements GUIReader {
    
    /* Singleton pattern, with this details:
     public class Singleton {
         // Private constructor suppresses generation of a (public) default constructor
        private Singleton() {}
 
        private static class SingletonHolder {
            private static Singleton instance = new Singleton();
        } 
 
        public static Singleton getInstance() {
            return SingletonHolder.instance;
        }
     }
     */
    
      /**
       * This class uses Singleton pattern for the constructor 
       * to make sure that there is only one instance of reader created
       * to get the reader instance, use <code>XMLConfigReader.getInstance()</code>
       *       
       */
      private XMLConfigReader() {}

      private static class XMLReaderHolder {
          private static XMLConfigReader instance = new XMLConfigReader();
      }
      
      /**
       * Singleton constructor 
       *
       * @return    the xml reader
       */
      public static XMLConfigReader getInstance(){
          return XMLReaderHolder.instance;
      }
    
      /**
       * The method called for reading from xml file
       *
       * @param     fFile the File to be read
       * @return    <code>ConfigProperties</code> object that holds the property type-value 
       * @throws    GUIReaderExceptoin  if there are any errors while finding
       *            and reading from file.
       */
      public ConfigProperties read(File fFile) throws GUIReaderException {        
        if ( fFile == null || fFile.getName().trim().equalsIgnoreCase("") )  /*no filename for input */
        {
            throw new GUIReaderException
                    ("No input filename specified",GUIReaderException.EXCEPTION_NO_FILENAME);
        }
        else /* start reading from config file */
        {
            try{          
               URL url = fFile.toURI().toURL();
               
               Map global = new HashMap(); 
               Map pm = loader(url, global);
               ConfigProperties cp = new ConfigProperties ();
               cp.setGlobal(global);
               cp.setProperty(pm);
               return cp;
               
            }catch(IOException e){
                throw new GUIReaderException("IO Exception during read",GUIReaderException.EXCEPTION_IO);
            }
        }
    }

      /**
     * Loads the configuration data from the given url and adds the info to the
     * symbol table. Throws an <code>IOexception</code> if there is a problem loading the
     * table. Note that this only performs a partial populating of the symbol
     * table entry
     * 
     * @param url
     *                the url to load from
     * @throws IOException
     *                 if an error occurs while loading the symbol table
     */
    private Map loader(URL url, Map global) throws IOException {
        SaxLoader saxLoader = new SaxLoader(url, global);
        return saxLoader.load();
    }
    
}
