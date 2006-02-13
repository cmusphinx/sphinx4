package edu.cmu.sphinx.tools.corpusEditor;

import java.util.*;

/**
 * Corpus = Dictionary PCMDescriptor Spectrogram Pitch Energy Utterance *
 */
public class Corpus {

    Dictionary dictionary;
    Waveform waveform;
    Spectrogram spectrogram;

    Pitch pitch;
    Energy energy;
    int bytesPerMillisecond;

    List<Utterance> utterances;
    List<String> characters;

    Map<String, List<Word>> character2Words = new HashMap<String, List<Word>>();
    Map<String, List<Word>> spelling2Words = new HashMap<String, List<Word>>();
    Map<String, List<Word>> phonemeSequence2Words = new HashMap<String, List<Word>>();

    public void init() {
        dictionary.init(this);
        for (Utterance u : utterances) {
            u.init(this);
        }
        bytesPerMillisecond = ((waveform.bitsPerSample / 8) * waveform.channelCount * waveform.samplesPerSecond) / 100;
    }

    public Corpus() {
    }


    public Corpus(String dictionaryFile, int bitsPerSample, int samplesPerSecond, int channelCount, String spectrogramFile, String pitchFile, String energyFile) {
        this.dictionary = new Dictionary(dictionaryFile);
        this.waveform = new Waveform(bitsPerSample, samplesPerSecond, channelCount);
        this.spectrogram = new Spectrogram();
        this.pitch = new Pitch();
        this.energy = new Energy();
        this.utterances = new ArrayList<Utterance>();
    }

    Set<String> getSpellings() {
        return spelling2Words.keySet();
    }

    List<Word> getWords(String spelling) {
        return spelling2Words.get(spelling);
    }

    public void addUtterance(Utterance utterance) {
        utterances.add(utterance);
        for (Word w : utterance.getWords()) {
            addWord(w);
        }
    }

    public Utterance newUtterance(String pcmFile, int beginTime, int endTime) {
        return new Utterance(pcmFile, beginTime, endTime);
    }


    public void addWord(Word w) {

        addSpelling(w);

        addCharacters(w);

        addPhonemeSequences(w);
    }
    /*
    private void addPhonemeSequences(Word w) {
        Set<String> allPhonemes = dictionary.getAllPhonemes();
        String[] pron = dictionary.getPronunciation(w);
        if (pron != null ) {
            for (String p1 : pron) {
                addPhonemeSequence(p1, w);
                for (String p0 : allPhonemes) {
                    addPhonemeSequence(p0 + " " + p1, w);
                    addPhonemeSequence(p1 + " " + p0, w);
                    for (String p2 : allPhonemes) {
                        addPhonemeSequence(p0 + " " + p1 + " " + p2, w);
                    }
                }
            }
        }
    }
    */

    private void addPhonemeSequences(Word w) {
        String[] pron = dictionary.getPronunciation(w);
        if (pron != null ) {
            for ( int i=0; i<pron.length; i++ ) {
                addPhonemeSequence(pron[i], w);
                if( i > 0 ) {
                    addPhonemeSequence(pron[i-1] + " " + pron[i], w);
                }
                if( i < pron.length-1 ) {
                    addPhonemeSequence(pron[i] + " " + pron[i+1], w);
                }
                if( i > 0 && i < pron.length-1 ) {
                    addPhonemeSequence(pron[i-1] + " " + pron[i] + " " + pron[i+1], w);
                }
            }
        }
    }

    private void addCharacters(Word w) {
        for (String character : w.getCharacters()) {
            addCharacter(character, w);
        }
    }

    private void addSpelling(Word w) {
        if (!spelling2Words.containsKey(w.spelling)) {
            spelling2Words.put(w.spelling, new ArrayList<Word>());
        }
        List<Word> l = spelling2Words.get(w.spelling);
        l.add(w);
    }

    private void addCharacter(String character, Word w) {
        if (!character2Words.containsKey(character)) {
            character2Words.put(character, new ArrayList<Word>());
        }
        List<Word> l = character2Words.get(character);
        l.add(w);
    }

    private void addPhonemeSequence(String ps, Word w) {
            if (!phonemeSequence2Words.containsKey(ps)) {
                phonemeSequence2Words.put(ps, new ArrayList<Word>());
            }
            List<Word> l = phonemeSequence2Words.get(ps);
            l.add(w);
    }

    public int time2Offet(int time) {
        return time * bytesPerMillisecond;
    }

    public List<Utterance> getUtterances() {
        return utterances;
    }

    public Set<String> getCharacters() {
        return character2Words.keySet();
    }

    public Set<String> getPhonemeSequences() {
        return phonemeSequence2Words.keySet();
    }

    public List<Word> character2Words(String character) {
        return character2Words.get(character);
    }

    public List<Word> phonemeSequence2Words(String phoneme) {
        return phonemeSequence2Words.get(phoneme);
    }
}
