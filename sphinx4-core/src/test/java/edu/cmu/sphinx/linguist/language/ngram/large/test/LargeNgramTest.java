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
package edu.cmu.sphinx.linguist.language.ngram.large.test;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.FullDictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;

public class LargeNgramTest {
	@Test
	public void testNgram() throws IOException {
		Dictionary dictionary = new FullDictionary(new URL(
				"file:src/test/edu/cmu/sphinx/linguist/language/ngram/large/test/100.dict"), new URL(
				"file:models/acoustic/wsj/noisedict"), null, false, null,
				false, false, new UnitManager());		
		LargeTrigramModel model = new LargeTrigramModel("", new URL("file:src/test/edu/cmu/sphinx/linguist/language/ngram/large/test/100.arpa.dmp"), null, 100, 100, false, 3, dictionary, false, 1.0f, 1.0f, 1.0f, false);
		dictionary.allocate();
		model.allocate();
		Assert.assertEquals(3, model.getMaxDepth());
		
		Word[] words = {new Word("huggins",  null, false), new Word("daines",  null, false)};
		Assert.assertEquals(-830.862f, model.getProbability(new WordSequence(words)), 0.001f);

		Word[] words1 = {new Word("huggins",  null, false), new Word("daines",  null, false), new Word("david",  null, false)};
		Assert.assertEquals(-67625.77, model.getProbability(new WordSequence(words1)), 0.01f);
	}
}
