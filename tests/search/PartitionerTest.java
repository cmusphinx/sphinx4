
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package tests.search;

import edu.cmu.sphinx.decoder.search.Partitioner;
import edu.cmu.sphinx.decoder.search.Token;

import java.util.Random;


public class PartitionerTest {

    public static void main(String[] argv) {

        int absoluteBeamWidth = 1500;
        int tokenListSize = 3000;

        Random random = new Random(System.currentTimeMillis());
        Partitioner partitioner = new Partitioner();

        Token parent = new Token(null, 0);

        // Test 1 : (tokenListSize > absoluteBeamWidth)
        Token[] tokens = new Token[tokenListSize];

        // create the tokens first
        for (int i = 0; i < tokens.length; i++) {
            float logTotalScore = random.nextFloat();
            tokens[i] = parent.child(null, logTotalScore, 0.0f, 0.0f, i);
        }

        int r = partitioner.partition(tokens, absoluteBeamWidth);
                
        assert (r == (absoluteBeamWidth - 1));

        float lowestScore = tokens[r].getScore();

        for (int i = 0; i <= r; i++) {
            assert (tokens[i].getScore() >= lowestScore);
        }

        System.out.println
            ("Test 1 : (tokenListSize > absoluteBeamWidth) PASSED");

        // Test 2 : (tokenListSize == absoluteBeamWidth)

        tokenListSize = absoluteBeamWidth;
        tokens = new Token[tokenListSize];

        // create the tokens first
        for (int i = 0; i < tokens.length; i++) {
            float logTotalScore = random.nextFloat();
            tokens[i] = parent.child(null, logTotalScore, 0.0f, 0.0f, i);
        }

        r = partitioner.partition(tokens, absoluteBeamWidth);
                
        assert (r == (tokenListSize - 1));

        lowestScore = tokens[r].getScore();

        for (int i = 0; i <= r; i++) {
            assert (tokens[i].getScore() >= lowestScore);
        }

        System.out.println
            ("Test 2 : (tokenListSize == absoluteBeamWidth) PASSED");

        // Test 3 : (tokenListSize < absoluteBeamWidth)

        tokenListSize = 1000;
        tokens = new Token[tokenListSize];

        // create the tokens first
        for (int i = 0; i < tokens.length; i++) {
            float logTotalScore = random.nextFloat();
            tokens[i] = parent.child(null, logTotalScore, 0.0f, 0.0f, i);
        }

        r = partitioner.partition(tokens, absoluteBeamWidth);
                
        assert (r == (tokenListSize - 1));

        lowestScore = tokens[r].getScore();

        for (int i = 0; i <= r; i++) {
            assert (tokens[i].getScore() >= lowestScore);
        }

        System.out.println
            ("Test 3 : (tokenListSize < absoluteBeamWidth) PASSED");

        // Test 4 : (tokenListSize == 0)
        
        tokenListSize = 0;
        tokens = new Token[tokenListSize];

        // create the tokens first
        for (int i = 0; i < tokens.length; i++) {
            float logTotalScore = random.nextFloat();
            tokens[i] = parent.child(null, logTotalScore, 0.0f, 0.0f, i);
        }

        r = partitioner.partition(tokens, absoluteBeamWidth);
                
        assert (r == (tokenListSize - 1));

        System.out.println("Test 4 : (tokenListSize == 0) PASSED");

        System.out.println("Partitioner works correctly.");
    }
}
