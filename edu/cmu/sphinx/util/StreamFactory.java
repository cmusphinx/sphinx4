
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

package edu.cmu.sphinx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;


/**
 * Supports the loading of files from different sources, e.g., a ZIP
 * file or a plain directory. Provides methods that returns an
 * InputStream to the named file in the given source.
 */
public class StreamFactory {

    /**
     * Identifies a ZIP file.
     */
    public static final String ZIP_FILE = "ZIP_FILE";

    
    /**
     * Identifies a plain directory.
     */
    public static final String DIRECTORY = "DIRECTORY";


    /**
     * Returns an appropriate InputStream of the given file
     * in the given URL location.
     * The location can be a plain directory or a ZIP file
     * (these are the only two supported at this point). 
     * The <code>resolve</code> method is called to resolve whether
     * "location" refers to a ZIP file or a directory.
     *
     * Suppose you want the InputStream to the file "dict/dictionary.txt"
     * in the ZIP file "file:/lab/speech/sphinx4/data/wsj.zip".
     * You will do:
     * <code>
     * StreamFactory.getInputStream(
     *    "file:/lab/speech/sphinx4/data/wsj.zip", "dict/dictionary.txt");
     * </code>
     *
     * Suppose you want the InputStream to the file "dict/dictionary.txt"
     * in the directory "file:/lab/speech/sphinx4/data/wsj", you will do:
     * <code>
     * StreamFactory.getInputStream(
     *    "file:/lab/speech/sphinx4/data/wsj", "dict/dictionary.txt");
     * </code>
     *
     * The <code>StreamFactory.resolve()</code> method is called to
     * resolve whether "location" refers to a ZIP file or a directory.
     *
     * @param format the format of the input data, the currently supported
     *    formats are:
     * <br>StreamFactory.ZIP_FILE
     * <br>StreamFactory.DIRECTORY
     *
     * @param location the URL location of the input data, it can now
     *    be a directory or a ZIP file
     *
     * @param file the file in the given location to obtain the InputStream
     *
     * @return an InputStream of the given file in the given location 
     */
    public static InputStream getInputStream(String location,
                                             String file) throws 
    FileNotFoundException, IOException, ZipException {
        if (location != null) {
            return StreamFactory.getInputStream
                (StreamFactory.resolve(location), location, file);
        } else {
            return StreamFactory.getInputStream(StreamFactory.DIRECTORY,
                                                location, file);
        }
    }


    /**
     * According to the given data format, returns an appropriate
     * InputStream of the given file in the given URL location.
     * The location can be a plain directory or a ZIP file
     * (these are the only two supported at this point).
     *
     * Suppose you want the InputStream to the file "dict/dictionary.txt"
     * in the ZIP file "file:/lab/speech/sphinx4/data/wsj.zip".
     * You will do:
     * <code>
     * StreamFactory.getInputStream(StreamFactory.ZIP_FILE,
     *    "file:/lab/speech/sphinx4/data/wsj.zip", "dict/dictionary.txt");
     * </code>
     *
     * Suppose you want the InputStream to the file "dict/dictionary.txt"
     * in the directory "file:/lab/speech/sphinx4/data/wsj", you will do:
     * <code>
     * StreamFactory.getInputStream(StreamFactory.DIRECTORY,
     *    "file:/lab/speech/sphinx4/data/wsj", "dict/dictionary.txt");
     * </code>
     *
     * @param format the format of the input data, the currently supported
     *    formats are:
     * <br>StreamFactory.ZIP_FILE
     * <br>StreamFactory.DIRECTORY
     *
     * @param location the URL location of the input data, it can now
     *    be a directory or a ZIP file, or null if no location is given,
     *    which means that the <code>argument</code> also 
     *    specifies the exact location
     *
     * @param file the file in the given location to obtain the InputStream
     *
     * @return an InputStream of the given file in the given location 
     */
    public static InputStream getInputStream(String format,
                                             String location,
                                             String file) throws 
    FileNotFoundException, IOException, ZipException {
        InputStream stream = null;
        if (format.equals(ZIP_FILE)) {
            try {
                ZipFile zipFile = new ZipFile(new File(new URI(location)));
                ZipEntry entry = zipFile.getEntry(file);
                if (entry != null) {
                    stream = zipFile.getInputStream(entry);
                }
            } catch (URISyntaxException use) {
                use.printStackTrace();
                throw new ZipException("URISyntaxException: " +
                                       use.getMessage());
            }
        } else if (format.equals(DIRECTORY)) {
            if (location != null) {
                stream = new FileInputStream(location + File.separator + file);
            } else {
                stream = new FileInputStream(file);
            }
        }
        return stream;
    }


    /**
     * Returns the type of the given data source.
     * The current supported types are:
     * <code>
     * StreamFactory.ZIP_FILE
     * StreamFactory.DIRECTORY
     * </code>
     */
    public static String resolve(String sourceName) {
        if (sourceName.endsWith(".zip")) {
            return StreamFactory.ZIP_FILE;
        } else {
            return StreamFactory.DIRECTORY;
        }
    }
}


