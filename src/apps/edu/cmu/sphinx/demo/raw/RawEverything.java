package edu.cmu.sphinx.demo.raw;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * User: peter
 * Date: Nov 6, 2009
 * Time: 8:59:23 AM
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
public class RawEverything {
    public static void main(String[] args) throws MalformedURLException, URISyntaxException, ClassNotFoundException {

        if( args.length > 2 ) {
            throw new Error( "USAGE: RawEverything [<WAV file>]" );
        }

        RawTranscriber.run(args);
        RawHelloNGram.run(args);
    }
}
