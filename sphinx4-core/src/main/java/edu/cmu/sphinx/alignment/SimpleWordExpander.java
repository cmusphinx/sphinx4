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
        String clean1 = text.replace("’", "'").replace("--", " ").replace(" - ", " ");
        String clean2 = clean1.replaceAll("[,.?:!;?()/»_]", "");
        String clean3 = clean2.toLowerCase();
        String[] tokens = clean3.split("\\s+");
        return Arrays.asList(tokens);
    }
}
