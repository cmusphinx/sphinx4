/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */


package tests.frontend;


import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FrontEndFactory;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.util.DataDumper;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;
import java.io.FileInputStream;

import java.net.URL;

import java.util.Collection;
import java.util.Iterator;


/**
 * Test program for the FrontEnd.
 */
public class FrontEndTest {

    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println
                ("Usage: java testClass <testName> " +
                 "<propertiesFile> <audiofilename>");
	}

        try {
            String testName = argv[0];
            String propertiesFile = argv[1];
            String audioFile = argv[2];

            String pwd = System.getProperty("user.dir");

            SphinxProperties.initContext
                (testName, new URL
                 ("file://" + pwd + File.separatorChar + propertiesFile));

            SphinxProperties props = 
                SphinxProperties.getSphinxProperties(testName);

            StreamDataSource source = new StreamDataSource();
            source.initialize("Audio", null, props, null);
            source.setInputStream(new FileInputStream(audioFile), audioFile);

            Collection names = FrontEndFactory.getNames(props);

            String name = (String) names.iterator().next();

            FrontEnd frontend = FrontEndFactory.getFrontEnd(props, name);
            
            frontend.setDataSource(source);

            DataDumper dumper = new DataDumper();
            dumper.initialize("Dumper", name, props, frontend);

            Data data = null;
            do {
                data = dumper.getData();
            } while (data != null);
            
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
