package edu.cmu.sphinx.tools.corpusEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utterance =
 * BeginTime
 * EndTime
 * Transcript
 * Word *
 * [ID]
 * [Pronunciation]
 * [SpectrogramData]
 * [PCMData]
 * [PitchData]
 * [EnergyData]
 */
public class Utterance {

    public void init(Corpus corpus) {
        this.corpus = corpus;
        for (Word w : words) {
            w.init(this);
            corpus.addWord(w);
        }
    }

    Corpus corpus;
    String dataFileBase;
    Integer beginTime;
    Integer endTime;
    List<Word> words;

    public Utterance() {
    }

    public Utterance(String dataFileBase, int beginTime, int endTime, List<Word> words) {
        this.dataFileBase = dataFileBase;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.words = words;
    }

    public Utterance(String dataFileBase, int beginTime, int endTime) {
        this(dataFileBase, beginTime, endTime, new ArrayList<Word>());
    }

    public String getPcmFile() {
        return dataFileBase +".raw";
    }

    public String getPitchFile() {
        return dataFileBase +".pitch";
    }

    public String getEnergyFile() {
        return dataFileBase +".energy";
    }

    public Integer getBeginTime() {
        return beginTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public List<Word> getWords() {
        return words;
    }

    public void addWord(String spelling, int begin, int end) {
        Word w = new Word(spelling, begin, end, false);
        words.add(w);
    }

    public String getTranscript() {
        String s = "";
        for( Word w : words ) {
            s += w.spelling + " ";
        }
        return s;      
    }

    public String toString() {
        return dataFileBase + " " + beginTime + " " + endTime + " " + getTranscript();
    }
}
