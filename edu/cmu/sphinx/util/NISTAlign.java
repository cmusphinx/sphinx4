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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;

/**
 * Implements a portion of the NIST align/scoring algorithm to compare
 * a reference string to a hypothesis string.  It only keeps track of
 * substitutions, insertions, and deletions.
 */
public class NISTAlign {

    /* Constants that help with the align.  The following are
     * used in the backtrace table and backtrace list.
     */
    final static int OK = 0;
    final static int SUBSTITUTION = 1;
    final static int INSERTION = 2;
    final static int DELETION = 3;

    /* Constants that help with the align.  The following are
     * used to create the penalty table.
     */
    final static int MAX_PENALTY = 1000000;
    final static int SUBSTITUTION_PENALTY = 100;
    final static int INSERTION_PENALTY = 75;
    final static int DELETION_PENALTY = 75;

    /* Used for padding out aligned strings.
     */
    final static String STARS  =
        "********************************************";
    final static String SPACES =
        "                                            ";
    final static String HRULE =
        "============================================"
        + "================================";
    
    /**
     * Totals over the life of this class.  These can be reset to 0
     * with a call to resetTotals.
     */
    private int totalSentences;
    private int totalSentencesWithErrors;
    private int totalSentencesWithSubtitutions;
    private int totalSentencesWithInsertions;
    private int totalSentencesWithDeletions;
    private int totalReferenceWords;
    private int totalHypothesisWords;
    private int totalAlignedWords;
    private int totalWordsCorrect;
    private int totalSubstitutions;
    private int totalInsertions;
    private int totalDeletions;
    
    /**
     * Error values for one call to 'align'
     */
    private int substitutions;
    private int insertions;
    private int deletions;
    private int correct;

    /**
     * The raw reference string.  Updated with each call to 'align'.
     */
    private String rawReference;

    /**
     * The reference annotation; typically the name of the
     * audio file for the reference string.  This is an optional
     * part of the rawReference string.  If it is included, it
     * is appended to the end of the string in parentheses.
     * Updated with each call to 'align'.
     */
    private String referenceAnnotation;
    
    /**
     * Ordered list of words from rawReference after the annotation
     * has been removed.  Updated with each call to 'align'.
     */
    private LinkedList referenceWords;

    /**
     * Aligned list of words from rawReference.  Created in
     * alignWords.  Updated with each call to 'align'.
     */
    private LinkedList alignedReferenceWords;

    /**
     * The raw hypothesis string.  Updated with each call to
     * 'align'.
     */
    private String rawHypothesis;

    /**
     * Ordered list of words from rawHypothesis after the annotation
     * has been removed.  Updated with each call to 'align'.
     */
    private LinkedList hypothesisWords;

    /**
     * Aligned list of words from rawHypothesis.  Created in
     * alignWords.  Updated with each call to 'align'.
     */
    private LinkedList alignedHypothesisWords;

    /**
     * Helpers to create percentage strings.
     */
    static DecimalFormat percentageFormat = new DecimalFormat("##0.0%");
    
    
    private boolean showResults;
    private boolean showAlignedResults;

    /**
     * Creates a new NISTAlign object.
     */
    public NISTAlign(boolean showResults, boolean showAlignedResults) {
        this.showResults = showResults;
        this.showAlignedResults = showAlignedResults;
        resetTotals();
    }

    /**
     * Sets whether results are displayed
     *
     * @param showResults true if the results should be
     * displayed
     */
    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    /**
     * Sets whether aligned results are displayed
     *
     * @param showAlignedResults true if the aligned results should be
     * displayed
     */
    public void setShowAlignedResults(boolean showAlignedResults) {
        this.showAlignedResults = showAlignedResults;
    }

    /**
     * Reset the total insertions, deletions, and substitutions
     * counts for this class.
     */
     public void resetTotals() {
        totalSentences = 0;
        totalSentencesWithErrors = 0;
        totalSentencesWithSubtitutions = 0;
        totalSentencesWithInsertions = 0;
        totalSentencesWithDeletions = 0;
        totalReferenceWords = 0;
        totalHypothesisWords = 0;
        totalAlignedWords = 0;
        totalWordsCorrect = 0;
        totalSubstitutions = 0;
        totalInsertions = 0;
        totalDeletions = 0;
    }
    
    /**
     * Performs the NIST alignment on the reference and hypothesis
     * strings.  This has the side effect of updating nearly all
     * the fields of this class.
     *
     * @param reference the reference string
     * @param hypothesis the hypothesis string
     *
     * @return true if the reference and hypothesis match
     */
    public boolean align(String reference, String hypothesis) {
        int annotationIndex;

        // Save the original strings for future reference.
        //
        rawReference = reference;
        rawHypothesis = hypothesis;

        // Strip the annotation off the reference string and
        // save it.
        //
        annotationIndex = rawReference.indexOf('(');
        if (annotationIndex != -1) {
            referenceAnnotation = rawReference.substring(annotationIndex);
            referenceWords = toList(rawReference.substring(0,annotationIndex));
        } else {
            referenceAnnotation = null;
            referenceWords = toList(rawReference);
        }

        // Strip the annotation off the hypothesis string.
        // If one wanted to be anal retentive, they might compare
        // the hypothesis annotation to the reference annotation,
        // but I'm not quite that obsessive.
        //
        annotationIndex = rawHypothesis.indexOf('(');
        if (annotationIndex != -1) {
            hypothesisWords = toList(
                rawHypothesis.substring(0, annotationIndex));
        } else {
            hypothesisWords = toList(rawHypothesis);
        }

        // Reset the counts for this sentence.
        //
        substitutions = 0;
        insertions = 0;
        deletions = 0;

        // Turn the list of reference and hypothesis words into two
        // aligned lists of strings.  This has the side effect of
        // creating alignedReferenceWords and alignedHypothesisWords.
        //
	alignWords(backtrace(createBacktraceTable(referenceWords,
                                                  hypothesisWords)));

        // Compute the number of correct words in the hypothesis.
        //
        correct = alignedReferenceWords.size()
            - (insertions + deletions + substitutions);

        // Update the totals that are kept over the lifetime of this
        // class.
        //
        updateTotals();

        return (insertions + deletions + substitutions) == 0;
    }
    
    /**
     * Returns the reference string.  This string will be filtered
     * (all spurious whitespace removed and annotation removed) and
     * set to all lower case.
     *
     * @return the reference string
     */
    public String getReference() {
        return toString(referenceWords);
    }

    /**
     * Returns the hypothesis string.  This string will be filtered
     * (all spurious whitespace removed and annotation removed) and
     * set to all lower case.
     *
     * @return the hypothesis string
     */
    public String getHypothesis() {
        return toString(hypothesisWords);
    }

    /**
     * Returns the aligned reference string. 
     *
     * @return the aligned reference string
     */
    public String getAlignedReference() {
        return toString(alignedReferenceWords);
    }

    /**
     * Returns the aligned hypothesis string.
     *
     * @return the aligned hypothesis string
     */
    public String getAlignedHypothesis() {
        return toString(alignedHypothesisWords);
    }

    /**
     * Gets the total number of word errors for all calls to align.
     *
     * @return the total number of word errors for all calls to align
     */
    public int getTotalWordErrors() {
        return totalSubstitutions + totalInsertions + totalDeletions;
    }
    
    /**
     * Returns the total word accuracy.
     *
     * @return the accuracy between 0.0 and 1.0
     */
    public float getTotalWordAccuracy() {
        if (totalReferenceWords == 0) {
            return 0;
        } else {
            return ((float) totalWordsCorrect) / ((float) totalReferenceWords);
        }
    }

    /**
     * Returns the total word accuracy.
     *
     * @return the accuracy between 0.0 and 1.0
     */
    public float getTotalWordErrorRate() {
        if (totalReferenceWords == 0) {
            return 0;
        } else {
            return ((float) getTotalWordErrors())
                / ((float) totalReferenceWords);
        }
    }

    /**
     * Returns the total sentence accuracy.
     *
     * @return the accuracy between 0.0 and 1.0
     */
    public float getTotalSentenceAccuracy() {
        int totalSentencesCorrect = totalSentences - totalSentencesWithErrors;
        if (totalSentences == 0) {
            return 0;
        } else {
            return ((float) totalSentencesCorrect / (float) totalSentences);
        }
    }

    /**
     * Gets the total number of words
     *
     * @return the total number of words
     */
    public int getTotalWords() {
        return totalReferenceWords;
    }

    /**
     * Gets the total number of substitution errors
     *
     * @return the total number of substitutions
     */
    public int getTotalSubstitutions() {
        return totalSubstitutions;
    }

    /**
     * Gets the total number of insertion errors
     *
     * @return the total number of insertion errors
     */
    public int getTotalInsertions() {
        return totalInsertions;
    }


    /**
     * Gets the total number of deletions
     *
     * @return the total number of deletions
     */
    public int getTotalDeletions() {
        return totalDeletions;
    }

    /**
     * Gets the total number of sentences
     *
     * @return the total number of sentences
     */
    public int getTotalSentences() {
        return totalSentences;
    }

    /**
     * Gets the total number of sentences with errors
     *
     * @return the total number of sentences with errors
     */
    public int getTotalSentencesWithErrors() {
        return totalSentencesWithDeletions;
    }

    /**
     * Prints the results for this sentence to System.out.  If you
     * want the output to match the NIST output, see
     * printNISTSentenceSummary.
     *
     * @see #printNISTSentenceSummary
     */
    public void printSentenceSummary() {
        if (showResults) {
            System.out.println("REF:       " + toString(referenceWords));
            System.out.println("HYP:       " + toString(hypothesisWords));
        }
        
        if (showAlignedResults) {
            System.out.println("ALIGN_REF: " + toString(alignedReferenceWords));
            System.out.println("ALIGN_HYP: " + toString(alignedHypothesisWords));
        }
    }

    /**
     * Prints the total summary for all calls.  If you want the output
     * to match the NIST output, see printNISTTotalSummary.
     *
     * @see #printNISTTotalSummary
     */
    public void printTotalSummary() {
        if (totalSentences > 0) {
            float wordErrorRate = getTotalWordErrorRate();

	    System.out.print(
                "   Accuracy: " + toPercentage("##0.000%",
                                               getTotalWordAccuracy()));
	    System.out.println(
                "    Errors: " + getTotalWordErrors() 
                + "  (Sub: " + totalSubstitutions
                + "  Ins: " + totalInsertions
                + "  Del: " + totalDeletions + ")");
            System.out.println(
                "   Words: " + totalReferenceWords 
                + "   Matches: " + totalWordsCorrect
                + "    WER: " + toPercentage("##0.000%",
                                             getTotalWordErrorRate()));
	    System.out.println(
                "   Sentences: " + totalSentences
                + "   Matches: " + (totalSentences - totalSentencesWithErrors)
                + "   SentenceAcc: " + toPercentage("##0.000%",
                                                    getTotalSentenceAccuracy()));
	}
    }
    
    /**
     * Prints the results for this sentence to System.out.  This
     * matches the output from the NIST aligner.
     */
    public void printNISTSentenceSummary() {
        int sentenceErrors = substitutions + insertions + deletions;
        
        System.out.println();
        
        System.out.print("REF: " + toString(alignedReferenceWords));
        if (referenceAnnotation != null) {
            System.out.print(" " + referenceAnnotation);
        }        
        System.out.println();
        
        System.out.print("HYP: " + toString(alignedReferenceWords));
        if (referenceAnnotation != null) {
            System.out.print(" " + referenceAnnotation);
        }
        System.out.println();

        System.out.println();

        if (referenceAnnotation != null) {
            System.out.println("SENTENCE " + totalSentences
                               + "  " + referenceAnnotation);
        } else {
            System.out.println("SENTENCE " + totalSentences);
        }
        
        System.out.println("Correct          = "
                           + toPercentage("##0.0%",
                                          correct,
                                          referenceWords.size())
                           + padLeft(5, correct)
                           + "   ("
                           + padLeft(6, totalWordsCorrect)
                           + ")");
        System.out.println("Errors           = "
                           + toPercentage("##0.0%",
                                          sentenceErrors,
                                          referenceWords.size())
                           + padLeft(5, sentenceErrors)
                           + "   ("
                           + padLeft(6, totalSentencesWithErrors)
                           + ")");
        
        System.out.println();
        System.out.println(HRULE);
    }

    
    /**
     * Prints the summary for all calls to align to System.out.  This
     * matches the output from the NIST aligner.
     */
    public void printNISTTotalSummary() {
        int totalSentencesCorrect = totalSentences - totalSentencesWithErrors;
        
        System.out.println();
        System.out.println("---------- SUMMARY ----------");
        System.out.println();
        System.out.println("SENTENCE RECOGNITION PERFORMANCE:");
        System.out.println(
            "sentences                          " + totalSentences);
        System.out.println(
            "  correct                  "
            + toPercentage("##0.0%", totalSentencesCorrect, totalSentences)
            + " (" + padLeft(4, totalSentencesCorrect) + ")");
        System.out.println(
            "  with error(s)            "
            + toPercentage("##0.0%", totalSentencesWithErrors, totalSentences)
            + " (" + padLeft(4, totalSentencesWithErrors) + ")");
        System.out.println(
            "    with substitutions(s)  "
            + toPercentage("##0.0%", totalSentencesWithSubtitutions, totalSentences)
            + " (" + padLeft(4, totalSentencesWithSubtitutions) + ")");
        System.out.println(
            "    with insertion(s)      "
            + toPercentage("##0.0%", totalSentencesWithInsertions, totalSentences)
            + " (" + padLeft(4, totalSentencesWithInsertions) + ")");
        System.out.println(
            "    with deletions(s)      "
            + toPercentage("##0.0%", totalSentencesWithDeletions, totalSentences)
            + " (" + padLeft(4, totalSentencesWithDeletions) + ")");

        System.out.println();
        System.out.println();
        System.out.println();

        System.out.println("WORD RECOGNITION PERFORMANCE:");
        System.out.println(
            "Correct           = "
            + toPercentage("##0.0%", totalWordsCorrect, totalReferenceWords)
            + " (" + padLeft(6, totalWordsCorrect) + ")");
        System.out.println(
            "Substitutions     = "
            + toPercentage("##0.0%", totalSubstitutions, totalReferenceWords)
            + " (" + padLeft(6, totalSubstitutions) + ")");
        System.out.println(
            "Deletions         = "
            + toPercentage("##0.0%", totalDeletions, totalReferenceWords)
            + " (" + padLeft(6, totalDeletions) + ")");
        System.out.println(
            "Insertions        = "
            + toPercentage("##0.0%", totalInsertions, totalReferenceWords)
            + " (" + padLeft(6, totalInsertions) + ")");
        System.out.println(
            "Errors            = "
            + toPercentage("##0.0%", getTotalWordErrors(), totalReferenceWords)
            + " (" + padLeft(6, getTotalWordErrors()) + ")");

        System.out.println();
        
        System.out.println(
            "Ref. words           = " + padLeft(6, totalReferenceWords));
        System.out.println(
            "Hyp. words           = " + padLeft(6, totalHypothesisWords));
        System.out.println(
            "Aligned words        = " + padLeft(6, totalAlignedWords));

        System.out.println();
        System.out.println(
            "WORD ACCURACY=  "
            + toPercentage("##0.000%", totalWordsCorrect, totalReferenceWords)
            + " ("
            + padLeft(5, totalWordsCorrect)
            + "/"
            + padLeft(5, totalReferenceWords)
            + ")  ERRORS= "
            + toPercentage("##0.000%",
                           getTotalWordErrors(),
                           totalReferenceWords)
            + " ("
            + padLeft(5, getTotalWordErrors())
            + "/"
            + padLeft(5, totalReferenceWords)
            + ")");            
                           
        System.out.println();
    }

    /**   
     * Creates the backtrace table.  This is magic.  The basic idea is
     * that the penalty table contains a set of penalty values based
     * on some strategically selected numbers.  I'm not quite sure
     * what they are, but they help determine the backtrace table
     * values.  The backtrace table contains information used to help
     * determine if words matched (OK), were inserted (INSERTION),
     * substituted (SUBSTITUTION), or deleted (DELETION).
     *
     * @param referenceWords the ordered list of reference words
     * @param hypothesisWords the ordered list of hypothesis words
     *
     * @return the backtrace table
     */
    int[][] createBacktraceTable(LinkedList referenceWords,
                               LinkedList hypothesisWords) {
        int[][] penaltyTable;
        int[][] backtraceTable;
        int penalty;
        int minPenalty;

	penaltyTable = 
	    new int[referenceWords.size() + 1][hypothesisWords.size() + 1];

	backtraceTable = 
	    new int[referenceWords.size() + 1][hypothesisWords.size() + 1];

	// Initialize the penaltyTable and the backtraceTable.  The
	// rows of each table represent the words in the reference
	// string.  The columns of each table represent the words in
	// the hypothesis string.
        //
	penaltyTable[0][0] = 0;
	backtraceTable[0][0] = OK;

        // The lower left of the tables represent deletions.  If you
	// think about this, a shorter hypothesis string will have
        // deleted words from the reference string.
        //
	for (int i = 1; i <= referenceWords.size(); i++) {
	    penaltyTable[i][0] = DELETION_PENALTY * i;
	    backtraceTable[i][0] = DELETION;
	}

        // The upper right of the tables represent insertions.  If
        // you think about this, a longer hypothesis string will have
        // inserted words.
        //
	for (int j = 1; j <= hypothesisWords.size(); j++) {
	    penaltyTable[0][j] = INSERTION_PENALTY * j;
	    backtraceTable[0][j] = INSERTION;
        }

        // Row-by-row, column-by-column, fill out the tables.
        // The goal is to keep the penalty for each cell to a
        // minimum.
        //
	for (int i = 1; i <= referenceWords.size(); i++) {
	    for (int j = 1; j <= hypothesisWords.size(); j++) {
	        minPenalty = MAX_PENALTY;

                // First assume that this represents a deletion.
                //
		penalty = penaltyTable[i-1][j] + DELETION_PENALTY;
		if (penalty < minPenalty) {
		    minPenalty = penalty;
		    penaltyTable[i][j] = penalty;
		    backtraceTable[i][j] = DELETION;
		}

                // If the words match, we'll assume it's OK.
                // Otherwise, we assume we have a substitution.
                //
		if (referenceWords.get(i-1).equals(hypothesisWords.get(j-1))) {
		    penalty = penaltyTable[i-1][j-1];
		    if (penalty < minPenalty) {
		        minPenalty = penalty;
		        penaltyTable[i][j] = penalty;
		        backtraceTable[i][j] = OK;
		    }
		} else {
		    penalty = penaltyTable[i-1][j-1] + SUBSTITUTION_PENALTY;
		    if (penalty < minPenalty) {
		        minPenalty = penalty;
		        penaltyTable[i][j] = penalty;
		        backtraceTable[i][j] = SUBSTITUTION;
		    }
		}

                // If you've made it this far, it should be obvious I
                // have no idea what the heck this code is doing.  I'm
                // just doing a transliteration.
                //
		penalty = penaltyTable[i][j-1] + DELETION_PENALTY;                                                                    
		if (penalty < minPenalty) {
		    minPenalty = penalty;
		    penaltyTable[i][j] = penalty;
		    backtraceTable[i][j] = INSERTION;
		}
	    }
	}
        return backtraceTable;
    }

    /**
     * Backtraces through the penalty table.  This starts at the
     * "lower right" corner (i.e., the last word of the longer of
     * the reference vs. hypothesis strings) and works its way
     * backwards.
     *
     * @param backtraceTable created from call to createBacktraceTable
     *
     * @return a linked list of Integers representing the backtrace
     */
    LinkedList backtrace(int[][] backtraceTable) {
        LinkedList list = new LinkedList();
	int i = referenceWords.size();
	int j = hypothesisWords.size();
	while ((i >= 0) && (j >= 0)) {
	    list.add(new Integer(backtraceTable[i][j]));
	    switch (backtraceTable[i][j]) {
	        case OK           : i--; j--; 	                break;
		case SUBSTITUTION : i--; j--; substitutions++;  break;
		case INSERTION    :      j--; insertions++;     break;
		case DELETION     : i--;      deletions++;      break;
	    }
	}
	return list;
    }

    /**
     * Based on the backtrace information, words are aligned as
     * appropriate with insertions and deletions causing asterisks
     * to be placed in the word lists.  This generates the
     * alignedReferenceWords and alignedHypothesisWords lists.
     *
     * @param backtrace the backtrace list created in backtrace
     */
    void alignWords(LinkedList backtrace) {
        ListIterator referenceWordsIterator = referenceWords.listIterator();
        ListIterator hypothesisWordsIterator = hypothesisWords.listIterator();
        String referenceWord;
        String hypothesisWord;
        
        alignedReferenceWords = new LinkedList();
        alignedHypothesisWords = new LinkedList();

        for (int m = backtrace.size() - 2; m >= 0; m--) {
            int backtraceEntry = ((Integer) backtrace.get(m)).intValue();
            if (backtraceEntry != INSERTION) {
                referenceWord = (String) referenceWordsIterator.next();
            } else {
                referenceWord = null;
            }
            if (backtraceEntry != DELETION) {
                hypothesisWord = (String) hypothesisWordsIterator.next();
            } else {
                hypothesisWord = null;
            }
            switch (backtraceEntry) {
                case SUBSTITUTION: {
                    referenceWord = referenceWord.toUpperCase();
                    hypothesisWord = hypothesisWord.toUpperCase();
                    break;
                }
                case INSERTION: {
                    hypothesisWord = hypothesisWord.toUpperCase();
                    break;
                }
                case DELETION: {
                    referenceWord = referenceWord.toUpperCase();
                    break;
                }
                case OK:
                    break;
            }

            // Expand the missing words out to be all *'s.
            //
            if (referenceWord == null) {
                referenceWord = STARS.substring(0, hypothesisWord.length());
            }
            if (hypothesisWord == null) {
                hypothesisWord = STARS.substring(0, referenceWord.length());
            }

            // Fill the words up with spaces so they are the same
            // length.
            //
            if (referenceWord.length() > hypothesisWord.length()) {
                hypothesisWord = hypothesisWord.concat(
                    SPACES.substring(0,
                                     referenceWord.length()
                                     - hypothesisWord.length()));
            } else if (referenceWord.length() < hypothesisWord.length()) {
                referenceWord = referenceWord.concat(
                    SPACES.substring(0,
                                     hypothesisWord.length()
                                     - referenceWord.length()));
            }   

            alignedReferenceWords.add(referenceWord);
            alignedHypothesisWords.add(hypothesisWord);
        }
    }

    /**
     * Updates the total counts based on the current alignment.
     */
    void updateTotals() {
        totalSentences++;
        if ((substitutions + insertions + deletions) != 0) {
            totalSentencesWithErrors++;
        }
        if (substitutions != 0) {
            totalSentencesWithSubtitutions++;
        }
        if (insertions != 0) {
            totalSentencesWithInsertions++;
        }
        if (deletions != 0) {
            totalSentencesWithDeletions++;
        }
        totalReferenceWords += referenceWords.size();
        totalHypothesisWords += hypothesisWords.size();
        totalAlignedWords += alignedReferenceWords.size();
        
        totalWordsCorrect += correct;
        totalSubstitutions += substitutions;
        totalInsertions += insertions;
        totalDeletions += deletions;
    }

    /**
     * Turns the numerator/denominator into a percentage.
     *
     * @param pattern percentage pattern (ala DecimalFormat)
     * @param numerator the numerator
     * @param denominator the denominator
     *
     * @return a String that represents the percentage value.
     */
    String toPercentage(String pattern, int numerator, int denominator) {
        percentageFormat.applyPattern(pattern);
        return padLeft(
            6,
            percentageFormat.format((double) numerator
                                    / (double) denominator));
    }

    /**
     * Turns the float into a percentage.
     *
     * @param pattern percentage pattern (ala DecimalFormat)
     * @param value the floating point value
     *
     * @return a String that represents the percentage value.
     */
    String toPercentage(String pattern, float value) {
        percentageFormat.applyPattern(pattern);
        return percentageFormat.format(value);
    }

    /**
     * Turns the integer into a left-padded string.
     *
     * @param width the total width of String, including spaces
     * @param i the integer
     *
     * @return a String padded left with spaces
     */
    String padLeft(int width, int i) {
        return padLeft(width, Integer.toString(i));
    }
    
    /**
     * Pads a string to the left with spaces (i.e., prepends spaces to
     * the string so it fills out the given width).
     *
     * @param width the total width of String, including spaces
     * @param string the String to pad
     *
     * @return a String padded left with spaces
     */
    String padLeft(int width, String string) {
        int len = string.length();
        if (len < width) {
            return SPACES.substring(0,width-len).concat(string);
        } else {
            return string;
        }
    }
    
    /**
     * Converts the given String or words to a LinkedList.  
     *
     * @param s the String of words to parse to a LinkedList
     *
     * @return a list, one word per item
     */
    LinkedList toList(String s) {
        LinkedList list = new LinkedList();
        StringTokenizer st = new StringTokenizer(s.trim());
        while (st.hasMoreTokens()) {
            String token = st.nextToken().toLowerCase();
            list.add(token);
        }
        return list;
    }
    
    /**
     * convert the list of words back to a space separated string
     *
     * @param list the list of words
     * @return a space separated string
     */
    private String toString(LinkedList list) {
        if (list != null) {
            StringBuffer sb = new StringBuffer();
            ListIterator iterator = list.listIterator();
            while (iterator.hasNext()) {
                sb.append(iterator.next());
                if (iterator.hasNext()) {
                    sb.append(" ");
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Take two filenames -- the first contains a list of reference
     * sentences, the second contains a list of hypothesis sentences.
     * Aligns each pair of sentences and outputs the individual and
     * total results.
     */
    public static void main(String args[]) {
        NISTAlign align = new NISTAlign(true, true);
        
        BufferedReader referenceFile;
        BufferedReader hypothesisFile;
        String reference;
        String hypothesis;
        
        try {
            referenceFile  = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[0])));
            hypothesisFile = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[1])));
            try {
                while (true) {
                    reference = referenceFile.readLine();
                    hypothesis = hypothesisFile.readLine();
                    if ((reference == null) || (hypothesis == null)) {
                        break;
                    } else {
                        align.align(reference, hypothesis);
                        align.printNISTSentenceSummary();
                    }
                }
            } catch (java.io.IOException e) {
            }
            align.printNISTTotalSummary();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.out.println();
            System.out.println("Usage: align <reference file> <hypothesis file>");
            System.out.println();
        }
    }
}
