
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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.frontend.DataSource;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.parallel.ParallelFrontEnd;
import edu.cmu.sphinx.frontend.SimpleFrontEnd;
import edu.cmu.sphinx.knowledge.acoustic.AcousticModel;
import edu.cmu.sphinx.knowledge.language.LanguageModel;
import edu.cmu.sphinx.knowledge.language.LanguageModelFactory;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.ResultListener;
import edu.cmu.sphinx.decoder.search.Pruner;
import edu.cmu.sphinx.decoder.search.SearchManager;
import edu.cmu.sphinx.decoder.scorer.AcousticScorer;
import edu.cmu.sphinx.decoder.linguist.Grammar;
import edu.cmu.sphinx.decoder.linguist.Linguist;
import edu.cmu.sphinx.decoder.linguist.LinguistProcessor;
import edu.cmu.sphinx.decoder.search.SimplePruner;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * A high level interface to the sphinx 4
 */
public class Recognizer {


    /**
     * The sphinx property name prefix for this Recognizer.
     */
    private final static String PROP_PREFIX =
        "edu.cmu.sphinx.decoder.Recognizer.";


    /**
     * The sphinx property for the front end class.
     */
    public final static String PROP_FRONT_END = PROP_PREFIX + "frontend";


    /**
     * The default value of PROP_FRONT_END.
     */
    public final static String PROP_FRONT_END_DEFAULT
        = "edu.cmu.sphinx.frontend.SimpleFrontEnd";


    /**
     * The sphinx property name for the Linguist class.
     */
    public final static String PROP_LINGUIST = PROP_PREFIX + "linguist";


    /**
     * The default value for the sphinx property name for the Linguist class.
     */
    public final static String PROP_LINGUIST_DEFAULT =
	"edu.cmu.sphinx.decoder.linguist.simple.SimpleLinguist";

    
    /**
     * The sphinx property name for the Grammar class.
     */
    public final static String PROP_GRAMMAR = PROP_PREFIX + "grammar";


    /**
     * The default value of the sphinx property name for the Grammar class.
     */
    public final static String PROP_GRAMMAR_DEFAULT =
	"edu.cmu.sphinx.decoder.linguist.SimpleWordListGrammar";


    /**
     * The sphinx property name for the AcousticScorer class.
     */
    public final static String PROP_ACOUSTIC_SCORER = 
        PROP_PREFIX + "acousticScorer";

    
    /**
     * The default value of the sphinx property name for the AcousticScorer
     * class.
     */
    public final static String PROP_ACOUSTIC_SCORER_DEFAULT =
	"edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer";


    /**
     * The sphinx property name for the SearchManager class.
     */
    public final static String PROP_SEARCH_MANAGER = 
        PROP_PREFIX + "searchManager";


    /**
     * The default value of the sphinx property name for the SearchManager
     * class.
     */
    public final static String PROP_SEARCH_MANAGER_DEFAULT =
	"edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager";


    /**
     * The sphinx property name for whether to output the sentence HMM.
     */
    public final static String PROP_DUMP_SENTENCE_HMM = 
        PROP_PREFIX + "dumpSentenceHMM";


    /**
     * The default value of the property PROP_DUMP_SENTENCE_HMM.
     */
    public final static boolean PROP_DUMP_SENTENCE_HMM_DEFAULT = false;


    /**
     * The sphinx property name for whether to output the FrontEnd.
     */
    public final static String PROP_DUMP_FRONT_END = 
        PROP_PREFIX + "dumpFrontEnd";


    /**
     * The default value of the property PROP_DUMP_FRONT_END.
     */
    public final static boolean PROP_DUMP_FRONT_END_DEFAULT = false;


    /**
     * The sphinx property name for the number of features to
     * recognize at once.
     */
    public final static String PROP_FEATURE_BLOCK_SIZE = 
        PROP_PREFIX + "featureBlockSize";


    /**
     * The default value of the property PROP_FEATURE_BLOCK_SIZE.
     */
    public final static int PROP_FEATURE_BLOCK_SIZE_DEFAULT = 50;


    /**
     * A sphinx property name for (space separated) set of sentence
     * hmm processors 
     */
    public final static String PROP_LINGUIST_PROCESSORS =
	PROP_PREFIX + "linguistProcessors";


    /**
     * The default value for the property PROP_LINGUIST_PROCESSORS.
     */
    public final static String PROP_LINGUIST_PROCESSORS_DEFAULT = null;


    /**
     * A SphinxProperty name for the boolean property that controls
     * whether or not the recognizer will display detailed memory
     * information while it is running. The default value is
     * <code>true</code>.
     */
    public final static String PROP_DUMP_MEMORY_INFO = 
        PROP_PREFIX + "dumpMemoryInfo";


    /**
     * The default value for the property PROP_DUMP_MEMORY_INFO.
     */
    public final static boolean PROP_DUMP_MEMORY_INFO_DEFAULT = false;


    private SphinxProperties props;       // sphinx properties

    private AcousticScorer scorer;        // used to score the active list
    private FrontEnd frontend;            // frontend audio preprocessor
    private Linguist linguist;            // provides grammar/language info
    private Pruner pruner;                // used to prune the active list
    private SearchManager searchManager;  // drives the search
    private LanguageModel languageModel;  // the language model
    private Grammar grammar;              // the grammar

    private int featureBlockSize = 50;    // the feature blocksize

    private Vector resultsListeners = new Vector();

    private boolean dumpMemoryInfo;
    private boolean dumpSentenceHMM;
    private boolean dumpFrontEnd;


    /**
     * Constructs a Recognizer with known dataSource.
     *
     * Some of the building blocks for the
     * decoder (currently the linguist and the grammar) are set via
     * properties. This can be extended for other parts of the decoder
     * as multiple implementations become available
     *
     * @param context the context of this Recognizer
     * @param dataSource the source of data of this Recognizer
     */
    public Recognizer(String context, DataSource dataSource) 
        throws IOException {
        this(context);

        frontend = getFrontEnd(context, dataSource);
        dumpMemoryInfo("front end");

        scorer = getAcousticScorer(frontend);
        dumpMemoryInfo("scorer");

        searchManager = getSearchManager(linguist, scorer, pruner);
        dumpMemoryInfo("search");
    }


    /**
     * Constructs a Recognizer when the dataSource is not yet known.
     * Only the following components are initialized:
     *
     * acoustic model
     * language model
     * grammar
     * linguist
     * pruner
     *
     * Some of the building blocks for the
     * decoder (currently the linguist and the grammar) are set via
     * properties. This can be extended for other parts of the decoder
     * as multiple implementations become available
     *
     * @param context the context of this Recognizer
     */
    protected Recognizer(String context) throws IOException {
        
        props = SphinxProperties.getSphinxProperties(context);
        dumpMemoryInfo = props.getBoolean(PROP_DUMP_MEMORY_INFO,
                                          PROP_DUMP_MEMORY_INFO_DEFAULT);
        dumpSentenceHMM = props.getBoolean(PROP_DUMP_SENTENCE_HMM,
                                           PROP_DUMP_SENTENCE_HMM_DEFAULT);
        dumpFrontEnd = props.getBoolean(PROP_DUMP_FRONT_END,
                                        PROP_DUMP_FRONT_END_DEFAULT);
        
        dumpMemoryInfo("recognizer start");
        
        AcousticModel[] models = getAcousticModels(context);
        dumpMemoryInfo("acoustic model");

        languageModel = LanguageModelFactory.createLanguageModel(context);
        dumpMemoryInfo("languageModel");

        grammar = getGrammar(languageModel);
        dumpMemoryInfo("grammar");

        linguist = getLinguist(languageModel, grammar, models);
        dumpMemoryInfo("linguist");

        // TODO: Pruner should come from configuration like all of the
        //       other pieces
        pruner = new SimplePruner();
        dumpMemoryInfo("pruner");
        
        featureBlockSize = props.getInt(PROP_FEATURE_BLOCK_SIZE,
                                        PROP_FEATURE_BLOCK_SIZE_DEFAULT);
    }


    /**
     * Prepares recognizer for forced alignment. It resets the grammar
     * and linguist restricting the "hypothesis" to one given phrase.
     *
     *
     * @param context the context of this Recognizer
     * @param referenceText a reference sentence used to build a grammar
     *
     * @throws java.io.IOException if recognizer has not been initialized
     */
    public void forcedAligner(String context, String referenceText)
                                        throws IOException {
        props = SphinxProperties.getSphinxProperties(context);

        if (frontend == null) {
            throw new IOException("Recognizer has not been initialized");
        }
        grammar = getGrammar(referenceText);
        linguist = getLinguist(null, grammar, null);

        searchManager = getSearchManager(linguist, scorer, pruner);
    }

    /**
     * Decodes an utterance.
     *
     * @param timer the Timer to use
     * @param ref the reference input
     *
     * @return the decoded Result
     */
    public Result recognize() {
        searchManager.start();
        
        Result result;
        
        do {
            result = searchManager.recognize(featureBlockSize);
            fireResultListeners(result);
        } while (!result.isFinal());

        searchManager.stop();

        dumpMemoryInfo("recognize");

        return result;
    }


    /**
     * Returns the FrontEnd of this Recognizer.
     *
     * @return the FrontEnd
     */
    public FrontEnd getFrontEnd() {
        return frontend;
    }


    /**
     * Returns the Grammar of this Recognizer.
     *
     * @return the Grammar
     */
    public Grammar getGrammar() {
        return grammar;
    }


    /**
     * Initialize and return the frontend based on the given sphinx
     * properties.
     *
     * @param context the context of interest
     * @param dataSource the source of data to decode
     */
    private FrontEnd getFrontEnd(String context, DataSource dataSource) {
        String path = null;
        try {
            path = props.getString(PROP_FRONT_END, PROP_FRONT_END_DEFAULT);
            FrontEnd fe = (FrontEnd)Class.forName(path).newInstance();
            fe.initialize("FrontEnd", context, dataSource);
            if (dumpFrontEnd) {
                System.out.println(fe.toString());
            }
            return fe;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create front end " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create front end " + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create front end " + path, iea);
        } catch (IOException ioe) {
            throw new Error("IOE: Can't create front end " + path + " "
                    + ioe, ioe);
        }
    }


    /**
     * Initialize and return the AcousticModel(s) used by this Recognizer.
     *
     * @param context the context of interest
     *
     * @return the AcousticModel(s) used by this Recognizer
     */
    private AcousticModel[] getAcousticModels(String context)
	throws IOException {
	List modelNames = AcousticModel.getNames(context);
	AcousticModel[] models;
	if (modelNames.size() == 0) {
	    models = new AcousticModel[1];
	    models[0] = AcousticModel.getAcousticModel(context);
	} else {
	    models = new AcousticModel[modelNames.size()];
	    int m = 0;
	    for (Iterator i = modelNames.iterator(); i.hasNext(); m++) {
		String modelName = (String) i.next();
		models[m] = AcousticModel.getAcousticModel(modelName, context);
	    }
	}
	return models;
    }


    /**
     * Returns an initialized grammar based upon sphinx properties
     *
     * @param languageModel the language model
     *
     * @return the grammar
     */
    private Grammar getGrammar(LanguageModel languageModel) {
        String path = null;
        try {
            path = props.getString(PROP_GRAMMAR, PROP_GRAMMAR_DEFAULT);
            Grammar newGrammar = (Grammar)Class.forName(path).newInstance();
            newGrammar.initialize(props.getContext(), languageModel, null);
            return newGrammar;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create grammar " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create grammar" + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create grammar" + path, iea);
        } catch (IOException ioe) {
            throw new Error("IOE: Can't create grammar " + path + " "
                    + ioe, ioe);
        } catch (NoSuchMethodException nsme) {
            throw new Error("NSME: Can't create grammar " + path, nsme);
        }
    }

    /**
     * Returns an initialized grammar based upon sphinx properties
     *
     * @param refText the reference sentence
     *
     * @return the grammar
     */
    private Grammar getGrammar(String referenceText) {
        String path = null;
        try {
            path = props.getString(PROP_GRAMMAR,
                    "edu.cmu.sphinx.decoder.linguist.ForcedAlignerGrammar");
            Grammar newGrammar = (Grammar)Class.forName(path).newInstance();
            newGrammar.initialize(props.getContext(), null, referenceText);
            return newGrammar;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create grammar " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create grammar" + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create grammar" + path, iea);
        } catch (IOException ioe) {
            throw new Error("IOE: Can't create grammar " + path + " "
                    + ioe, ioe);
        } catch (NoSuchMethodException nsme) {
            throw new Error("NSME: Can't create grammar " + path, nsme);
        }
    }

    /**
     * Returns an initialized linguist based upon sphinx properties
     *
     * @param languageModel the language model
     * @param grammar the grammar 
     * @param acousticModel the acoustic model
     *
     * @return the linguist
     */
    private Linguist getLinguist(LanguageModel languageModel,
				 Grammar grammar,
				 AcousticModel[] models) {
        try {
	    String linguistClass = 
		props.getString(PROP_LINGUIST, PROP_LINGUIST_DEFAULT);
	    
            Linguist newLinguist = (Linguist)
                Class.forName(linguistClass).newInstance();
            newLinguist.initialize(props.getContext(), 
				   languageModel, grammar, models);

	    runLinguistProcessors(newLinguist);
            return newLinguist;
        } catch (ClassNotFoundException fe) {
            throw new Error("Can't create linguist", fe);
        } catch (InstantiationException ie) {
            throw new Error("Can't create linguist", ie);
        } catch (IllegalAccessException iea) {
            throw new Error("Can't create linguist", iea);
        }

    }

    /**
     * Runs the LinguistProcessors on the just initialized linguist
     * LinguistProcessors are typically used to dump out the hmm,
     * validate the hmm or optimize it.
     *
     * @param linguist the linguist
     */
    private void runLinguistProcessors(Linguist linguist) {
	String processors = props.getString(PROP_LINGUIST_PROCESSORS,
                                            PROP_LINGUIST_PROCESSORS_DEFAULT);
	if (processors != null) {
	    StringTokenizer st = new StringTokenizer(processors);
	    while (st.hasMoreTokens()) {
		String className = st.nextToken();
		System.out.println("Class name " + className);
		try {
		    LinguistProcessor lp = (LinguistProcessor)
			Class.forName(className).newInstance();
		    lp.process(props, linguist);
		} catch (ClassNotFoundException fe) {
		    throw new Error("Can't find LinguistProccesor "
			    + className , fe);
		} catch (InstantiationException ie) {
		    throw new Error("Can't create LinguistProccesor "
			    + className, ie);
		} catch (IllegalAccessException iea) {
		    throw new Error("Can't access LinguistProccesor "
			    + className, iea);
		} catch (ClassCastException cce) {
		    throw new Error("Bad class type for  LinguistProccesor "
			    + className, cce);
		}
	    }
	}
    }


    /**
     * Initializes and returns the AcousticScorer specified in
     * the sphinx properties file. If not specified, the
     * ThreadedAcousticScorer will be returned.
     *
     * @param frontend the frontend used to initialize the AcousticScorer
     *
     * @return an AcousticScorer
     */ 
    private AcousticScorer getAcousticScorer(FrontEnd frontend) {
	String path = null;
        try {
            path = props.getString
		(PROP_ACOUSTIC_SCORER, PROP_ACOUSTIC_SCORER_DEFAULT);
	    
            AcousticScorer scorer =
		(AcousticScorer)Class.forName(path).newInstance();
            scorer.initialize(props.getContext(), frontend);
            return scorer;
        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't find scorer class " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("InstantiationException: Can't create scorer " +
			    path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create scorer " + path, iea);
        }
    }


    /**
     * Initializes and returns the SearchManager specified in the
     * sphinx properties file. If it is not specified,
     * the BreadthFirstSearchManager will be returned
     *
     * @param linguist the Linguist to use
     * @param scorer the AcousticScorer to use
     * @param pruner the Pruner to use
     *
     * @return a SearchManager
     */
    private SearchManager getSearchManager(Linguist linguist,
					   AcousticScorer scorer,
					   Pruner pruner) {
        String path = null;
        try {
            path = props.getString
		(PROP_SEARCH_MANAGER, PROP_SEARCH_MANAGER_DEFAULT);

            SearchManager searchManager =
		(SearchManager)Class.forName(path).newInstance();
            searchManager.initialize(props.getContext(), linguist,
				     scorer, pruner);
            return searchManager;

        } catch (ClassNotFoundException fe) {
            throw new Error("CNFE:Can't create SearchManager " + path, fe);
        } catch (InstantiationException ie) {
            throw new Error("IE: Can't create SearchManager " + path, ie);
        } catch (IllegalAccessException iea) {
            throw new Error("IEA: Can't create SearchManager " + path, iea);
        }
    }


    /**
     * Fire all result listeners
     *
     * @param result the new result
     */
    private void fireResultListeners(Result result) {
        Vector copy = (Vector) resultsListeners.clone();

        for (Iterator i = copy.iterator(); i.hasNext(); ) {
            ResultListener listener = (ResultListener) i.next();
            listener.newResult(result);
        }
    }

    /**
     * Add a listener to be called when a new result is generated
     *
     * @param listener the listener to be added
     */
    public void addResultListener(ResultListener listener) {
        resultsListeners.add(listener);
    }

    /**
     * Removes a listener 
     *
     * @param listener the listener to be removed
     */
    public void removeResultListener(ResultListener listener) {
        resultsListeners.remove(listener);
    }


    /**
     * Conditional dumps out memory information
     *
     * @param what an additional info string
     */
    private void dumpMemoryInfo(String what) {
        if (dumpMemoryInfo) {
            Utilities.dumpMemoryInfo(what);
        }
    }


}

