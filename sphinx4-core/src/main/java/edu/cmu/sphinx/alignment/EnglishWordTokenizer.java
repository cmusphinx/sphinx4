package edu.cmu.sphinx.alignment;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

public class EnglishWordTokenizer implements WordTokenizer {
    public List<String> getWords(String text) {
	String clean1 = text.replace("’", "'").replace("--", " ");
	String clean2 = clean1.replaceAll("[,.?:!?()/»_]", "");
	String clean3 = clean2.toLowerCase();
	String[] tokens = clean3.split("\\s+");
	return Arrays.asList(tokens);
    }
}
