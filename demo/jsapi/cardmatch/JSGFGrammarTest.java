
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

package demo.jsapi.cardmatch;

import com.sun.speech.engine.recognition.BaseRecognizer;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.net.URL;

import javax.speech.EngineException;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.Recognizer;
import javax.speech.recognition.Rule;
import javax.speech.recognition.RuleAlternatives;
import javax.speech.recognition.RuleCount;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleName;
import javax.speech.recognition.RuleParse;
import javax.speech.recognition.RuleSequence;
import javax.speech.recognition.RuleToken;



/**
 * A test program for the JSGFGrammar class
 */
public class JSGFGrammarTest {

    public static void main(String[] argv) {
        try {
            String propertiesFile = argv[0];
            String pwd = System.getProperty("user.dir");
            String context = "JSGFGrammarTest";

            SphinxProperties.initContext
                (context, new URL
                 ("file://" + pwd + File.separatorChar + propertiesFile));
            SphinxProperties props = 
                SphinxProperties.getSphinxProperties(context);

            Microphone microphone = new Microphone("mic", context, props);

            Decoder decoder = new Decoder(context, microphone);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
