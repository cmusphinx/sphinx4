package edu.cmu.sphinx.tools.corpusEditor;

import javolution.util.FastMap;
import javolution.util.FastSet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.Set;

/**
 * Dictionary =
 * DictionaryFile
 */
public class Dictionary {

    public Dictionary() {
    }

    public Dictionary(String dictionaryFile) {
        this.dictionaryFile = dictionaryFile;
    }

    Corpus corpus;
    public String dictionaryFile;
    Map<String,String[]> pronunciations = new FastMap<String,String[]>();
    Map<String,String> pronunciationStrings = new FastMap<String,String>();
    Set<String> allPhonemes = new FastSet<String>();

    public void init( Corpus c ) {
        try {

            corpus = c;

            LineNumberReader in = new LineNumberReader(new FileReader(dictionaryFile));

            for( String line = in.readLine(); line != null; line = in.readLine() ) {
                String [] fields = line.split("\\s");
                String [] pron = new String [fields.length -1];
                String s = "";
                for( int i=1; i<fields.length; i++ ) {
                    allPhonemes.add( fields[i] );
                    pron[i-1] = fields[i];
                    s += fields[i];
                }
                pronunciations.put( fields[0], pron );
                pronunciationStrings.put( fields[0], s );
            }
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }

    }

    public Set<String> getAllPhonemes() {
        return allPhonemes;
    }

    public String[] getPronunciation(Word w) {
        return pronunciations.get(w.getSpelling());
    }

    public String getPronunciationString(Word w) {
        return pronunciationStrings.get(w.getSpelling());
    }

    public boolean containsPronunciationSequence(Word w, String ps) {
        return getPronunciationString(w).contains( ps );
    }
}
