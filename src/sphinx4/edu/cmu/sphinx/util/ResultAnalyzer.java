/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.util;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Compares a reference result strings to actual result strings and keeps track of statistics with regard to the
 * strings
 */
public class ResultAnalyzer {

    private final static DecimalFormat percent = new DecimalFormat("%.0");
    private int numSentences;
    private int numRefWords;
    private int numHypWords;
    private int numMatchingWords;
    private int numMatchingSentences;
    private int recognitionErrors;
    private int insertionErrors;
    private int deletionErrors;

    private final boolean verbose;

    private StringBuffer hypOutput;
    private StringBuffer refOutput;

    private final List<Misrecognition> mismatchedUtterances;


    /**
     * Creates a result analyzer
     *
     * @param verbose if true output comparisons as they are made
     */
    public ResultAnalyzer(boolean verbose) {
        this.verbose = verbose;
        this.mismatchedUtterances = new LinkedList<Misrecognition>();
    }


    /**
     * Compare the hypothesis to the reference string collecting statistics on it. If verbose was set to true,
     * statistics of the match sent to stdout.
     *
     * @param ref the reference string
     * @param hyp the hypothesis string
     * @return true if the reference and  hypothesis match
     */
    public boolean analyze(String ref, String hyp) {
        List<String> refList = stringToList(ref);
        List<String> hypList = stringToList(hyp);
        String filteredRef = toString(refList);
        String filteredHyp = toString(hypList);
        boolean match = false;

        hypOutput = new StringBuffer();
        refOutput = new StringBuffer();

        numRefWords += refList.size();
        numHypWords += hypList.size();
        numSentences++;

        while (!refList.isEmpty() || !hypList.isEmpty()) {

            if (refList.isEmpty()) {
                addInsert(refList, hypList);
            } else if (hypList.isEmpty()) {
                addDeletion(refList, hypList);
            } else if (!refList.get(0).equals(hypList.get(0))) {
                processMismatch(refList, hypList);
            } else {
                addMatch(refList, hypList);
            }
        }

        if (filteredHyp.equals(filteredRef)) {
            numMatchingSentences++;
            match = true;
        } else {
            mismatchedUtterances.add(new Misrecognition(ref, hyp));
        }

        if (verbose) {
            System.out.println();
            System.out.println("REF:       " + filteredRef);
            System.out.println("HYP:       " + filteredHyp);
            System.out.println("ALIGN_REF: " + refOutput);
            System.out.println("ALIGN_HYP: " + hypOutput);
            System.out.println();
            showResults();
        }

        return match;
    }


    /**
     * Returns the accuracy
     *
     * @return the accuracy between 0.0 and 1.0
     */
    public float getWordAccuracy() {
        if (numMatchingWords == 0 || numRefWords == 0) {
            return 0;
        } else {
            return ((float) numMatchingWords) / ((float) numRefWords);
        }
    }


    /**
     * Returns the sentence accuracy
     *
     * @return the accuracy between 0.0 and 1.0
     */
    public float getSentenceAccuracy() {
        if (numMatchingSentences == 0 || numSentences == 0) {
            return 0;
        } else {
            return ((float) numMatchingSentences) / ((float) numSentences);
        }
    }


    /** Returns the list of hypothesized words only  to a space separated string. */
    public String getHypothesis() {
        if (hypOutput == null) {
            return null;
        } else {
            return hypOutput.toString().trim();
        }
    }


    /** Resets all the accuracy and error statistics. */
    public void reset() {
        numSentences = 0;
        numRefWords = 0;
        numHypWords = 0;
        numMatchingWords = 0;
        numMatchingSentences = 0;
        recognitionErrors = 0;
        insertionErrors = 0;
        deletionErrors = 0;
        mismatchedUtterances.clear();
    }


    /**
     * convert the list of words back to a space separated string
     *
     * @param list the list of words
     * @return a space separated string
     */
    private String toString(List<String> list) {
        if (list.isEmpty())
            return "";        
        StringBuilder sb = new StringBuilder();
        for (String s : list)
            sb.append(s).append(' ');
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }


    /**
     * Add an insertion error corresponding to the first item on the hypList
     *
     * @param refList the list of reference words
     * @param hypList the list of hypothesis  words
     */
    private void addInsert(List<String> refList, List<String> hypList) {
        insertionErrors++;
        String word = hypList.remove(0);

        refOutput.append(' ').append(pad(word.length()));
        hypOutput.append(' ').append(word.toUpperCase());
    }


    /**
     * Add a deletion error corresponding to the first item on the refList
     *
     * @param refList the list of reference words
     * @param hypList the list of hypothesis  words
     */
    private void addDeletion(List<String> refList, List<String> hypList) {
        deletionErrors++;
        String word = refList.remove(0);

        refOutput.append(' ').append(word.toUpperCase());
        hypOutput.append(' ').append(pad(word.length()));
    }


    /**
     * Add a recognition error
     *
     * @param refList the list of reference words
     * @param hypList the list of hypothesis  words
     */
    private void addRecognitionError(List<String> refList, List<String> hypList) {
        recognitionErrors++;
        String ref = refList.remove(0);
        String hyp = hypList.remove(0);
        int length = Math.max(ref.length(), hyp.length());

        refOutput.append(' ').append(pad(ref.toUpperCase(), length));
        hypOutput.append(' ').append(pad(hyp.toUpperCase(), length));
    }


    /**
     * Add a match
     *
     * @param refList the list of reference words
     * @param hypList the list of hypothesis  words
     */
    private void addMatch(List<String> refList, List<String> hypList) {
        numMatchingWords++;
        String ref = refList.remove(0);
        String hyp = hypList.remove(0);
        refOutput.append(' ').append(ref);
        hypOutput.append(' ').append(hyp);
    }


    /**
     * Process a mismatch by seeing which type of error is most likely
     *
     * @param refList the list of reference words
     * @param hypList the list of hypothesis  words
     */
    private void processMismatch(List<String> refList, List<String> hypList) {
        int deletionMatches = countMatches(
                refList, 1, hypList, 0);
        int insertMatches = countMatches(
                refList, 0, hypList, 1);
        int normalMatches = countMatches(refList, 0, hypList, 0);

        if (deletionMatches > insertMatches &&
                deletionMatches > normalMatches) {
            addDeletion(refList, hypList);
        } else if (insertMatches > deletionMatches &&
                insertMatches > normalMatches) {
            addInsert(refList, hypList);
        } else {
            addRecognitionError(refList, hypList);
        }
    }


    /**
     * Counts the number of matches between the two lists starting at the respective indexes
     *
     * @param refList  the list of reference words
     * @param refIndex the starting point in the ref list
     * @param hypList  the list of hypothesis  words
     * @param hypIndex the starting point in the hyp list
     * @return the number of matching words
     */
    private int countMatches(List<String> refList, int refIndex,
                             List<String> hypList, int hypIndex) {
        int match = 0;

        while (refIndex < refList.size() && hypIndex < hypList.size()) {
            String ref = refList.get(refIndex++);
            String hyp = hypList.get(hypIndex++);
            if (ref.equals(hyp)) {
                match++;
            }
        }
        return match;
    }


    /**
     * Returns a string of "*" of the given length
     *
     * @param length the length of the resulting string
     * @return the string
     */
    private String pad(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append('*');
        }
        return result.toString();
    }


    /**
     * Pads the given string with spaces to the given length
     *
     * @param s      the string to pad
     * @param length the length of the resulting string
     * @return the padded string
     */
    private String pad(String s, int length) {
        StringBuilder result = new StringBuilder(length);
        result.append(s);
        for (int i = s.length(); i < length; i++) {
            result.append(' ');
        }
        return result.toString();
    }


    /**
     * Converts the given string to a list
     *
     * @param s the string to convert
     * @return a list, one word per item with silences removed
     */
    private List<String> stringToList(String s) {
        List<String> list = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(s);

        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            list.add(word);
        }
        return list;
    }


    /** Shows the misrecognized utterances. */
    public void showMisrecognitions() {
        System.out.println
                (mismatchedUtterances.size() + " sentence errors");
        for (Misrecognition misrecognition : mismatchedUtterances) {
            System.out.println(misrecognition.getReference());
            System.out.println(misrecognition.getHypothesis());
        }
    }


    /** Shows the results for this analyzer */
    public void showResults() {
        if (numSentences > 0) {
            int totalErrors = recognitionErrors
                    + insertionErrors + deletionErrors;
            float wordErrorRate = (recognitionErrors + insertionErrors
                    + deletionErrors) / (float) numRefWords;

            System.out.print("   Accuracy: " +
                    percent.format(getWordAccuracy()));
            System.out.println("    Errors: " + totalErrors +
                    "  (Sub: " + recognitionErrors +
                    "  Ins: " + insertionErrors +
                    "  Del: " + deletionErrors + ')');
            System.out.println("   Words: " + numRefWords +
                    "   Matches: " + numMatchingWords +
                    "    WER: " + percent.format(wordErrorRate));
            System.out.println("   Sentences: " + numSentences +
                    "   Matches: " + numMatchingSentences +
                    "   SentenceAcc: " +
                    percent.format(getSentenceAccuracy()));
        }
    }


    /**
     * Quick and dirty test program
     *
     * @param args the commandline arguments
     */
    public static void main(String[] args) {
        ResultAnalyzer ra = new ResultAnalyzer(true);

        ra.analyze("a", "a");
        ra.analyze("a", "b");
        ra.analyze("a", "");
        ra.analyze("", "a");
        ra.analyze("a b", "a b");
        ra.analyze("a b", "a");
        ra.analyze("a b", "b");
        ra.analyze("a b", "c c");
        ra.analyze("aaa bbb ccc", "aaaa bbbb cccc");
        ra.analyze("aaa bbb ccc ddd", "aaa bbb bbb ccc ddd");
        ra.analyze("aaa bbb ccc ddd", "aaa bbb ccc ddd");

        ra.analyze("a b c d e f", "a z b c e f");

        ra.showResults();
    }
}

/** Represents the reference and hypothesis of a misrecognized utterance. */
class Misrecognition {

    private final String reference;
    private final String hypothesis;


    /**
     * Constructs a Misrecognition.
     *
     * @param reference  the reference utterance
     * @param hypothesis the hypothesis utterance
     */
    public Misrecognition(String reference, String hypothesis) {
        this.reference = reference;
        this.hypothesis = hypothesis;
    }


    /**
     * Returns the reference.
     *
     * @return the reference string
     */
    public String getReference() {
        return "REF: " + reference;
    }


    /**
     * Returns the hypothesis string.
     *
     * @return the hypothesis string
     */
    public String getHypothesis() {
        return "HYP: " + hypothesis;
    }
}
