package edu.cmu.sphinx.alignment;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.primitives.Ints;

public abstract class TextAlignerSmallTest {

    protected abstract SequenceAligner<String> createAligner();

    @DataProvider(name = "words")
    public static Object[][] createWords() {
        return new Object[][] {
                // Align a single tuple.
                {asList("foo", "baz"), contains(2, 3)},
                // Align disjoint tuples.
                {asList("foo", "bar", "foo", "bar", "baz", "42"),
                        contains(0, 1, 2, 4, 5, 6)},
                // Align overlapping tuples.
                {asList("foo", "bar", "foo", "baz", "bar"),
                        contains(0, 1, 2, 3, 4)},
                {asList("foo", "bar", "foo", "x", "foo", "baz", "bar"),
                        contains(0, 1, 2, -1, -1, 3, 4)},
                {asList("foo", "bar", "foo", "foo", "baz", "bar", "42"),
                        contains(0, 1, 2, -1, -1, -1)},};
    }

    private SequenceAligner<String> aligner;

    @BeforeClass
    public void setUp() {
        aligner = createAligner();
    }

    @Test(dataProvider = "words")
    public void align(List<String> words, Matcher<List<Integer>> matcher) {
        assertThat(Ints.asList(aligner.align(words)), matcher);
    }

    @Test()
    public void alignSequenceOfTwoWords() {
        List<String> words = newArrayList();
        for (int i = 0; i < 20; ++i) {
            words.addAll(asList("foo", "bar"));
        }
        List<String> words2 = newArrayList(words.subList(1, words.size()));
        words2.addAll(words);

        for (int i = 0; i < 20; ++i) {
            words.add("baz");
        }
        aligner = new LongTextAligner(words, 1);
        int[] ids = aligner.align(words2);
        System.err.println(Arrays.toString(ids));
        assertThat(Ints.asList(ids), hasItem(-1));
    }
}
