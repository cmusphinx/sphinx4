package edu.cmu.sphinx.tools.batch;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.linguist.language.grammar.ForcedAlignerGrammar;
import edu.cmu.sphinx.linguist.language.grammar.BatchForcedAlignerGrammar;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.decoder.search.Token;

import java.io.*;
import java.util.Iterator;
import java.net.URL;

/**
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * User: Peter Wolf
 * Date: Jan 9, 2006
 * Time: 5:35:54 PM
 *
 * Utility for generating word segmentation by forced alignment
 *
 * Given a CTL file that specifies a series of audio and coresponding correct transcripts,
 * this utility creates a trivial grammar from the transcript, and runs the recognizer on
 * the utterance.  The output is words with beginning and end times.
 *
 * See BatchNISTRecognizer for more information about the format of CTL and audio files.
 */

public class BatchForcedAlignerRecognizer extends BatchNISTRecognizer {

    String segFile;
    ForcedAlignerGrammar forcedAlignerGrammar;

    protected void setInputStream(CTLUtterance utt) throws IOException {
        super.setInputStream(utt);
        ((BatchForcedAlignerGrammar)(((FlatLinguist)(recognizer.getDecoder().getSearchManager().getLinguist())).getGrammar())).setUtterance(utt.getName());
    }

    protected void handleResult(DataOutputStream out, CTLUtterance utt, Result result) throws IOException {
        System.out.println(utt+ " --> " + result);
        Token token = result.getBestToken();
        dumpTokenTimes(token);
    }

    void dumpTokenTimes( Token token ) {
        if( token != null ) {
            dumpTokenTimes(token.getPredecessor());
            System.out.println(token.getWord() + " " + token.getFrameNumber());
        }
    }

    public static void main(String[] argv) {

        if (argv.length != 1) {
            System.out.println(
                    "Usage: BatchForcedAlignerRecognizer propertiesFile");
            System.exit(1);
        }

        BatchNISTRecognizer.main(argv);
    }
}
