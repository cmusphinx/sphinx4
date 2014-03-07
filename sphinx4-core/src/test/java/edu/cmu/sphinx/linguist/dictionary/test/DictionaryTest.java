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

import java.io.IOException;
import java.net.URL;

import org.testng.Assert;
import org.testng.annotations.Test;

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
		Assert.assertEquals(2, word.getPronunciations().length);
		Assert.assertEquals("one(HH W AH N )", word.getPronunciations()[0].toString());
		Assert.assertEquals("one(W AH N )", word.getPronunciations()[1].toString());

		word = dictionary.getWord("something");
		Assert.assertNull(word);
		
		Assert.assertEquals("</s>", dictionary.getSentenceEndWord().getSpelling());
		Assert.assertEquals("<s>", dictionary.getSentenceStartWord().getSpelling());
		Assert.assertEquals("<sil>", dictionary.getSilenceWord().getSpelling());
		
		Assert.assertEquals(12, dictionary.getFillerWords().length);
	}
}
