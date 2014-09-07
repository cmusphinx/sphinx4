/**
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.alignment;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;

import org.testng.annotations.Test;

import edu.cmu.sphinx.util.Utilities;

public class SpeechAlignerTest {

    @Test
    public void shouldAlignText() {
        align(asList("foo"), asList("bar"), -1);
        align(asList("foo"), asList("foo"), 0);
        align(asList("foo", "bar"), asList("foo"), 0);
        align(asList("foo", "bar"), asList("bar"), 1);
        align(asList("foo"), asList("foo", "bar"), 0, -1);
        align(asList("bar"), asList("foo", "bar"), -1, 0);
        align(asList("foo", "bar", "baz"), asList("foo", "baz"), 0, 2);
        align(asList("foo", "bar", "42", "baz", "qux"), asList("42", "baz"), 2,
                3);
    }

    private void align(List<String> database, List<String> query,
            Integer... result) {
        LongTextAligner aligner = new LongTextAligner(database, 1);
        int[] alignment = aligner.align(query);

        assertThat(Utilities.asList(alignment), contains(result));
    }
}
