package edu.cmu.sphinx.linguist.language.ngram;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * 3-gram language model that can change its content at runtime.
 *
 * @author Alexander Solovets
 *
 */
public class DynamicTrigramModel implements BackoffLanguageModel {

    private Dictionary dictionary;
    private final Set<String> vocabulary;
    private int maxDepth;
    private float unigramWeight;

    private List<String> textWords;
    private Map<WordSequence, Float> logProbs;
    private Map<WordSequence, Float> logBackoffs;

    public DynamicTrigramModel() {
        vocabulary = newHashSet();
        logProbs = newHashMap();
        logBackoffs = newHashMap();
    }

    public DynamicTrigramModel(Dictionary dictionary) {
        this();
        this.dictionary = dictionary;
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);
        maxDepth = ps.getInt(PROP_MAX_DEPTH);
        unigramWeight = ps.getFloat(PROP_UNIGRAM_WEIGHT);
    }

    public void allocate() throws IOException {
        vocabulary.clear();
        logProbs.clear();
        logBackoffs.clear();

        vocabulary.addAll(textWords);
        List<Word> words = transform(textWords, new Function<String, Word>() {

            public Word apply(String word) {
                return fromNullable(dictionary.getWord(word)).or(Word.UNKNOWN);
            }
        });

        Multiset<WordSequence> unigrams = HashMultiset.create();
        Multiset<WordSequence> bigrams = HashMultiset.create();
        Multiset<WordSequence> trigrams = HashMultiset.create();

        if (words.size() > 0)
            unigrams.add(new WordSequence(words.get(0)));

        if (words.size() > 1) {
            unigrams.add(new WordSequence(words.get(1)));
            bigrams.add(new WordSequence(words.get(0), words.get(1)));
        }

        if (words.size() > 2) {
            bigrams.add(new WordSequence(words.get(1), words.get(2)));
            trigrams.add(new WordSequence(words.get(0), words.get(1), words
                    .get(2)));
        }

        for (int i = 2; i < words.size(); ++i) {
            unigrams.add(new WordSequence(words.get(i)));
            bigrams.add(new WordSequence(words.get(i - 1), words.get(i)));
            trigrams.add(new WordSequence(words.get(i - 2),
                                          words.get(i - 1),
                                          words.get(i)));
        }

        Map<WordSequence, Float> uniprobs = newHashMap();
        for (Multiset.Entry<WordSequence> e : unigrams.entrySet())
            uniprobs.put(e.getElement(),
                         (float) e.getCount() / unigrams.size());

        LogMath lmath = LogMath.getLogMath();
        float logUnigramWeight = lmath.linearToLog(unigramWeight);
        float invLogUnigramWeight = lmath.linearToLog(1 - unigramWeight);
        float logUniformProb = -lmath.linearToLog(uniprobs.size());
        float discount = .5f;

        for (WordSequence unigram : unigrams.elementSet()) {
            float sum = 0.f;
            for (WordSequence bigram : bigrams.elementSet()) {
                if (unigram.equals(bigram.getOldest()))
                    sum += uniprobs.get(bigram.getNewest());
            }
            float p = lmath.linearToLog(uniprobs.get(unigram));
            p += logUnigramWeight;
            p = lmath.addAsLinear(p, logUniformProb + invLogUnigramWeight);
            logProbs.put(unigram, p);
            logBackoffs.put(unigram, lmath.linearToLog(discount / (1 - sum)));
        }

        float deflate = 1 - discount;
        Map<WordSequence, Float> biprobs = newHashMap();
        for (Multiset.Entry<WordSequence> entry : bigrams.entrySet()) {
            int unigramCount = unigrams.count(entry.getElement().getOldest());
            biprobs.put(entry.getElement(),
                        entry.getCount() * deflate / unigramCount);
        }

        for (WordSequence biword : bigrams.elementSet()) {
            float sum = 0.f;
            for (WordSequence triword : trigrams.elementSet()) {
                if (biword.equals(triword.getOldest()))
                    sum += biprobs.get(triword.getNewest());
            }
            logProbs.put(biword, lmath.linearToLog(biprobs.get(biword)));
            logBackoffs.put(biword, lmath.linearToLog(sum));
        }

        for (Multiset.Entry<WordSequence> e : trigrams.entrySet()) {
            float p = e.getCount() * deflate;
            p /= bigrams.count(e.getElement().getOldest());
            logProbs.put(e.getElement(), lmath.linearToLog(p));
        }
    }

    public void deallocate() throws IOException {
    }

    public float getProbability(WordSequence wordSequence) {
        if (logProbs.containsKey(wordSequence)) {
            return logProbs.get(wordSequence);
        }
        if (wordSequence.size() > 1) {
            return fromNullable(logBackoffs.get(wordSequence.getOldest()))
                    .or(LogMath.LOG_ONE) +
                   getProbability(wordSequence.getNewest());
        }
        return LogMath.LOG_ZERO;
    }

    public float getSmear(WordSequence wordSequence) {
        // TODO: implement
        return 0;
    }

    public Set<String> getVocabulary() {
        return unmodifiableSet(vocabulary);
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setText(List<String> textWords) {
        this.textWords = textWords;
    }

    public ProbDepth getProbDepth(WordSequence wordSequence) {
        return new ProbDepth(getProbability(wordSequence), maxDepth);
    }
}
