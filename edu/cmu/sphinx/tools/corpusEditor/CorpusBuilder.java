package edu.cmu.sphinx.tools.corpusEditor;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.linguist.flat.FlatLinguist;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.tools.batch.BatchForcedAlignerRecognizer;
import edu.cmu.sphinx.tools.corpus.*;
import edu.cmu.sphinx.util.props.*;
import javolution.util.FastSet;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Jan 18, 2006
 * Time: 12:44:53 PM
 */
public class CorpusBuilder extends BatchForcedAlignerRecognizer {
    private static final String PROP_EXCLUDE_UTTERANCES = "excludeUtterances";

    protected Set<Integer> excludedUtterances;

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
    *      edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_EXCLUDE_UTTERANCES, PropertyType.STRING);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        String eu = ps.getString(PROP_EXCLUDE_UTTERANCES, "");
        excludedUtterances = new FastSet<Integer>();
        for (String uid : eu.split(",")) {
            excludedUtterances.add(new Integer(uid));
        }
        FlatLinguist fl = (FlatLinguist) (recognizer.getDecoder().getSearchManager().getLinguist());
        String dictionaryFile;

	dictionaryFile = fl.getGrammar().getDictionary().getWordDictionaryFile().getPath();

        Dictionary dictionary = new Dictionary();
        dictionary.setDictionaryFile(dictionaryFile);

        corpus = new Corpus();
        corpus.setDictionary(dictionary);
    }



    public void decode() {

        try {
            utteranceId = 0;
            DataOutputStream ctm = new DataOutputStream(new FileOutputStream(ctmFile));
            recognizer.allocate();

            for (Iterator i = new CTLIterator(); i.hasNext();) {
                CTLUtterance utt = (CTLUtterance) i.next();

                if (excludedUtterances.contains(utteranceId)) {
                    System.out.println("Excluding utterance " + utteranceId);
                } else {
                    setInputStream(utt);
                    Result result = recognizer.recognize();
                    System.out.println("Utterance " + utteranceId + ": " + utt.getName());
                    System.out.println("Reference: " + utt.getRef());
                    System.out.println("Result   : " + result);
                    logger.info("Utterance " + utteranceId + ": " + utt.getName());
                    logger.info("Result   : " + result);
                    handleResult(ctm, utt, result);
                }
                utteranceId++;
            }

            recognizer.deallocate();
        } catch (IOException io) {
            logger.severe("I/O error during decoding: " + io.getMessage());
        }
        logger.info("BatchCTLDecoder: " + utteranceId + " utterances decoded");
    }

    private String stripExtension( String fileName  ) {
        return fileName.substring(0,fileName.lastIndexOf('.'));
    }

    protected void handleResult(DataOutputStream out, CTLUtterance utt, Result result) throws IOException {
        Utterance utterance = new Utterance();
        AudioDatabase adb = new PCMFileAudioDatabase();
        adb.setPcmFileName( utt.getFile() );
        adb.setBitsPerSample( 16 );
        adb.setChannelCount( 1 );
        adb.setSamplesPerSecond( 16000 );
        RegionOfAudioData rad = new RegionOfAudioData();
        rad.setBeginTime(utt.getStartOffset());
        rad.setEndTime(utt.getEndOffset());
        addWords(utterance, result.getBestToken(), utterance.getEndTime(), adb);
        corpus.addUtterance(utterance);
        System.out.println(utt + " --> " + result);
    }

    // add all the lastWord and all the words from this token and before
    private void addWords(Utterance utterance, Token token, int nextBegin, AudioDatabase adb) {
        if (token != null) {

            edu.cmu.sphinx.linguist.dictionary.Word word = token.getWord();
            int begin = token.getFrameNumber() + utterance.getBeginTime();

            addWords(utterance, token.getPredecessor(), begin, adb);

            if (word != null) {
                String spelling = word.getSpelling();
                Word w = new Word();
                w.setSpelling(spelling);
                RegionOfAudioData rad = new RegionOfAudioData();
                rad.setAudioDatabase(adb);
                rad.setBeginTime( begin );
                rad.setEndTime( nextBegin );
                rad.setExcluded( false);
                w.setRegionOfAudioData(rad);
                utterance.addWord(w);
                System.out.println(token.getWord() + " " + begin);
            }
        }
    }

    protected Corpus corpus = null;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(
                    "Usage: CorpusBuilder propertiesFile");
            System.exit(1);
        }

        String propertiesFile = args[0];

        try {
            URL url = new File(propertiesFile).toURI().toURL();
            ConfigurationManager cm = new ConfigurationManager(url);

            CorpusBuilder me = (CorpusBuilder) cm.lookup("batchNIST");

            if (me == null) {
                System.err.println("Can't find batchCTL in " + propertiesFile);
                return;
            }

            me.decode();

            me.corpus.writeToXML( new FileOutputStream("dump.xml") );

            Corpus c2 = Corpus.readFromXML( new FileInputStream("dump.xml") );


        } catch (IOException ioe) {
            System.err.println("I/O error during initialization: \n   " + ioe);
            return;
        } catch (InstantiationException e) {
            System.err.println("Error during initialization: \n  " + e);
            return;
        } catch (PropertyException e) {
            System.err.println("Error during initialization: \n  " + e);
            return;
        }


    }

    /*
    static void test1() {
        Corpus.Word dog = new Corpus.Word("Dog", 123, 256, false);
        Corpus.Word cat = new Corpus.Word("Cat", 456, 789, true);
        ArrayList l = new ArrayList();
        l.add(dog);
        l.add(cat);
        Corpus.Utterance utt = new Corpus.Utterance(100, 800, l);

        Corpus.Word bread = new Corpus.Word("Dog", 100, 200, false);
        Corpus.Word butter = new Corpus.Word("Butter", 300, 400, true);
        ArrayList l2 = new ArrayList();
        l2.add(bread);
        l2.add(butter);
        Corpus.Utterance utt2 = new Corpus.Utterance(100, 500, l2);

        ArrayList l3 = new ArrayList();
        l3.add(utt);
        l3.add(utt2);

        Corpus corpus = new Corpus(
                new Corpus.Dictionary("foo.dict"),
                new Corpus.Waveform("foo.pcm", 16, 16000, 1),
                new Corpus.Spectrogram(),
                new Corpus.Pitch(),
                new Corpus.Energy(),
                l3);

        ObjectWriter<Corpus.Utterance> uttWriter = new ObjectWriter<Corpus.Utterance>();
        ObjectWriter<Corpus> corpusWriter = new ObjectWriter<Corpus>();

        //writer.setClassIdentifierEnabled(false);
        try {
            ObjectWriter ow = new ObjectWriter();
            OutputStream oc = new FileOutputStream("corpus.xml");
            Writer oc2 = new Utf8StreamWriter().setOutputStream(oc); // UTF-8 encoding.

            WriterHandler corpusHandler = new WriterHandler().setWriter(oc2);
            corpusHandler.setIndent("\t"); // Indents with tabs.
            corpusHandler.setProlog("<?xml version=\"1.0\" encoding=\"UTF-8\"/>");

            ow.write(corpus, corpusHandler);
            uttWriter.write(utt, new FileOutputStream("utt.xml"));
            corpusWriter.write(corpus, new FileOutputStream("corpus.xml"));
            Corpus.Utterance u2 = new ObjectReader<Corpus.Utterance>().read(new FileInputStream("utt.xml"));
            Corpus c2 = new ObjectReader<Corpus>().read(new FileInputStream("corpus.xml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
    */
}
