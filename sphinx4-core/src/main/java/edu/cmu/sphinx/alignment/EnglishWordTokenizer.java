/**
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.alignment;

import java.util.List;
import java.util.Arrays;

public class EnglishWordTokenizer implements WordTokenizer {
    public List<String> getWords(String text) {
	String clean1 = text.replace("’", "'").replace("--", " ").replace(" - ", " ");
	String clean2 = clean1.replaceAll("[,.?:!;?()/»_]", "");
	String clean3 = clean2.toLowerCase();
	String[] tokens = clean3.split("\\s+");
	return Arrays.asList(tokens);
    }
}
