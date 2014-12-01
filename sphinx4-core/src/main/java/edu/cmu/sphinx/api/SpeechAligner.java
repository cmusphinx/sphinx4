/*
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.cmu.sphinx.alignment.LongTextAligner;
import edu.cmu.sphinx.alignment.UsEnglishWordExpander;
import edu.cmu.sphinx.alignment.WordExpander;
import edu.cmu.sphinx.linguist.language.grammar.AlignerGrammar;
import edu.cmu.sphinx.linguist.language.ngram.DynamicTrigramModel;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.Range;
import edu.cmu.sphinx.util.TimeFrame;

/**
 * @author Alexander Solovets
 */
public class SpeechAligner {
    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private static final int TUPLE_SIZE = 3;

    private final Context context;
    private final Recognizer recognizer;
    private final AlignerGrammar grammar;
    private final DynamicTrigramModel languageModel;

    private WordExpander wordExpander;

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
        setWordExpander(new UsEnglishWordExpander());
    }

    /**
     * TODO: fill
     *
     * @param dataStream
     * @return
     * @throws IOException
     * @throws ProcessException
     */
    public List<WordResult> align(URL audioUrl, String transcript)
            throws IOException {
        return align(audioUrl, getWordExpander().expand(transcript));
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
        Map<Integer, WordResult> alignedWords = new TreeMap<Integer, WordResult>();
        Queue<Range> ranges = new LinkedList<Range>();
        Queue<List<String>> texts = new ArrayDeque<List<String>>();
        Queue<TimeFrame> timeFrames = new ArrayDeque<TimeFrame>();

        ranges.offer(new Range(0, transcript.size()));
        texts.offer(transcript);
        TimeFrame totalTimeFrame = TimeFrame.INFINITE;
        timeFrames.offer(totalTimeFrame);
        long lastFrame = TimeFrame.INFINITE.getEnd();

        for (int i = 0; i < 4; ++i) {
            if (i == 3) {
                context.setLocalProperty("decoder->searchManager",
                        "alignerSearchManager");
            }

            while (!texts.isEmpty()) {
                assert texts.size() == ranges.size();
                assert texts.size() == timeFrames.size();

                List<String> text = texts.poll();
                TimeFrame frame = timeFrames.poll();
                Range range = ranges.poll();

                logger.info("Aligning frame " + frame + " to text " + text
                        + " range " + range);

                if (i < 3) {
                    languageModel.setText(text);
                }

                recognizer.allocate();

                if (i == 3) {
                    grammar.setWords(text);
                }

                context.setSpeechSource(audioUrl.openStream(), frame);

                List<WordResult> hypothesis = new ArrayList<WordResult>();
                Result result;
                while (null != (result = recognizer.recognize())) {
                    hypothesis.addAll(result.getTimedBestResult(false));
                }

                if (i == 0) {
                    if (hypothesis.size() > 0) {
                        lastFrame = hypothesis.get(hypothesis.size() - 1)
                                .getTimeFrame().getEnd();
                    }
                }

                List<String> words = new ArrayList<String>();
                for (WordResult wr : hypothesis) {
                    words.add(wr.getWord().getSpelling());
                }
                int[] alignment = aligner.align(words, range);

                List<WordResult> results = hypothesis;

                logger.info("Decoding result is " + results);

                dumpAlignment(transcript, alignment, results);

                for (int j = 0; j < alignment.length; j++) {
                    if (alignment[j] != -1) {
                        alignedWords.put(alignment[j], hypothesis.get(j));
                    }
                }

                recognizer.deallocate();
            }

            scheduleNextAlignment(transcript, alignedWords, ranges, texts,
                    timeFrames, lastFrame);
        }

        return new ArrayList<WordResult>(alignedWords.values());
    }

    private void scheduleNextAlignment(List<String> transcript,
            Map<Integer, WordResult> alignedWords,
            Queue<Range> ranges, Queue<List<String>> texts,
            Queue<TimeFrame> timeFrames, long lastFrame) {
        int prevKey = -1;
        long prevEnd = 0;
        for (Map.Entry<Integer, WordResult> e : alignedWords.entrySet()) {
            if (e.getKey() - prevKey > 1) {
                checkedOffer(transcript, texts, timeFrames, ranges,
                        prevKey + 1, e.getKey(), prevEnd, e.getValue()
                                .getTimeFrame().getStart());
            }
            prevKey = e.getKey();
            prevEnd = e.getValue().getTimeFrame().getEnd();
        }
        if (transcript.size() - prevKey > 1) {
            checkedOffer(transcript, texts, timeFrames, ranges,
                    prevKey + 1, transcript.size(), prevEnd, lastFrame);
        }
    }

    private void dumpAlignment(List<String> transcript, int[] alignment,
            List<WordResult> results) {
        logger.info("Alignment");
        int[] aid = alignment;
        int lastId = -1;
        for (int ij = 0; ij < aid.length; ++ij) {
            if (aid[ij] == -1) {
                logger.info(String.format("+ %s", results.get(ij)));
            } else {
                if (aid[ij] - lastId > 1) {
                    for (String result1 : transcript.subList(lastId + 1,
                            aid[ij])) {
                        logger.info(String.format("- %-25s", result1));
                    }
                } else {
                    logger.info(String.format("  %-25s",
                            transcript.get(aid[ij])));
                }
                lastId = aid[ij];
            }
        }

        if (lastId >= 0 && transcript.size() - lastId > 1) {
            for (String result1 : transcript.subList(lastId + 1,
                    transcript.size())) {
                logger.info(String.format("- %-25s", result1));
            }
        }
    }

    private void checkedOffer(List<String> transcript,
            Queue<List<String>> texts, Queue<TimeFrame> timeFrames,
            Queue<Range> ranges, int start, int end, long timeStart,
            long timeEnd) {

        double wordDensity = ((double) (timeEnd - timeStart)) / (end - start);

        // Skip range if it's too short, average word is less than 10
        // milliseconds
        if (wordDensity < 10.0) {
            logger.info("Skipping text range due to a high density "
                    + transcript.subList(start, end).toString());
            return;
        }

        texts.offer(transcript.subList(start, end));
        timeFrames.offer(new TimeFrame(timeStart, timeEnd));
        ranges.offer(new Range(start, end - 1));
    }

    public WordExpander getWordExpander() {
        return wordExpander;
    }

    public void setWordExpander(WordExpander wordExpander) {
        this.wordExpander = wordExpander;
    }
}
