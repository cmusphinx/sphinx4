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
import java.util.List;
import java.util.ListIterator;
import java.io.IOException;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.FeatureFrame;
import edu.cmu.sphinx.frontend.Feature;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.SphinxProperties;


/**
 * An acoustic scorer that breaks the scoring up into a configurable
 * number of separate threads.
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
     * The default value for PROP_NUM_THREADS.
     */
    public final static int PROP_NUM_THREADS_DEFAULT = 1;


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
     * The default value for PROP_IS_CPU_RELATIVE.
     */
    public final static boolean PROP_IS_CPU_RELATIVE_DEFAULT = false;


    /**
     * A Sphinx Property name that controls the minimum number of
     * scoreables sent to a thread. This is used to prevent over threading
     * of the scoring that could happen if the number of threads is
     * high compared to the size of the activelist. The default is 50
     */
    public final static String PROP_MIN_SCOREABLES_PER_THREAD
        = PROP_PREFIX + "minScoreablesPerThread";


    /**
     * The default value for PROP_MIN_SCOREABLES_PER_THREAD.
     */
    public final static int PROP_MIN_SCOREABLES_PER_THREAD_DEFAULT = 50;


    /**
     * A SphinxProperty specifying whether the scoreables should keep
     * a reference to the scored features.
     */
    public final static String PROP_SCOREABLES_KEEP_FEATURE
        = PROP_PREFIX + "scoreablesKeepFeature";

    /**
     * The default value for PROP_SCOREABLES_KEEP_FEATURE.
     */
    public final static boolean PROP_SCOREABLES_KEEP_FEATURE_DEFAULT = false;


    private FrontEnd frontEnd;		// where features come from
    private SphinxProperties props;	// the sphinx properties
    private Mailbox mailbox;		// sync between caller and threads
    private Semaphore semaphore;	// join after call
    private Feature curFeature;		// current feature being processed
    private int numThreads;		// number of threads in use
    private int minScoreablesPerThread;	// min scoreables sent to a thread
    private boolean keepFeature;        // scoreables keep feature or not


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

	boolean cpuRelative =  props.getBoolean(PROP_IS_CPU_RELATIVE,
                                                PROP_IS_CPU_RELATIVE_DEFAULT);

	numThreads =  props.getInt(PROP_NUM_THREADS, PROP_NUM_THREADS_DEFAULT);
	minScoreablesPerThread =  
            props.getInt(PROP_MIN_SCOREABLES_PER_THREAD,
                         PROP_MIN_SCOREABLES_PER_THREAD_DEFAULT);
        keepFeature = props.getBoolean(PROP_SCOREABLES_KEEP_FEATURE,
                                       PROP_SCOREABLES_KEEP_FEATURE_DEFAULT);

	if (cpuRelative) {
	    numThreads += Runtime.getRuntime().availableProcessors();
	}
	if (numThreads < 1) {
	    numThreads = 1;
	}
        System.out.println("# of scoring threads: " + numThreads);

	// if we only have one thread, then we'll score the
	// states in the calling thread and we won't need any
	// of the synchronization objects or threads

	if (numThreads > 1) {
	    mailbox = new Mailbox();
	    semaphore = new Semaphore();
	    for (int i = 0; i < (numThreads - 1); i++) {
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
     * Checks to see if a FeatureFrame is null or if there are Features in it.
     *
     * @param ff the FeatureFrame to check
     *
     * @return false if the given FeatureFrame is null or if there
     * are no Features in the FeatureFrame; true otherwise.
     */
    private boolean hasFeatures(FeatureFrame ff) {
        if (ff == null) {
            System.out.println("ThreadedAcousticScorer: FeatureFrame is null");
            return false;
        }
        if (ff.getFeatures() == null) {
            System.out.println
                ("ThreadedAcousticScorer: no features in FeatureFrame");
            return false;
        }
        return true;
    }

    /**
     * Scores the given set of states
     *
     * @param scoreableList a list containing scoreable objects to
     * be scored
     *
     * @return the best scorign scoreable, or null if there are no
     * more features to score
     */
    public Scoreable calculateScores(List scoreableList) {
        Scoreable best = null;

	FeatureFrame ff;

	try {
	    ff = frontEnd.getFeatureFrame(1, null);

            if (!hasFeatures(ff)) {
                return best;
            }

	    curFeature = ff.getFeatures()[0];

	    if (curFeature.getSignal() == Signal.UTTERANCE_START) {
                ff = frontEnd.getFeatureFrame(1, null);
                if (!hasFeatures(ff)) {
                    return best;
                }
                curFeature = ff.getFeatures()[0];
            }

	    if (curFeature.getSignal() == Signal.UTTERANCE_END) {
		return best;
	    }

            if (!curFeature.hasContent()) {
                throw new Error("Can't score non-content feature");
            }

	    if (numThreads > 1) {
                
		int nThreads = numThreads;
		int scoreablesPerThread = (scoreableList.size()
			+ (numThreads - 1)) / numThreads;
		if (scoreablesPerThread < minScoreablesPerThread) {
		    scoreablesPerThread = minScoreablesPerThread;
		    nThreads =
                        (scoreableList.size() + 
                         (scoreablesPerThread - 1)) / scoreablesPerThread;
		}

		semaphore.reset(nThreads);

		for (int i = 0; i < nThreads; i++) {
		    ScoreableJob job = new ScoreableJob(scoreableList,
			    i * scoreablesPerThread, scoreablesPerThread);
                    if (i < (nThreads - 1)) {
                        mailbox.post(job);
                    } else {
                        Scoreable myBest = scoreScoreables(job);
                        semaphore.post(myBest);
                    }
		}

		best = semaphore.pend();

	    } else {
		ScoreableJob job = new ScoreableJob(scoreableList, 0,
                        scoreableList.size());
		best = scoreScoreables(job);
	    }
	} catch (IOException ioe) {
	    System.out.println("IO Exception " + ioe);
	    ioe.printStackTrace();
	    return best;
	}
	return best;
    }

    /**
     * Performs post-recognition cleanup. 
     */
    public void stop() {
    }

    /**
     * Scores all of the Scoreables in the ScoreableJob
     *
     * @param job the scoreable job
     * @return the best scoring scoreable in the job
     */
    private Scoreable scoreScoreables(ScoreableJob job) {
        
        Scoreable best = job.getFirst();
	int listSize = job.getScoreables().size();
        int end = job.getStart() + job.getSize();
        if (end > listSize) {
            end = listSize;
        }

        ListIterator iterator = job.getListIterator();

	for (int i = job.getStart(); i < end; i++) {
            Scoreable scoreable = (Scoreable) iterator.next();

            // since we are potentially doing somethigns such as frame
            // skipping and grow skipping, this check can become
            // troublesome. Thus it is currently disabled.

	    if (false && scoreable.getFrameNumber() != curFeature.getID()) {
		throw new Error
		    ("Frame number mismatch: Token: " + 
		     scoreable.getFrameNumber() +
		     "  Feature: " + curFeature.getID());
	    } 
	    if (scoreable.calculateScore(curFeature, keepFeature) > 
                best.getScore()) {
                best = scoreable;
            }
	}
        return best;
    }


    /**
     * A scoring thread waits for a new scoreable to arrive
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
	 * Waits for a scoreable job and scores the scoreable 
	 * in the job, signally back when done
	 */
	public void run() {
	    while (true) {
		ScoreableJob scoreableJob = mailbox.pend();
		Scoreable best = scoreScoreables(scoreableJob);
		semaphore.post(best);
	    }
	}
    }
}


/**
 * Mailbox class allows a set of threads to communicate a single
 * scoreable
 * job
 */
class Mailbox {

    private ScoreableJob curScoreableJob;

    /**
     * Posts a scoreable to the mail box. The caller will block
     * until the mailbox is empty and will then notify
     * any waiters
     */
    synchronized void post(ScoreableJob scoreableJob) {
      while (curScoreableJob != null) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
      }
      curScoreableJob = scoreableJob;
      notifyAll();
    }

    /**
     * Waits for a scoreable to arrive in the mailbox and returns it.
     * This will block the caller until a scoreable arrives
     *
     * @return the next scoreable
     */
    synchronized ScoreableJob pend() {
	ScoreableJob returnScoreableJob;
	while (curScoreableJob == null) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
	}
	returnScoreableJob = curScoreableJob;
	curScoreableJob = null;
	notifyAll();
	return returnScoreableJob;
    }
}


/**
 * A counting semaphore
 */
class Semaphore {
    int count;
    Scoreable bestScoreable;

    /**
     * Sets the count for this counting semaphore
     *
     * @param count the count for the semaphore
     */
    synchronized void reset(int count) {
	this.count = count;
        bestScoreable = null;
    }

    /**
     * Pends the caller until the count reaches zero
     *
     * @return the best scoreable encounted
     */
    synchronized Scoreable pend() {
	while (count > 0) {
	  try {
	      wait();
	  } catch (InterruptedException ioe) {}
	}
        return bestScoreable;
    }

    /**
     * Posts to the semaphore, decrementing the counter by one.
     * should the counter arrive at zero, wake up any penders.
     *
     * @param postedBest the best scoreable encounted for this batch.
     */
    synchronized void post(Scoreable postedBest) {
        if (bestScoreable == null ||
                postedBest.getScore() > bestScoreable.getScore()) {
            bestScoreable = postedBest;
        }
	count--;
	if (count <= 0) {
	    notifyAll();
	}
    }

}

/**
 * Represent a set of scoreables to be scored
 */
class ScoreableJob {

    private List scoreables;
    private int start;
    private int size;

    /**
     * Creates a scoreable job
     *
     * @param scoreables the list of scoreables
     * @param start the starting point for this job
     * @param size the number of scoreables in this job
     */
    ScoreableJob(List scoreables, int start, int size) {
	this.scoreables = scoreables;
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
     * Gets the number of scoreables in this job
     *
     * @return the number of scoreables in this job
     */
    int getSize() {
	return size;
    }

    /**
     * Returns the first scoreable in this job.
     *
     * @return the first scoreable in this job
     */
    Scoreable getFirst() {
        return (Scoreable) scoreables.get(start);
    }

    /**
     * Gets the entire list of scoreables
     *
     * @return the list of scoreables
     */
    List getScoreables() {
	return scoreables;
    }

    /**
     * Returns a ListIterator for this job.
     *
     * @return a ListIterator for this job.
     */
    ListIterator getListIterator() {
        return scoreables.listIterator(start);
    }

    /**
     * Returns a string representation of this object
     *
     * @return the string representation
     */
    public String toString() {
	return "List size " + scoreables.size() + " start " 
	    + start + " size " + size;
    }
}
