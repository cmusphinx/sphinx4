package edu.cmu.sphinx.decoder.search.test;

import junit.framework.Assert;

import org.junit.Test;

import edu.cmu.sphinx.decoder.search.Partitioner;
import edu.cmu.sphinx.decoder.search.Token;

public class PartitionerTest {
    
    public void testSorted (Token[] tokens, int p) {
        for (int i = 0; i < p; i++) {
            Assert.assertTrue(tokens[i].getScore() >= tokens[p].getScore());
        }
        for (int i = p; i < tokens.length; i++) {
            Assert.assertTrue(tokens[i].getScore() <= tokens[p].getScore());
        }        
    }
    @Test
    public void testPartition() {
        int p;
        Token[] tokens = new Token[100000];
        Partitioner partitioner = new Partitioner();

        for (int i = 0; i < 100000; i++) 
            tokens[i] = new Token(null, null, 1 - i, 0, 0, 0);
        p = partitioner.partition(tokens, 100000, 3000);
        Assert.assertEquals(p, 2999);
        testSorted (tokens, p);

        for (int i = 0; i < 100000; i++) 
            tokens[i] = new Token(null, null, i, 0, 0, 0);
        p = partitioner.partition(tokens, 100000, 3000);
        Assert.assertEquals(p, 2999);
        testSorted (tokens, p);

        for (int i = 0; i < 100000; i++) 
            tokens[i] = new Token(null, null, 0, 0, 0, 0);
        p = partitioner.partition(tokens, 100000, 3000);
        Assert.assertEquals(p, 2999);
        testSorted (tokens, p);

        for (int i = 0; i < 100000; i++) 
            tokens[i] = new Token(null, null, (float)Math.random(), 0, 0, 0);
        p = partitioner.partition(tokens, 100000, 3000);
        Assert.assertEquals(p, 2999);
        testSorted (tokens, p);
    }
}

