package edu.cmu.sphinx.linguist.language.ngram;

import java.io.IOException;
import java.util.*;

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
public class DynamicTrigramModel implements LanguageModel {

    private Dictionary dictionary;
    private final Set<String> vocabulary;
    private int maxDepth;
    private float unigramWeight;

    private List<String> sentences;
    private Map<WordSequence, Float> logProbs;
    private Map<WordSequence, Float> logBackoffs;

    public DynamicTrigramModel() {
        vocabulary = new HashSet<String>();
        logProbs = new HashMap<WordSequence, Float>();
        logBackoffs = new HashMap<WordSequence, Float>();
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
        HashMap<WordSequence, Integer> unigrams = new HashMap<WordSequence, Integer>();
        HashMap<WordSequence, Integer> bigrams = new HashMap<WordSequence, Integer>();
        HashMap<WordSequence, Integer> trigrams = new HashMap<WordSequence, Integer>();
        int wordCount = 0;

        for (String sentence : sentences) {
            String[] textWords = sentence.split("\\s+");
            List<Word> words = new ArrayList<Word>();
            words.add(dictionary.getSentenceStartWord());
            for (String wordString : textWords) {
        	if (wordString.length() == 0) {
        	    continue;
        	}
                vocabulary.add(wordString);
                Word word = dictionary.getWord(wordString);
                if (word == null) {
                    words.add(Word.UNKNOWN);
                } else {
                    words.add(word);
                }
            }
            words.add(dictionary.getSentenceEndWord());

            if (words.size() > 0) {
                addSequence(unigrams, new WordSequence(words.get(0)));
                wordCount++;
            }

            if (words.size() > 1) {
                wordCount++;
                addSequence(unigrams, new WordSequence(words.get(1)));
                addSequence(bigrams, new WordSequence(words.get(0), words.get(1)));
            }

            for (int i = 2; i < words.size(); ++i) {
                wordCount++;
                addSequence(unigrams, new WordSequence(words.get(i)));
                addSequence(bigrams, new WordSequence(words.get(i - 1), words.get(i)));
                addSequence(trigrams, new WordSequence(words.get(i - 2), words.get(i - 1), words.get(i)));
            }
        }

        float discount = .5f;
        float deflate = 1 - discount;
        Map<WordSequence, Float> uniprobs = new HashMap<WordSequence, Float>();
        for (Map.Entry<WordSequence, Integer> e : unigrams.entrySet()) {
            uniprobs.put(e.getKey(), (float) e.getValue() * deflate / wordCount);
        }

        LogMath lmath = LogMath.getLogMath();
        float logUnigramWeight = lmath.linearToLog(unigramWeight);
        float invLogUnigramWeight = lmath.linearToLog(1 - unigramWeight);
        float logUniformProb = -lmath.linearToLog(uniprobs.size());

        Set<WordSequence> sorted1grams = new TreeSet<WordSequence>(unigrams.keySet());
        Iterator<WordSequence> iter = new TreeSet<WordSequence>(bigrams.keySet()).iterator();
        WordSequence ws = iter.hasNext() ? iter.next() : null;
        for (WordSequence unigram : sorted1grams) {
            float p = lmath.linearToLog(uniprobs.get(unigram));
            p += logUnigramWeight;
            p = lmath.addAsLinear(p, logUniformProb + invLogUnigramWeight);
            logProbs.put(unigram, p);

            float sum = 0.f;
            while (ws != null) {
                int cmp = ws.getOldest().compareTo(unigram);
                if (cmp > 0) {
                    break;
                }
                if (cmp == 0) {
                    sum += uniprobs.get(ws.getNewest());
                }
                ws = iter.hasNext() ? iter.next() : null;
            }

            logBackoffs.put(unigram, lmath.linearToLog(discount / (1 - sum)));
        }

        Map<WordSequence, Float> biprobs = new HashMap<WordSequence, Float>();
        for (Map.Entry<WordSequence, Integer> entry : bigrams.entrySet()) {
            int unigramCount = unigrams.get(entry.getKey().getOldest());
            biprobs.put(entry.getKey(), entry.getValue() * deflate / unigramCount);
        }

        Set<WordSequence> sorted2grams = new TreeSet<WordSequence>(bigrams.keySet());
        iter = new TreeSet<WordSequence>(trigrams.keySet()).iterator();
        ws = iter.hasNext() ? iter.next() : null;
        for (WordSequence biword : sorted2grams) {
            logProbs.put(biword, lmath.linearToLog(biprobs.get(biword)));

            float sum = 0.f;
            while (ws != null) {
                int cmp = ws.getOldest().compareTo(biword);
                if (cmp > 0) {
                    break;
                }
                if (cmp == 0) {
                    sum += biprobs.get(ws.getNewest());
                }
                ws = iter.hasNext() ? iter.next() : null;
            }
            logBackoffs.put(biword, lmath.linearToLog(discount / (1 - sum)));
        }

        for (Map.Entry<WordSequence, Integer> e : trigrams.entrySet()) {
            float p = e.getValue() * deflate;
            p /= bigrams.get(e.getKey().getOldest());
            logProbs.put(e.getKey(), lmath.linearToLog(p));
        }
    }

    private void addSequence(HashMap<WordSequence, Integer> grams, WordSequence wordSequence) {
        Integer count = grams.get(wordSequence);
        if (count != null) {
            grams.put(wordSequence, count + 1);
        } else {
            grams.put(wordSequence, 1);
        }
    }

    public void deallocate() throws IOException {
    }

    public float getProbability(WordSequence wordSequence) {
        float prob;
        if (logProbs.containsKey(wordSequence)) {
            prob = logProbs.get(wordSequence);
        } else if (wordSequence.size() > 1) {
            Float backoff = logBackoffs.get(wordSequence.getOldest());
            if (backoff == null) {
                prob = LogMath.LOG_ONE + getProbability(wordSequence.getNewest());
            } else {
                prob = backoff + getProbability(wordSequence.getNewest());
            }
        } else {
            prob = LogMath.LOG_ZERO;
        }
        return prob;
    }

    public float getSmear(WordSequence wordSequence) {
        // TODO: implement
        return 0;
    }

    public Set<String> getVocabulary() {
        return vocabulary;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public void onUtteranceEnd() {
        //TODO not implemented
    }

    public void setText(List<String> sentences) {
        this.sentences = sentences;
    }
}
