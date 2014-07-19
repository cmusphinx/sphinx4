package edu.cmu.sphinx.alignment;

import java.util.List;

public interface WordTokenizer {
    List<String> getWords(String text);
}
