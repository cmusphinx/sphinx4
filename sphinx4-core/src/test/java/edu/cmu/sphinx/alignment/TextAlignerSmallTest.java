package edu.cmu.sphinx.alignment;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.hamcrest.Matcher;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import edu.cmu.sphinx.util.Utilities;

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
        URL url = getClass().getResource("transcription-small.txt");
        ArrayList<String> words = new ArrayList<String>();
        Scanner scanner = new Scanner(url.openStream());
        while (scanner.hasNext()) {
            words.add(scanner.next());
        }
        scanner.close();
        aligner = new LongTextAligner(words, 2);
    }

    @Test(dataProvider = "words")
    public void align(List<String> words, Matcher<List<Integer>> matcher) {
        assertThat(Utilities.asList(aligner.align(words)), matcher);
    }

    @Test(enabled=false)
    public void alignRange() {
    }
}
