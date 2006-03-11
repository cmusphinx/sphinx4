package edu.cmu.sphinx.tools.corpus;

import javolution.xml.ObjectReader;
import javolution.xml.ObjectWriter;
import javolution.xml.XmlElement;
import javolution.xml.XmlFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;


/**
 * The Copus object encapsulates a collection of recordings associated with transcripts and dictionaries of pronunciations.
 * It contains all information needed to train and test a speech recognizers.
 * <br><br>
 * A Corpus is a tree data structure that contains meta-data, and references binary audio data stored externally.  A Corpus contains a collection of Utterances.  Each Utterance contains a collection
 * of Words.  Utterances and Words include begin and end times and a reference to an AudioData object that contains the audio data.
 * <br><br>
 * The Copus is initially created empty and then decorated with information and data as it becomes available.  As a typical
 * example, a Corpus might be created initially with Utterances containing only transcripts.  This Corpus would be then sent to a
 * CorpusRecorder tool that would create the AudioData objects and decorate the Utterances with begin and end times.  This Corpus
 * would then be sent to a ForceAlignRecognizerTool which would decorate the Word objects with begin and end times.  Finally,
 * the Corpus would be sent to a CorpusBrowser tool that would allow a human operator to edit the begin and end times and
 * remove bad data from the Corpus.
 * <br><br>
 * While in use, a Corpus is an in-memory data structure which references out-of-memory persistant binary data.  A Corpus
 * can be serialized as XML.  This XML is used to pass Corpra between users and tools.  The XML could be perhaps sent over a socket or stored as a file.  In the future we might consider
 * other forms of serialization such as serialized Java objects, or RDF.
 */
public class Corpus {

    protected Dictionary dictionary;
    protected List<Utterance> utterances = new ArrayList<Utterance>();
    protected Map<String, List<Word>> character2Words = new HashMap<String, List<Word>>();
    protected Map<String, List<Word>> spelling2Words = new HashMap<String, List<Word>>();
    protected Map<String, List<Word>> phonemeSequence2Words = new HashMap<String, List<Word>>();

    void init() {
        dictionary.init(this);
        for (Utterance u : utterances) {
            u.setCorpus(this);
        }
    }

    public Corpus() {
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<Utterance> getUtterances() {
        return utterances;
    }

    public void setUtterances(List<Utterance> utterances) {
        this.utterances = utterances;
    }

    public Set<String> getSpellings() {
        return spelling2Words.keySet();
    }

    public List<Word> getWords(String spelling) {
        return spelling2Words.get(spelling);
    }

    public void addUtterance(Utterance utterance) {
        utterances.add(utterance);
        for (Word w : utterance.getWords()) {
            addWord(w);
        }
    }

    void addWord(Word w) {

        addSpelling(w);

        addCharacters(w);

        addPhonemeSequences(w);
    }

    private void addPhonemeSequences(Word w) {
        String[] pron = dictionary.getPronunciation(w);
        if (pron != null) {
            for (int i = 0; i < pron.length; i++) {
                addPhonemeSequence(pron[i], w);
                if (i > 0) {
                    addPhonemeSequence(pron[i - 1] + " " + pron[i], w);
                }
                if (i < pron.length - 1) {
                    addPhonemeSequence(pron[i] + " " + pron[i + 1], w);
                }
                if (i > 0 && i < pron.length - 1) {
                    addPhonemeSequence(pron[i - 1] + " " + pron[i] + " " + pron[i + 1], w);
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

    public static Corpus readFromXML(InputStream in) {
        Corpus c = new ObjectReader<Corpus>().read(in);
        c.init();
        return c;
    }

    public void writeToXML(OutputStream out) throws IOException {
        ObjectWriter<Corpus> ow = new ObjectWriter<Corpus>();
        ow.write(this, out);
    }

    static final XmlFormat<Corpus> CorpusXMLFormat = new XmlFormat<Corpus>(Corpus.class) {

        public void format(Corpus c, XmlElement xml) {
            xml.add(c.getDictionary(), "dictionary");
            xml.add(c.getUtterances(), "utterances");
        }

        public Corpus parse(XmlElement xml) {
            Corpus c = xml.object();
            c.setDictionary((Dictionary) xml.get("dictionary"));
            c.setUtterances((List<Utterance>) xml.get("utterances"));
            return c;
        }
    };

    static final XmlFormat<Dictionary> DictionaryXMLFormat = new XmlFormat<Dictionary>(Dictionary.class) {

        public void format(Dictionary d, XmlElement xml) {
            xml.setAttribute("dictionaryFile", d.getDictionaryFile());
        }

        public Dictionary parse(XmlElement xml) {
            Dictionary d = xml.object();
            d.setDictionaryFile(xml.getAttribute("dictionaryFile", ""));
            return d;
        }

    };

    static final XmlFormat<AudioDatabase> AudioDatabaseXMLFormat = new XmlFormat<AudioDatabase>(AudioDatabase.class) {

        public void format(AudioDatabase adb, XmlElement xml) {
            xml.setAttribute("bitsPerSample", adb.getBitsPerSample());
            xml.setAttribute("samplesPerSecond", adb.getSamplesPerSecond());
            xml.setAttribute("channelCount", adb.getChannelCount());
        }

        public AudioDatabase parse(XmlElement xml) {
            AudioDatabase adb = xml.object();
            adb.setBitsPerSample(xml.getAttribute("bitsPerSample", -1));
            adb.setSamplesPerSecond(xml.getAttribute("samplesPerSecond", -1));
            adb.setChannelCount(xml.getAttribute("channelCount", -1));
            return adb;
        }

    };

    static final XmlFormat<RegionOfAudioData> RegionOfAudioDataXMLFormat = new XmlFormat<RegionOfAudioData>(RegionOfAudioData.class) {

        public void format(RegionOfAudioData r, XmlElement xml) {
            xml.add(r.getAudioDatabase(), "audioDatabase");
            xml.setAttribute("beginTime", r.getBeginTime());
            xml.setAttribute("endTime", r.getEndTime());
            xml.setAttribute("isExcluded", r.isExcluded());
            xml.add(r.getNotes(), "notes");
        }

        public RegionOfAudioData parse(XmlElement xml) {
            RegionOfAudioData r = xml.object();
            r.setAudioDatabase((AudioDatabase) xml.get("audioDatabase"));
            r.setBeginTime(xml.getAttribute("beginTime", -1));
            r.setEndTime(xml.getAttribute("endTime", -1));
            r.setExcluded(xml.getAttribute("isExcluded", false));
            r.setNotes((Collection<Note>) xml.get("notes"));
            return r;
        }

    };

    static final XmlFormat<Utterance> UtteranceXMLFormat = new XmlFormat<Utterance>(Utterance.class) {

        public void format(Utterance u, XmlElement xml) {
            xml.setAttribute("transcript", u.getTranscript());
            xml.add( u.getRegionOfAudioData(), "regionOfAudioData");
            xml.add(u.words, "words");
        }

        public Utterance parse(XmlElement xml) {
            Utterance u = xml.object();
            u.setTranscript( xml.getAttribute("transcript", "") );
            u.setRegionOfAudioData( (RegionOfAudioData) xml.get("regionOfAudioData"));
            u.setWords( (List<Word>) xml.get("words") );
            return u;
        }

    };

    static final XmlFormat<Word> WordXMLFormat = new XmlFormat<Word>(Word.class) {

        public void format(Word w, XmlElement xml) {
            xml.setAttribute("spelling", w.getSpelling());
            xml.add( w.getRegionOfAudioData(), "regionOfAudioData");
        }

        public Word parse(XmlElement xml) {
            Word w = xml.object();
            w.setSpelling( xml.getAttribute("spelling", "") );
            w.setRegionOfAudioData( (RegionOfAudioData) xml.get("regionOfAudioData") );
            return w;
        }

    };

}

