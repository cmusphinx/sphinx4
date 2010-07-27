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
package frontend;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.util.DataDumper;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.FileInputStream;

/**
 * Test program for the FrontEnd.
 */
public class FrontEndTest {

    public static void main(String[] argv) {

	if (argv.length < 3) {
	    System.out.println
                ("Usage: java testClass <testName> " +
                 "<configFile> <audiofilename>");
	}

        try {
            String configFile = argv[1];
            String audioFile = argv[2];
            
            ConfigurationManager cm = new ConfigurationManager(configFile);

            StreamDataSource source = (StreamDataSource) cm.lookup ("streamDataSource");
            source.setInputStream(new FileInputStream(audioFile), audioFile);
            DataDumper dumper = (DataDumper)cm.lookup("dataDumper");


            Data data = null;
            do {
                data = dumper.getData();
            } while (data != null);
            
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
