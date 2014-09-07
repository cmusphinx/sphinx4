package edu.cmu.sphinx.alignment;

import java.util.Arrays;
import java.util.List;


/**
 *
 * @author Alexander Solovets
 *
 */
public class SimpleWordExpander implements WordExpander {
    public List<String> expand(String text) {

        text = text.replace('’', '\'');
        text = text.replace('‘', ' ');
        text = text.replace('”', ' ');
        text = text.replace('“', ' ');
        text = text.replace('»', ' ');
        text = text.replace('«', ' ');
        text = text.replace('–', '-');
        text = text.replace('—', ' ');
        text = text.replace('…', ' ');

	text = text.replace(" - ", " ");	
	text = text.replaceAll("[,.?:!;?()/_*%]", " ");
	text = text.toLowerCase();
	
        String[] tokens = text.split("\\s+");
        return Arrays.asList(tokens);
    }
}
