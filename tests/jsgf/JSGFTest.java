/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package tests.jsgf;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import edu.cmu.sphinx.util.props.PropertyException;


/**
 * A test program for jsgf grammars
 */
public class JSGFTest {
    /**
     * Main method for running the HelloDigits demo.
     */
    public static void main(String[] args) {
        try {
            URL url = new File(args[0]).toURI().toURL();
            ConfigurationManager cm = new ConfigurationManager(url);

	    JSGFGrammar jsgfGrammar = (JSGFGrammar) cm.lookup("jsgfGrammar");

            jsgfGrammar.allocate();
            jsgfGrammar.dumpRandomSentences("jsgf_out.txt", 100000);
        } catch (IOException e) {
            System.err.println("Problem when loading JSGFTest: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring JSGFTest: " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Problem creating JSGFTest: " + e);
            e.printStackTrace();
        }
    }
}
