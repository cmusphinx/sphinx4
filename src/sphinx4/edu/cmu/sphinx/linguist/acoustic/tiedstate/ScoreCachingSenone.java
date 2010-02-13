package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.frontend.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implements a Senone that contains a cache of scores for previously used data.
 * <p>
 * Subclasses shoulod implement the abstract {@link #calculateScore} method,
 * which is called by the {@link #getScore} method to calculate the score
 * for each cache miss.
 * <p>
 * Note: this implementation is thread-safe and can be safely used
 * across different threads without external synchronization.
 *
 * @author Yaniv Kunda
 */
public abstract class ScoreCachingSenone implements Senone {

    private final ConcurrentMap<Data, Float> scoreCache = new ConcurrentHashMap<Data, Float>();

    /**
     * Gets the cached score for this senone based upon the given feature.
     * If the score was not cached, it is calculated using {@link #calculateScore},
     * cached, and then returned.  
     */
    @Override
    public float getScore(Data feature) {
        Float score = scoreCache.get(feature);
        if (score == null) {
            score = calculateScore(feature);
            scoreCache.putIfAbsent(feature, score);
        }
        return score;
    }

    /**
     * Calculates the score for this senone based upon the given feature.
     *
     * @param feature the feature vector to score this senone against
     * @return the score for this senone in LogMath log base
     */
    protected abstract float calculateScore(Data feature);

}
