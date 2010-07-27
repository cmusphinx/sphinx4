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
package linguist.language;

import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.LMGrammar;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.linguist.language.ngram.SimpleNGramModel;
import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * A test for LMGrammar
 */
public class LMGramTest {

    /**
     * Construct a FullDictionaryTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public LMGramTest(String configFile) throws Exception {
        
        FullDictionary dictionary;
        Grammar grammar;

        ConfigurationManager cm = new ConfigurationManager(configFile);

        Timer lmTimer = TimerPool.getTimer(this, "LanguageModel");
        Timer gramTimer = TimerPool.getTimer(this, "Grammar");

        lmTimer.start();
        dictionary = (FullDictionary)cm.lookup("dictionary");
        dictionary.allocate();
        lmTimer.stop();

        Utilities.dumpMemoryInfo("before grammar load");
        gramTimer.start();
        grammar = new LMGrammar();
        grammar.allocate();
        gramTimer.stop();
        Utilities.dumpMemoryInfo("after grammar load");

        TimerPool.dumpAll();

        grammar.dumpStatistics();

    }


    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            new LMGramTest(argv[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
