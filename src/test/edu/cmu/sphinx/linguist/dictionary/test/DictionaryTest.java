/*
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2012 Nexiwave
 * 
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.dictionary.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;

public class DictionaryTest {
	@Test
	public void testDictionary() throws IOException {
		Dictionary dictionary = new FullDictionary(new URL(
				"file:models/acoustic/wsj/dict/digits.dict"), new URL(
				"file:models/acoustic/wsj/noisedict"), null, false, null,
				false, false, new UnitManager());
		dictionary.allocate();
		Word word;

		word = dictionary.getWord("one");
		assertEquals(2, word.getPronunciations().length);
		assertEquals("one(HH W AH N )", word.getPronunciations()[0].toString());
		assertEquals("one(W AH N )", word.getPronunciations()[1].toString());

		word = dictionary.getWord("something");
		assertNull(word);
		
		assertEquals("</s>", dictionary.getSentenceEndWord().getSpelling());
		assertEquals("<s>", dictionary.getSentenceStartWord().getSpelling());
		assertEquals("<sil>", dictionary.getSilenceWord().getSpelling());
		
		assertEquals(12, dictionary.getFillerWords().length);
	}
}
