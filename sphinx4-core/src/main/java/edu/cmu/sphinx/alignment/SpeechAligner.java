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
import java.util.logging.Logger;

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

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

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
        long lastFrame = TimeFrame.INFINITE.getEnd();

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
                TimeFrame frame = timeFrames.poll();
                Range range = ranges.poll();
                
                System.out.println("---------------------------");
                System.out.println("Aligning frame " + frame + " to text " + text + " range " + range);

                if (i < 3) {
                    languageModel.setText(text);
                }

                recognizer.allocate();

                if (i == 3) {
                    grammar.setWords(text);
                }

                context.setSpeechSource(audioUrl.openStream(), frame);

                List<WordResult> hypothesis = newArrayList();
                Result result;
                while (null != (result = recognizer.recognize())) {
                    addAll(hypothesis, result.getTimedBestResult(false, i > 2));
                }

                if (i == 0) {
                    if (hypothesis.size() > 0) {
                        lastFrame = hypothesis.get(hypothesis.size() - 1).getTimeFrame().getEnd();
                    }
                }

                List<String> words = transform(hypothesis, toSpelling());
                int[] alignment = aligner.align(words, range);

                if (i < 5) {
                    List<WordResult> results = hypothesis;

                    System.out.println("--Result--------------------");

                    for (WordResult result1 : results) {
                        System.out.println(result1.getWord());
                    }
                    System.out.println("-----------------------");

                    int[] aid = alignment;
                    int lastId = -1;
                    for (int ij  = 0; ij < aid.length; ++ij) {
                        if (aid[ij] == -1) {
                            System.out.format("+ %s\n", results.get(ij));
                        } else {
                            if (aid[ij] - lastId > 1) {
                                for (String result1 : transcript.subList(lastId + 1,
                                                                   aid[ij])) {
                                    System.out.format("- %-25s\n", result1);
                                }
                            } else {
                                System.out.format("  %-25s\n", transcript.get(aid[ij]));
                            }
                            lastId = aid[ij];
                        }
                    }

                    if (lastId >= 0 && transcript.size() - lastId > 1) {
                        for (String result1 : transcript.subList(lastId + 1, transcript.size())) {
                            System.out.format("- %-25s\n", result1);
                        }
                    }

                }



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
                    checkedOffer(transcript, texts, timeFrames, ranges, prevKey + 1, e.getKey(), prevEnd, e.getValue()
                                 .getTimeFrame().getStart());
                }
                prevKey = e.getKey();
                prevEnd = e.getValue().getTimeFrame().getEnd();
            }
            if (transcript.size() - prevKey > 1) {
                checkedOffer(transcript, texts, timeFrames, ranges, prevKey + 1, transcript.size(), prevEnd, lastFrame);
            }
        }

        return newArrayList(alignedWords.values());
    }

    private void checkedOffer(List<String> transcript, Queue<List<String>> texts, Queue<TimeFrame> timeFrames, Queue<Range<Integer>> ranges, int start, int end, long timeStart, long timeEnd) {

        double wordDensity = ((double)(timeEnd - timeStart)) / (end - start);

        System.out.println("Word density " + wordDensity + " " + timeEnd  + " " + timeStart + " " + end + " " + start + " "  + transcript.subList(start,  end).toString());
        // Skip range if it's too short, average word is less than 10 milliseconds
        if (wordDensity < 10.0) {
            logger.info("Skipping text range due to a high density " + transcript.subList(start,  end).toString());
            return;
        }

        texts.offer(transcript.subList(start, end));
        timeFrames.offer(new TimeFrame(timeStart, timeEnd));
        ranges.offer(Range.closed(start, end - 1));
    }
}
