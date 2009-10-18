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

package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Integer;

import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/**
 * An acoustic scorer that breaks the scoring up into a configurable number of separate threads.
 * <p/>
 * All scores are maintained in LogMath log base
 */
public class ThreadedAcousticScorer extends AbstractScorer {

    /**
     * A SphinxProperty name that controls the number of threads that are used to score hmm states. If the isCpuRelative
     * property is false, then is is the exact number of threads that are used to score hmm states. If the isCpuRelative
     * property is true, then this value is combined with the number of available proessors on the system. If you want
     * to have one thread per CPU available to score states, set the NUM_THREADS property to 0 and the isCpuRelative to
     * true. If you want exactly one thread to process scores set NUM_THREADS to 1 and isCpuRelative to false.
     * <p/>
     * If the value is 1 isCpuRelative is false no additional thread will be instantiated, and all compuation will be
     * done in the calling thread itself. The default value is 0.
     */
    @S4Integer(defaultValue = 0)
    public final static String PROP_NUM_THREADS = "numThreads";


    /**
     * A sphinx property name that controls whether the number of available CPUs on the system is used when determining
     * the number of threads to use for scoring. If true, the NUM_THREADS property is combined with the available number
     * of CPUS to deterimine the number of threads. Note that the number of threads is contrained to be never lower than
     * zero. Also, if the number of threads is 0, the states are scored on the calling thread, no separate threads are
     * started. The default value is false.
     */
    @S4Boolean(defaultValue = true)
    public final static String PROP_IS_CPU_RELATIVE = "isCpuRelative";


    /**
     * A Sphinx Property name that controls the minimum number of scoreables sent to a thread. This is used to prevent
     * over threading of the scoring that could happen if the number of threads is high compared to the size of the
     * activelist. The default is 50
     */
    @S4Integer(defaultValue = 10)
    public final static String PROP_MIN_SCOREABLES_PER_THREAD = "minScoreablesPerThread";


    private int numThreads;         // number of threads in use
    private int minScoreablesPerThread; // min scoreables sent to a thread
    private boolean areThreadsAllocated;

    private Mailbox mailbox;        // sync between caller and threads
    private Semaphore semaphore;    // join after call
    private Data currentData;       // current feature being processed

    /**
     * @param frontEnd the frontend to retrieve features from for scoring
     * @param scoreNormalizer optional post-processor for computed scores that will normalize scores. If not set, no normalization will
     * applied and the token scores will be returned unchanged.
     * @param minScoreablesPerThread the number of threads that are used to score hmm states. If the isCpuRelative
     * property is false, then is is the exact number of threads that are used to score hmm states. If the isCpuRelative
     * property is true, then this value is combined with the number of available proessors on the system. If you want
     * to have one thread per CPU available to score states, set the NUM_THREADS property to 0 and the isCpuRelative to
     * true. If you want exactly one thread to process scores set NUM_THREADS to 1 and isCpuRelative to false.
     * <p/>
     * If the value is 1 isCpuRelative is false no additional thread will be instantiated, and all compuation will be
     * done in the calling thread itself. The default value is 0.
     * @param cpuRelative controls whether the number of available CPUs on the system is used when determining
     * the number of threads to use for scoring. If true, the NUM_THREADS property is combined with the available number
     * of CPUS to deterimine the number of threads. Note that the number of threads is contrained to be never lower than
     * zero. Also, if the number of threads is 0, the states are scored on the calling thread, no separate threads are
     * started. The default value is false.
     * @param numThreads the minimum number of scoreables sent to a thread. This is used to prevent
     * over threading of the scoring that could happen if the number of threads is high compared to the size of the
     * activelist. The default is 50
     */
    public ThreadedAcousticScorer(BaseDataProcessor frontEnd, ScoreNormalizer scoreNormalizer, int minScoreablesPerThread,boolean cpuRelative, int numThreads) {
        super(frontEnd, scoreNormalizer);
        init(minScoreablesPerThread,cpuRelative, numThreads );
    }

    public ThreadedAcousticScorer() {
    }
    
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        init(ps.getInt(PROP_MIN_SCOREABLES_PER_THREAD), ps.getBoolean(PROP_IS_CPU_RELATIVE), ps.getInt(PROP_NUM_THREADS));
    }

    private void init(int minScoreablesPerThread,boolean cpuRelative, int numThreads ) {
        this.minScoreablesPerThread = minScoreablesPerThread;

        this.numThreads = numThreads;

        if (cpuRelative) {
            this.numThreads += Runtime.getRuntime().availableProcessors();
        }
    }


    @Override
    public void startRecognition() {
        super.startRecognition();

        startScoringThreads();
    }


    @Override
    public void stopRecognition() {
        super.stopRecognition();
        
        stopScoringThreads();
    }


    /**
     * (Re-)Starts all scoring threads. If no addtional scoring thread shoudl be used, then we'll score the states in
     * the calling thread and we won't need any of the synchronization objects or threads.
     */
    private void startScoringThreads() {
        if (areThreadsAllocated) // don't start the threads more than once
            return;

        areThreadsAllocated = true;
        logger.fine("# of scoring threads: " + numThreads);


        if (numThreads > 0) {
            mailbox = new Mailbox();
            semaphore = new Semaphore();

            for (int i = 0; i < numThreads; i++) {
                new ScoringThread().start();
            }
        }
    }



    /**
     * Stops all scoring threads by setting the mailbox state to be deallocated. This is in many cases not necessary
     * because all of them are daemon threads, but is mandatory if many sphinx4 instances are started within one
     * appliation.
     */
    private void stopScoringThreads() {
        if (mailbox != null) {
            // remark: every call of deallocate will call just one scoring thread.
            for (int i = 0; i < numThreads; i++) {
                mailbox.deallocate();
//                }
            }
        }

        areThreadsAllocated = false;
    }

     @Override
    public void deallocate() {
        super.deallocate();

        // normally it should not be necessary to stop the scoring threads manually, but who knows...
        stopScoringThreads();
    }


    @Override
    protected Data doScoring(List<? extends Scoreable> scoreableList, Data data) {
        Scoreable best;

        currentData = data;

        if (numThreads > 0) {

            int nThreads = numThreads;
            int scoreablesPerThread = scoreableList.size() / numThreads;

            // adapt the number of required scoring threads
            if (scoreablesPerThread < minScoreablesPerThread) {
                scoreablesPerThread = minScoreablesPerThread;
                nThreads = (scoreableList.size() + (scoreablesPerThread - 1)) / scoreablesPerThread;
            }

            semaphore.reset(nThreads);

            for (int i = 0; i < nThreads; i++) {
                int size = i == (nThreads - 1) ? scoreableList.size() - scoreablesPerThread * i : scoreablesPerThread;

                ScoreableJob job = new ScoreableJob(scoreableList, i * scoreablesPerThread, size);
                // why should do we need this special treatment of the last job?
//                if (i < (nThreads - 1)) {
                mailbox.post(job);
//                } else {
//                    Scoreable myBest = scoreScoreables(job);
//                    semaphore.post(myBest);
//                }
            }

            best = semaphore.pend();

        } else {
            ScoreableJob job = new ScoreableJob(scoreableList, 0, scoreableList.size());
            best = scoreScoreables(job);
        }

        currentData = null;
        
        return best;
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

        ListIterator<? extends Scoreable> iterator = job.getListIterator();

        for (int i = job.getStart(); i < end; i++) {
            Scoreable scoreable = iterator.next();

            // since we are potentially doing somethigns such as frame
            // skipping and grow skipping, this check can become
            // troublesome. Thus it is currently disabled.

            /*
             * if (false && scoreable.getFrameNumber() != currentData.getID()) {
             * throw new Error ("Frame number mismatch: Token: " +
             * scoreable.getFrameNumber() + " Data: " + currentData.getID()); }
             */

            if (scoreable.calculateScore(currentData) > best.getScore()) {
                best = scoreable;
            }
        }
        return best;
    }


    /**
     * A scoring thread waits for a new scoreable to arrive at the mailbox, scores it, and notifies when its done by
     * posting to the semaphore
     */
    class ScoringThread extends Thread {

        /** Creates a new ScoringThread. */
        ScoringThread() {
            setDaemon(true);
        }


        /** Waits for a scoreable job and scores the scoreable in the job, signally back when done */
        @Override
        public void run() {
            while (true) {
                ScoreableJob scoreableJob = mailbox.pend();
                if (scoreableJob == null) {
                    logger.finer("scorer thread " + this + "registered to " + ThreadedAcousticScorer.this + "dies ...");
                    break;  // this will happen when the scorer becomes deallocated
                }

                Scoreable best = scoreScoreables(scoreableJob);
                semaphore.post(best);
            }
        }
    }
}

/** Mailbox class allows a set of threads to communicate a single scoreable job */

class Mailbox {

    private ScoreableJob curScoreableJob;
    private boolean isAllocated = true;


    /**
     * Posts a scoreable to the mail box. The caller will block until the mailbox is empty and will then notify any
     * waiters
     * @param scoreableJob
     */
    synchronized void post(ScoreableJob scoreableJob) {
        while (curScoreableJob != null) {
            try {
                wait();
            } catch (InterruptedException ioe) {
            }
        }
        curScoreableJob = scoreableJob;
        notifyAll();
    }


    /**
     * Waits for a scoreable to arrive in the mailbox and returns it. This will block the caller until a scoreable
     * arrives
     *
     * @return the next scoreable
     */
    synchronized ScoreableJob pend() {
        ScoreableJob returnScoreableJob;
        while (curScoreableJob == null) {
            if (!isAllocated) {
                return null;
            }

            try {
                wait();
            } catch (InterruptedException ioe) {
            }
        }
        returnScoreableJob = curScoreableJob;
        curScoreableJob = null;
        notifyAll();
        return returnScoreableJob;
    }


    public synchronized void deallocate() {
        isAllocated = false;
        notifyAll();
    }
}

/** A counting semaphore */

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
            } catch (InterruptedException ioe) {
            }
        }
        return bestScoreable;
    }


    /**
     * Posts to the semaphore, decrementing the counter by one. should the counter arrive at zero, wake up any penders.
     *
     * @param postedBest the best scoreable encounted for this batch.
     */
    synchronized void post(Scoreable postedBest) {
        if (bestScoreable == null
                || postedBest.getScore() > bestScoreable.getScore()) {
            bestScoreable = postedBest;
        }
        count--;
        if (count <= 0) {
            notifyAll();
        }
    }

}

/** Represent a set of scoreables to be scored */

class ScoreableJob {

    private final List<? extends Scoreable> scoreables;
    private final int start;
    private final int size;


    /**
     * Creates a scoreable job
     *
     * @param scoreables the list of scoreables
     * @param start      the starting point for this job
     * @param size       the number of scoreables in this job
     */
    ScoreableJob(List<? extends Scoreable> scoreables, int start, int size) {
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
        return scoreables.get(start);
    }


    /**
     * Gets the entire list of scoreables
     *
     * @return the list of scoreables
     */
    List<? extends Scoreable> getScoreables() {
        return scoreables;
    }


    /**
     * Returns a ListIterator for this job.
     *
     * @return a ListIterator for this job.
     */
    ListIterator<? extends Scoreable> getListIterator() {
        return scoreables.listIterator(start);
    }


    /**
     * Returns a string representation of this object
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "List size " + scoreables.size() + " start " + start + " size "
                + size;
    }
}
