
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

import java.util.*;


public class PartitionerTest {

    public static void main(String[] argv) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            PartitionerTest test = new PartitionerTest();
        }
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
    }


    /**
     * A single test run.
     */
    private void test(int absoluteBeamWidth, int tokenListSize, 
                      boolean tokenListLarger) {

        Random random = new Random(System.currentTimeMillis());
        Partitioner partitioner = new Partitioner();

        Token parent = new Token(null, 0);
        Token[] tokens = new Token[tokenListSize];

        // create the tokens first
        for (int i = 0; i < tokens.length; i++) {
            float logTotalScore = random.nextFloat();
            tokens[i] = parent.child(null, logTotalScore, 0.0f, 0.0f, i);
        }

        final int r = partitioner.partition(tokens, absoluteBeamWidth);
        
        if (tokenListLarger) {                
            assert (r == (absoluteBeamWidth - 1));
        } else {
            assert (r == (tokenListSize - 1));
        }

        List firstList = new LinkedList();
        if (r >= 0) {
            float lowestScore = tokens[r].getScore();
            
            for (int i = 0; i <= r; i++) {
                assert (tokens[i].getScore() >= lowestScore);
                firstList.add(tokens[i]);
            }
            for (int i = r + 1; i < tokens.length; i++) {
                assert (lowestScore > tokens[i].getScore());
            }
        
            Collections.sort(firstList, Token.COMPARATOR);
            
            List secondList = Arrays.asList(tokens);
            Collections.sort(secondList, Token.COMPARATOR);
            
            for (Iterator i1 = firstList.iterator(), 
                     i2 = secondList.iterator();
                 i1.hasNext() && i2.hasNext(); ) {
                Token t1 = (Token) i1.next();
                Token t2 = (Token) i2.next();
                assert (t1 == t2);
            }
        }
    }


    public PartitionerTest() {

        int absoluteBeamWidth = 1500;
        int tokenListSize = 3000;

        // Test 1 : (tokenListSize > absoluteBeamWidth)
        test(absoluteBeamWidth, tokenListSize, true);
        System.out.println
            ("Test 1 : (tokenListSize > absoluteBeamWidth) PASSED");

        // Test 2 : (tokenListSize == absoluteBeamWidth)
        tokenListSize = absoluteBeamWidth;
        test(absoluteBeamWidth, tokenListSize, false);
        System.out.println
            ("Test 2 : (tokenListSize == absoluteBeamWidth) PASSED");

        // Test 3 : (tokenListSize < absoluteBeamWidth)
        tokenListSize = 1000;
        test(absoluteBeamWidth, tokenListSize, false);
        System.out.println
            ("Test 3 : (tokenListSize < absoluteBeamWidth) PASSED");

        // Test 4 : (tokenListSize == 0)
        tokenListSize = 0;
        test(absoluteBeamWidth, tokenListSize, false);
        System.out.println("Test 4 : (tokenListSize == 0) PASSED");

        System.out.println("Partitioner works correctly.");
    }
}
