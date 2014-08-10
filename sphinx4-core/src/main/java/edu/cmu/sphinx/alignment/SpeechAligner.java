package edu.cmu.sphinx.alignment;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newTreeMap;
import static com.google.common.collect.Queues.newArrayDeque;
import static edu.cmu.sphinx.result.WordResults.toSpelling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.google.common.collect.Range;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.linguist.language.ngram.DynamicTrigramModel;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.TimeFrame;


/**
 * TODO: fill
 *
 * @author Alexander Solovets
 *
 */
public class SpeechAligner {

    private static final int TUPLE_SIZE = 3;

    private final Context context;
    private final Recognizer recognizer;
    private final AlignerGrammar grammar;
    private final DynamicTrigramModel languageModel;

    /**
     * TODO: fill
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public SpeechAligner(String amPath, String dictPath, String g2pPath)
            throws MalformedURLException, IOException {
        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(amPath);
        configuration.setDictionaryPath(dictPath);

        context = new Context(configuration);
        if (g2pPath != null) {
            context.setLocalProperty("dictionary->allowMissingWords", "true");
            context.setLocalProperty("dictionary->createMissingWords", "true");
            context.setLocalProperty("dictionary->g2pModelPath", g2pPath);
            context.setLocalProperty("dictionary->g2pMaxPron", "2");
        }
        context.setLocalProperty("lexTreeLinguist->languageModel",
                                 "dynamicTrigramModel");
        recognizer = context.getInstance(Recognizer.class);
        grammar = context.getInstance(AlignerGrammar.class);
        languageModel = context.getInstance(DynamicTrigramModel.class);
    }

    /**
     * TODO: fill
     *
     * @param dataStream
     * @return
     * @throws IOException
     */
    public List<WordResult> align(URL audioUrl, String transcript)
            throws IOException {
        WordTokenizer tokenizer = new EnglishWordTokenizer();
        return align(audioUrl, tokenizer.getWords(transcript));
    }

    /**
     * TOOD: fill
     *
     * @param audioFile
     * @param transcript
     * @return
     * @throws IOException
     */
    public List<WordResult> align(URL audioUrl, List<String> transcript)
            throws IOException {
        LongTextAligner aligner = new LongTextAligner(transcript, TUPLE_SIZE);
        Map<Integer, WordResult> alignedWords = newTreeMap();
        Queue<Range<Integer>> ranges = newArrayDeque();
        Queue<List<String>> texts = newArrayDeque();
        Queue<TimeFrame> timeFrames = newArrayDeque();

        ranges.offer(Range.closed(0, transcript.size()));
        texts.offer(transcript);
        TimeFrame totalTimeFrame = TimeFrame.INFINITE;
        timeFrames.offer(totalTimeFrame);

        for (int i = 0; i < 4; ++i) {
            if (i == 3) {
                context.setLocalProperty("decoder->searchManager",
                        "alignerSearchManager");
                context.processBatch();
            }

            while (!texts.isEmpty()) {
                checkState(texts.size() == ranges.size());
                checkState(texts.size() == timeFrames.size());

                List<String> text = texts.poll();

                if (i < 3) {
                    languageModel.setText(text);
                }

                recognizer.allocate();

                if (i == 3) {
                    grammar.setWords(text);
                }

                context.setSpeechSource(audioUrl.openStream(),
                        timeFrames.poll());

                List<WordResult> hypothesis = newArrayList();
                Result result;
                while (null != (result = recognizer.recognize())) {
                    addAll(hypothesis, result.getTimedBestResult(false, i > 2));
                }

                List<String> words = transform(hypothesis, toSpelling());
                int[] alignment = aligner.align(words, ranges.poll());

                for (int j = 0; j < alignment.length; j++) {
                    if (alignment[j] != -1) {
                        alignedWords.put(alignment[j], hypothesis.get(j));
                    }
                }

                recognizer.deallocate();
            }

            int prevKey = -1;
            long prevEnd = 0;
            for (Map.Entry<Integer, WordResult> e : alignedWords.entrySet()) {
                if (e.getKey() - prevKey > 1) {
                    texts.offer(transcript.subList(prevKey + 1, e.getKey()));
                    timeFrames.offer(new TimeFrame(prevEnd, e.getValue()
                            .getTimeFrame().getStart()));
                    // TODO: 'texts' are not required, should use only 'ranges'
                    ranges.offer(Range.closed(prevKey + 1, e.getKey() - 1));
                }
                prevKey = e.getKey();
                prevEnd = e.getValue().getTimeFrame().getEnd();
            }
            if (transcript.size() - prevKey > 1) {
                texts.offer(transcript.subList(prevKey + 1, transcript.size()));
                // TODO: do not use MAX_VALUE
                timeFrames.offer(new TimeFrame(prevEnd, totalTimeFrame
                        .getEnd()));
                ranges.offer(Range.closed(prevKey + 1, transcript.size() - 1));
            }
        }

        return newArrayList(alignedWords.values());
    }

}
