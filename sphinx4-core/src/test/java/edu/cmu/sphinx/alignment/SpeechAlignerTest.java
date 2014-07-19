package edu.cmu.sphinx.alignment;

import static edu.cmu.sphinx.alignment.SpeechAligner.alignText;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.primitives.Ints;

public class SpeechAlignerTest {

    @Test
    public void shouldAlignText() {
        align(asList("foo"), asList("bar"), -1);
        align(asList("foo"), asList("foo"), 1);
        align(asList("foo", "bar"), asList("foo"), 1);
        align(asList("foo", "bar"), asList("bar"), 2);
        align(asList("foo"), asList("foo", "bar"), 1, -1);
        align(asList("bar"), asList("foo", "bar"), -1, 1);
        align(asList("foo", "bar", "baz"), asList("foo", "baz"), 1, 3);
        align(asList("foo", "bar", "42", "baz", "qux"), asList("42", "baz"), 3, 4);
    }

    private void align(List<String> database, List<String> query, Integer... result) {
        int[] alignment = alignText(database, query, 1);
        assertThat(Ints.asList(alignment), contains(result));
    }
}
