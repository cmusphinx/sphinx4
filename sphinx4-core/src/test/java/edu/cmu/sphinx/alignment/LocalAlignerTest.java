package edu.cmu.sphinx.alignment;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.asCharSource;

import java.io.IOException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharSource;

public class LocalAlignerTest extends TextAlignerSmallTest {

    @Override
    protected SequenceAligner<String> createAligner() {
        Splitter ws = Splitter.on(' ').trimResults().omitEmptyStrings();
        URL url = getClass().getResource("transcription-small.txt");
        CharSource source = asCharSource(url, Charsets.UTF_8);
        try {
            return new LocalAligner(newArrayList(ws.split(source.read())));
        } catch (IOException e) {
            throw new RuntimeException("test fixture not found", e);
        }
    }
}
