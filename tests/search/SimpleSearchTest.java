
/**
 * [[[copyright]]]
 */
package tests.search;

import edu.cmu.sphinx.model.acoustic.AcousticModel;

import edu.cmu.sphinx.search.Dictionary;
import edu.cmu.sphinx.search.Linguist;
import edu.cmu.sphinx.search.SimpleLinguist;
import edu.cmu.sphinx.search.SearchManager;
import edu.cmu.sphinx.search.BreadthFirstSearchManager;
import edu.cmu.sphinx.search.AcousticScorer;
import edu.cmu.sphinx.search.Pruner;
import edu.cmu.sphinx.search.Token;
import edu.cmu.sphinx.search.Result;

import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.util.SphinxProperties;

import java.io.File;

import java.net.URL;
import java.util.List;
import java.util.Iterator;


/**
 * A simple search test
 */
public class SimpleSearchTest {


    private String context = "SimpleSearchTest";
    private SimpleLinguist simpleLinguist;
    private SearchManager searchManager;
    private AcousticScorer acousticScorer;
    private Pruner pruner;


    /**
     * Construct a SimpleLinguistTest with the given SphinxProperties file.
     *
     * @param propertiesFile a SphinxProperties file
     */
    public SimpleSearchTest(String propertiesFile) throws Exception {
        
        String pwd = System.getProperty("user.dir");
        SphinxProperties.initContext
            (context, new URL
             ("file://" + pwd + File.separatorChar + propertiesFile));
        
        simpleLinguist = new SimpleLinguist
            (context, AcousticModel.getAcousticModel(context));

	acousticScorer = new SimpleAcousticScorer();
	pruner = new SimplePruner();

	searchManager = new BreadthFirstSearchManager(
		context, simpleLinguist, acousticScorer, pruner);
	dumpGrammar();
	dumpSentenceHMM();
    }


    /**
     * Dumps the SentenceHMM
     */
    public void dumpSentenceHMM() {
	simpleLinguist.getInitialState().dump();
    }


    /**
     * Dumps the grammar 
     */
    public void dumpGrammar() {
        System.out.print
            (((SimpleLinguist) simpleLinguist).getGrammar().toString());
    }


    /**
     * Perform a search
     */
    public void search() {
	searchManager.initialize();
	Result result = null;

	for (int i = 0; i < 10; i++) {
	    result = searchManager.recognize(20);
	    if (result.isFinal()) {
		showResult(result);
		break;
	    } else {
		showResultSummary(result);
	    }
	}
	searchManager.terminate();
    }



    /**
     * Shows the result
     */

    public void showResult(Result result) {
	System.out.println(" -------------- results ---------------- ");
	for (Iterator i = result.getResultTokens().iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    assert(token.isFinal());
	    System.out.println( "score: " + token.getScore() + 
			" path: " + token.getWordPath() +
			(token.isFinal() ? " final" : "") );
	    token.dumpTokenPath();
	}
    }

    /**
     * Shows the result
     */

    public void showResultSummary(Result result) {
	System.out.println(" -------------- results frame # " +
		result.getFrameNumber() + " ---------------- ");

	for (Iterator i = result.getResultTokens().iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    assert(token.isFinal());
	    System.out.println( "  score: " + token.getScore() + 
			" path: " + token.getWordPath());
	}
    }

    public void showActive(Result result) {
	System.out.println(" -------------- active  ---------------- ");
	for (Iterator i = result.getActiveTokens().iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    System.out.println( "score: " + token.getScore() + 
			" path: " + token.getWordPath() +
			(token.isFinal() ? " final" : "") );
	    token.dumpTokenPath();
	}
    }

    /**
     * Main method of this Test.
     */
    public static void main(String[] argv) {
        try {
            SimpleSearchTest test = new SimpleSearchTest(argv[0]);
	    test.search();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


/**
 * A simple pruner that doesn't really prune
 */
class SimplePruner implements Pruner {

    float minScore = -1E20F;
    /**
     * Initializes the scorer
     */
    public void initialize() {
    }

    /**
     * prunes the given set of states
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     */
    public void prune(List stateTokenList) {
	int startSize = stateTokenList.size();
	for (Iterator i = stateTokenList.listIterator(); i.hasNext();) {
	    Token token = (Token) i.next();
	    if (token.getScore() < minScore) {
		i.remove();
	    }
	}

	System.out.println("Pruner: " + stateTokenList.size() 
		+ " tokens remaining after pruning " + (startSize -
		stateTokenList.size()));
    }


    /**
     * Performs post-recognition cleanup. 
     */
    public void terminate() {
    }
}


class SimpleAcousticScorer implements AcousticScorer {
    int maxFeatures = 1000;
    int curFeature = 0;

    /**
     * Initializes the scorer
     */
    public void initialize() {
    }

    /**
     * Scores the given set of states
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     */
    public boolean  calculateScores(List stateTokenList) {
	for (Iterator i = stateTokenList.iterator(); i.hasNext(); ) {
	    Token token = (Token) i.next();
	    assert(token.isEmitting());
	}
	return (curFeature++ < maxFeatures) ;
    }

    /**
     * Performs post-recognition cleanup. 
     */
    public void terminate() {
    }
}
