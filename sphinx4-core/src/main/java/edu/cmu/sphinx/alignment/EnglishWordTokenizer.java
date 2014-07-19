package edu.cmu.sphinx.alignment;

import static com.google.common.base.CharMatcher.forPredicate;
import static com.google.common.base.Splitter.on;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.*;
import com.google.common.collect.FluentIterable;

public class EnglishWordTokenizer implements WordTokenizer {
    private static final Predicate<Character> SEPARATOR =
            new Predicate<Character>() {
                public boolean apply(Character input) {
                    return CharMatcher.WHITESPACE.matches(input)
                            || input == '-';
                }
            };

    public List<String> getWords(String text) {
        Splitter splitter =
                on(forPredicate(SEPARATOR)).trimResults().omitEmptyStrings();
        final Pattern nonAlpha = Pattern.compile("[^a-z '’]");
        final Pattern tilde = Pattern.compile("[’]");
        return FluentIterable.from(splitter.split(text))
                .transform(new Function<String, String>() {
                    public String apply(String word) {
                        Matcher matcher = nonAlpha.matcher(word.toLowerCase());
                        return tilde.matcher(matcher.replaceAll(""))
                                .replaceAll("'");
                    }
                }).filter(new Predicate<String>() {
                    public boolean apply(String word) {
                        return !word.isEmpty();
                    }
                }).toList();
    }
}
