
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

package edu.cmu.sphinx.decoder.scorer;

import java.util.Iterator;
import java.io.IOException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.SphinxProperties;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.linguist.HMMStateState;


/**
 * A test acoustic scorer that pretends to score tokens until
 * a certain number of frames have been processed
 *
 * All scores are maintained in LogMath log base
 */
public class ThreadedAcousticScorer implements AcousticScorer {

    private final static String PROP_PREFIX = 
        "edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer.";


    /**
     * A SphinxProperty name that controls the number of threads that
     * are used to score hmm states.  If the isCpuRelative property is
     * false, then is is the exact number of threads that are used to
     * score hmm states. if the isCpuRelative property is true, then
     * this value is combined with the number of available proessors
     * on the system.  If you want to have one thread per CPU
     * available to score states, set the NUM_THREADS property to 0
     * and the isCpuRelative to true.  If you want exactly one thread
     * to process scores set NUM_THREADS to 1 and isCpuRelative to
     * false. The default value is 1
     */
    public final static String PROP_NUM_THREADS = PROP_PREFIX + "numThreads";

    /**
     * A sphinx property name that controls whether the number of
     * available CPUs on the system is used when determining the
     * number of threads to use for scoring. If true, the NUM_THREADS
     * property is combined with the available number of CPUS to
     * deterimine the number of threads. Note that the number of
     * threads is contrained to be never lower than zero. Also, if the
     * number of threads is one, the states are scored on the calling
     * thread, no separate threads are started. The default value is
     * false.
     */
    public final static String PROP_IS_CPU_RELATIVE = 
        PROP_PREFIX + "isCpuRelative";

    /**
     * A Sphinx Property name that controls the minimum number of
     * tokens sent to a thread. This is used to prevent over threading
     * of the scoring that could happen if the number of threads is
     * high compared to the size of the activelist. The default is 50
     */
    public final static String PROP_MIN_TOKENS_PER_THREAD  =
        PROP_PREFIX + "minTokensPerThread";


    private FrontEnd frontEnd;		// where features come from
    private SphinxProperties props;	// the sphinx properties
    private Mailbox mailbox;		// sync between caller and threads
    private Semaphore semaphore;	// join after call
    private Feature curFeature;		// current feature being processed
    private int numThreads;		// number of threads in use
    private int minTokensPerThread;	// min tokens sent to a thread


    /**
     * Constructs a threaded acoustic scorer. The
     * ThreadedAcousticScorer will create a set of scoring threads to
     * score hmm states. This can improve performance on systems that
     * have multiple CPUS. It is unlikely to improve performance on
     * single CPUS. The number of threads created are configured by
     * two SphinxProperties PROP_NUM_THREADS and PROP_IS_CPU_RELATIVE.
     * If the number of threads is configured to be 1, this scorer
     * behaves identically to the SimpleAcousticScorer.
     *
     * @param context the cotext to use
     * @param frontEnd the FrontEnd to use
     */
    public void initialize(String context, FrontEnd frontEnd) {
	this.frontEnd = frontEnd;
	this.props = SphinxProperties.getSphinxProperties(context);
	boolean cpuRelative =  props.getBoolean(PROP_IS_CPU_RELATIVE, false);

	numThreads =  props.getInt(PROP_NUM_THREADS, 1);
	minTokensPerThread =  props.getInt(PROP_MIN_TOKENS_PER_THREAD, 50);

	if (cpuRelative) {
	    numThreads += Runtime.getRuntime().availableProcessors();
	}
	if (numThreads < 1) {
	    numThreads = 1;
	}
	// if we only have one thread, then we'll score the
	// states in the calling thread and we won't need any
	// of the synchronization objects or threads

	if (numThreads > 1) {
	    mailbox = new Mailbox();
	    semaphore = new Semaphore();
	    for (int i = 0; i < numThreads; i++) {
		Thread t = new ScoringThread();
		t.start();
	    }
	}
    }

    /**
     * Initializes the scorer
     */
    public void start() {
    }

    /**
     * Scores the given set of states
     *
     * @param stateTokenList a list containing StateToken objects to
     * be scored
     *
     * @return true if there are more features available
     */
    public boolean calculateScores(ActiveList stateTokenList) {

	FeatureFrame ff;

	try {
	    // TODO: fix the 'modelName', now set to null
	    ff = frontEnd.getFeatureFrame(1, null);
            

	    if (ff == null) {
		System.out.println("FeatureFrame is null");
		return false;
	    }


	    if (ff.getFeatures() == null) {
		System.out.println("features array is null ");
		return false;
	    }

	    curFeature  = ff.getFeatures()[0];

	    if (curFeature.getSignal() == Signal.UTTERANCE_START) {
		return true; //calculateScores(stateTokenList);
	    }
	    if (curFeature.getSignal() == Signal.UTTERANCE_END) {
		return false;
	    }

	    Token[] tokens = stateTokenList.getTokens();

	    if (numThreads > 1) {
		int nThreads = numThreads;
		int tokensPerThread = (tokens.length
			+ (numThreads - 1)) / numThreads;
		if (tokensPerThread < minTokensPerThread) {
		    tokensPerThread = minTokensPerThread;
		    nThreads = (tokens.length + (tokensPerThread - 1))
			/ tokensPerThread;
		}

		semaphore.setCount(nThreads);

		for (int i = 0; i < nThreads; i++) {
		    TokenJob job = new TokenJob(tokens,
			    i * tokensPerThread, tokensPerThread);
			mailbox.post(job);
		}

		semaphore.pend();

	    } else {
		TokenJob job = new TokenJob(tokens, 0, tokens.length);
		scoreTokens(job);
	    }
	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    ioe.printStackTrace();
	    return false;
	}
	return true;
    }

    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }

    /**
     * Scores a token
     *
     * @param token the token
     */
    private void scoreTokens(TokenJob job) {
	Token[] tokens = job.getTokens();
	int end = job.getStart() + job.getSize();
	for (int i = job.getStart(); i < end && i < tokens.length; i++) {

            Token token = tokens[i];

	    HMMStateState hmmStateState = (HMMStateState)
		token.getSentenceHMMState();

	    if (curFeature.hasContent()) {
		float logScore = hmmStateState.getScore(curFeature);
		token.applyScore(logScore, curFeature);
	    } else {
		System.out.println("non-content feature " +
		    curFeature + " id " + curFeature.getID());
	    }
	}
    }


    /**
     * A scoring thread waits for a new token to arrive
     * at the mailbox, scores it, and notifies when its done
     * by posting to the semaphore
     */
    class ScoringThread extends Thread {
	/**
	 * Creates a new ScoringThread.
	 */
	ScoringThread() {
	    setDaemon(true);
	}

	/**
	 * Waits for a token job and scores the token 
	 * in the job, signally back when done
	 */
	public void run() {
	    while (true) {
		TokenJob tokenJob = mailbox.pend();
		scoreTokens(tokenJob);
		semaphore.post();
	    }
	}

    }
}


/**
 * Mailbox class allows a set of threads to communicate a single token
 * job
 */
class Mailbox {

    private TokenJob curTokenJob;

    /**
     * Posts a token to the mail box. The caller will block
     * until the mailbox is empty and will then notify
     * any waiters
     */
    synchronized void post(TokenJob tokenJob) {
      while (curTokenJob != null) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
      }
      curTokenJob = tokenJob;
      notifyAll();
    }

    /**
     * Waits for a token to arrive in the mailbox and returns it.
     * This will block the caller until a token arrives
     *
     * @return the next token
     */
    synchronized TokenJob pend() {
	TokenJob returnTokenJob;
	while (curTokenJob == null) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
	}
	returnTokenJob = curTokenJob;
	curTokenJob = null;
	notifyAll();
	return returnTokenJob;
    }
}


/**
 * A counting semaphore
 */
class Semaphore {
    int count;

    /**
     * Sets the count for this counting semaphore
     *
     * @param count the count for the semaphore
     */
    synchronized void setCount(int count) {
	this.count = count;
    }

    /**
     * Pends the caller until the count reaches zero
     */
    synchronized void pend() {
	while (count > 0) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
	}
    }

    /**
     * Posts to the semaphore, decrementing the counter by one.
     * should the counter arrive at zero, wake up any penders.
     */
    synchronized void post() {
	count--;
	if (count <= 0) {
	    notifyAll();
	}
    }
}

/**
 * Represent a set of tokens to be scored
 */
class TokenJob {

    private Token[] tokens;
    private int start;
    private int size;

    /**
     * Creates a token job
     *
     * @param tokenList the list of tokens
     * @param start the starting point for this job
     * @param size the number of tokens in this job
     */
    TokenJob(Token[] tokens, int start, int size) {
	this.tokens = tokens;
	this.start = start;
	this.size = size;
    }

    /**
     * Gets the starting index for this job
     *
     * @return the starting index
     */
    int getStart() {
	return start;
    }

    /**
     * Gets the number of tokens in this job
     *
     * @return the number of tokens in this job
     */
    int getSize() {
	return size;
    }

    /**
     * Gets the entire list of tokens
     *
     * @return the list of tokens
     */
    Token[] getTokens() {
	return tokens;
    }

    /**
     * Returns a string representation of this object
     *
     * @return the string representation
     */
    public String toString() {
	return "List size " + tokens.length + " start " 
	    + start + " size " + size;
    }
}
