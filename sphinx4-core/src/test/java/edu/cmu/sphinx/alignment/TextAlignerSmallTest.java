package edu.cmu.sphinx.alignment;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.hamcrest.Matcher;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharSource;
import com.google.common.primitives.Ints;


public class TextAlignerSmallTest {

    @DataProvider(name = "words")
    public static Object[][] createWords() {
        return new Object[][] {
            // No match.
            {
                asList("foo", "foo"),
                contains(-1, -1)},
            // Align a single tuple.
            {
                asList("foo", "baz"),
                contains(2, 3)},
            // Align disjoint tuples.
            {
                asList("foo", "bar", "foo", "bar", "baz", "42"),
                contains(0, 1, 2, 4, 5, 6)},
            // Align overlapping tuples.
            {
                asList("foo", "bar", "foo", "baz", "bar"),
                contains(0, 1, 2, 3, 4)},
        // {
        // asList("foo", "bar", "foo", "x", "foo", "baz", "bar"),
        // contains(0, 1, 2, -1, -1, 3, 4)},
        // {
        // asList("foo", "bar", "foo", "foo", "baz", "bar", "42"),
        // contains(0, 1, 2, -1, -1, -1)},
        };
    }

    private LongTextAligner aligner;

    @BeforeClass
    public void setUp() throws IOException {
        Splitter ws = Splitter.on(' ').trimResults().omitEmptyStrings();
        URL url = getResource(getClass(), "transcription-small.txt");
        CharSource source = asCharSource(url, Charsets.UTF_8);
        aligner = new LongTextAligner(newArrayList(ws.split(source.read())), 2);
    }

    @Test(dataProvider = "words")
    public void align(List<String> words, Matcher<List<Integer>> matcher) {
        assertThat(Ints.asList(aligner.align(words)), matcher);
    }

    @Test(enabled=false)
    public void alignRange() {
    }
}
