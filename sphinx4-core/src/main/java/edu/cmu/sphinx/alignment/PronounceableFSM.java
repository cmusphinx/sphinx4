/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * Implements a finite state machine that checks if a given string is
 * pronounceable. If it is pronounceable, the method <code>accept()</code> will
 * return true.
 */
public class PronounceableFSM {

    private static final String VOCAB_SIZE = "VOCAB_SIZE";
    private static final String NUM_OF_TRANSITIONS = "NUM_OF_TRANSITIONS";
    private static final String TRANSITIONS = "TRANSITIONS";

    /**
     * The vocabulary size.
     */
    protected int vocabularySize;

    /**
     * The transitions of this FSM
     */
    protected int[] transitions;

    /**
     * Whether we should scan the input string from the front.
     */
    protected boolean scanFromFront;

    /**
     * Constructs a PronounceableFSM with information in the given URL.
     * 
     * @param url the URL that contains the FSM specification
     * @param scanFromFront indicates whether this FSM should scan the input
     *        string from the front, or from the back
     */
    public PronounceableFSM(URL url, boolean scanFromFront) throws IOException {
        this.scanFromFront = scanFromFront;
        InputStream is = url.openStream();
        loadText(is);
        is.close();
    }

    /**
     * Constructs a PronounceableFSM with the given attributes.
     * 
     * @param vocabularySize the vocabulary size of the FSM
     * @param transitions the transitions of the FSM
     * @param scanFromFront indicates whether this FSM should scan the input
     *        string from the front, or from the back
     */
    public PronounceableFSM(int vocabularySize, int[] transitions,
            boolean scanFromFront) {
        this.vocabularySize = vocabularySize;
        this.transitions = transitions;
        this.scanFromFront = scanFromFront;
    }

    /**
     * Loads the ASCII specification of this FSM from the given InputStream.
     * 
     * @param is the input stream to load from
     * 
     * @throws IOException if an error occurs on input.
     */
    private void loadText(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("***")) {
                if (line.startsWith(VOCAB_SIZE)) {
                    vocabularySize = parseLastInt(line);
                } else if (line.startsWith(NUM_OF_TRANSITIONS)) {
                    int transitionsSize = parseLastInt(line);
                    transitions = new int[transitionsSize];
                } else if (line.startsWith(TRANSITIONS)) {
                    StringTokenizer st = new StringTokenizer(line);
                    String transition = st.nextToken();
                    int i = 0;
                    while (st.hasMoreTokens() && i < transitions.length) {
                        transition = st.nextToken().trim();
                        transitions[i++] = Integer.parseInt(transition);
                    }
                }
            }
        }
        reader.close();
    }

    /**
     * Returns the integer value of the last integer in the given string.
     * 
     * @param line the line to parse the integer from
     * 
     * @return an integer
     */
    private int parseLastInt(String line) {
        String lastInt = line.trim().substring(line.lastIndexOf(" "));
        return Integer.parseInt(lastInt.trim());
    }

    /**
     * Causes this FSM to transition to the next state given the current state
     * and input symbol.
     * 
     * @param state the current state
     * @param symbol the input symbol
     */
    private int transition(int state, int symbol) {
        for (int i = state; i < transitions.length; i++) {
            if ((transitions[i] % vocabularySize) == symbol) {
                return (transitions[i] / vocabularySize);
            }
        }
        return -1;
    }

    /**
     * Checks to see if this finite state machine accepts the given input
     * string.
     * 
     * @param inputString the input string to be tested
     * 
     * @return true if this FSM accepts, false if it rejects
     */
    public boolean accept(String inputString) {
        int symbol;
        int state = transition(0, '#');
        int leftEnd = inputString.length() - 1;
        int start = (scanFromFront) ? 0 : leftEnd;

        for (int i = start; 0 <= i && i <= leftEnd;) {
            char c = inputString.charAt(i);
            if (c == 'n' || c == 'm') {
                symbol = 'N';
            } else if ("aeiouy".indexOf(c) != -1) {
                symbol = 'V';
            } else {
                symbol = c;
            }
            state = transition(state, symbol);
            if (state == -1) {
                return false;
            } else if (symbol == 'V') {
                return true;
            }
            if (scanFromFront) {
                i++;
            } else {
                i--;
            }
        }
        return false;
    }
}
