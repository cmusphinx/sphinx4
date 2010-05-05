package edu.cmu.sphinx.demo.raw;

import java.net.URISyntaxException;
import java.net.MalformedURLException;

/**
 * User: peter
 * Date: Nov 6, 2009
 * Time: 8:49:32 AM
 * <p/>
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
public class RawHelloNGram extends RawBase {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        new RawHelloNGram().run(args);
    }

    protected CommonConfiguration getConfiguration() throws MalformedURLException, URISyntaxException, ClassNotFoundException {
        HelloNGramConfiguration config = new HelloNGramConfiguration();
        return config;
    }

}
