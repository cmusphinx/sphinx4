package edu.cmu.sphinx.result;

import com.google.common.base.Function;
import com.google.common.base.Predicate;


public final class WordResults {

    private static final Function<WordResult, String> TO_SPELLING =
            new Function<WordResult, String>() {

                public String apply(WordResult input) {
                    return input.getWord().getSpelling();
                }
            };

    private static final Predicate<WordResult> IS_FILLER =
            new Predicate<WordResult>() {

                public boolean apply(WordResult wordResult) {
                    return wordResult.isFiller();
                }
            };

    public static final Function<WordResult, String> toSpelling() {
        return TO_SPELLING;
    }

    public static final Predicate<WordResult> isFiller() {
        return IS_FILLER;
    }
}
