/*
 * Copyright 1999-2012 Carnegie Mellon University. Portions Copyright 2002 Sun
 * Microsystems, Inc. Portions Copyright 2002 Mitsubishi Electric Research
 * Laboratories. Portions Copyright 2012 Nexiwave All Rights Reserved. Use is
 * subject to license terms. See the file "license.terms" for information on
 * usage and redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.linguist.dictionary;

import static org.testng.AssertJUnit.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;


public class DictionaryTest {

    @Test
    public void testDictionary() throws IOException {
        URL dictUrl = getClass()
                .getResource("/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        URL noiseDictUrl = getClass()
                .getResource("/edu/cmu/sphinx/models/en-us/en-us/noisedict");

        Dictionary dictionary = new TextDictionary(dictUrl,
                                                   noiseDictUrl,
                                                   null,
                                                   null,
                                                   new UnitManager());
        dictionary.allocate();
        Word word = dictionary.getWord("one");

        assertThat(word.getPronunciations(), arrayWithSize(2));
        assertThat(word.getPronunciations()[0].toString(),
                   equalTo("one(W AH N )"));
        assertThat(word.getPronunciations()[1].toString(),
                   equalTo("one(HH W AH N )"));

        word = dictionary.getWord("something_missing");
        assertThat(word, nullValue());

        assertThat(dictionary.getSentenceStartWord().getSpelling(),
                   equalTo("<s>"));
        assertThat(dictionary.getSentenceEndWord().getSpelling(),
                   equalTo("</s>"));
        assertThat(dictionary.getSilenceWord().getSpelling(), equalTo("<sil>"));

        assertThat(dictionary.getFillerWords(), arrayWithSize(5));
    }
    
    @Test
    public void testBadDictionary() throws IOException {
        URL dictUrl = getClass()
                .getResource("/edu/cmu/sphinx/linguist/dictionary/bad.dict");
        URL noiseDictUrl = getClass()
                .getResource("/edu/cmu/sphinx/models/en-us/en-us/noisedict");

        Dictionary dictionary = new TextDictionary(dictUrl,
                                                   noiseDictUrl,
                                                   null,
                                                   null,
                                                   new UnitManager());
        boolean failed = false;
        try {
            dictionary.allocate();
        } catch (Error e) {
            failed = true;
        }
        assertTrue(failed);
   }
}
