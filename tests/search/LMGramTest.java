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

package tests.search;

import edu.cmu.sphinx.util.SphinxProperties;

import edu.cmu.sphinx.decoder.linguist.LMGrammar;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.LanguageModelFactory;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;

import java.io.File;

import java.net.URL;


/**
 * A test for LMGrammar
 */
public class LMGramTest {

    private String context = "LMGramTest";

    /**
     * Construct a FullDictionaryTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public LMGramTest(String propertiesFile) throws Exception {
        
        LanguageModel languageModel;
        edu.cmu.sphinx.decoder.linguist.Grammar grammar;

        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (context, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
        SphinxProperties props = SphinxProperties.getSphinxProperties(context);

        Timer lmTimer = Timer.getTimer("LMGramTest", "LanguageModel");
        Timer gramTimer = Timer.getTimer("LMGramTest", "Grammar");

        lmTimer.start();
        languageModel = LanguageModelFactory.createLanguageModel(context);
        lmTimer.stop();

        Utilities.dumpMemoryInfo("before grammar load");
        gramTimer.start();
        grammar = new edu.cmu.sphinx.decoder.linguist.LMGrammar();
        grammar.initialize(props.getContext(), languageModel, null);
        gramTimer.stop();
        Utilities.dumpMemoryInfo("after grammar load");

        Timer.dumpAll();

        grammar.dumpStatistics();

    }


    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            LMGramTest test = new LMGramTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
