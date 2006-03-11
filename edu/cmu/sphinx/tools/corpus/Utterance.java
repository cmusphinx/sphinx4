package edu.cmu.sphinx.tools.corpus;

import java.util.List;

/**
 * An Utterance represents a single connected sequence of spoken words.  It contains the transcript of this speech,
 * the associated audio data, and the individual Word objects that describe the sub-segments containing the data for
 * the individual words.
 */
public class Utterance  {

    protected Corpus corpus;
    protected List<Word> words;
    protected String transcript;
    protected RegionOfAudioData regionOfAudioData;

    public RegionOfAudioData getRegionOfAudioData() {
        return regionOfAudioData;
    }

    public int getBeginTime() {
        return regionOfAudioData.getBeginTime();
    }

    public int getEndTime() {
        return regionOfAudioData.getBeginTime();
    }

    public void setRegionOfAudioData(RegionOfAudioData regionOfAudioData) {
        this.regionOfAudioData = regionOfAudioData;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = corpus;
        for (Word w : words) {
            w.setUtterance(this);
            corpus.addWord(w);
        }
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Utterance() {
    }

    public void addWords(List<Word> words) {
        for (Word w : words) {
            addWord(w);
        }
    }

    public List<Word> getWords() {
        return words;
    }

    public void addWord(Word w) {
        words.add(w);
    }

    public String getTranscript() {
        String s = "";
        for (Word w : words) {
            s += w.spelling + " ";
        }
        return s;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public String toString() {
        return getRegionOfAudioData() + " " + getTranscript();
    }
}
