/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 *
 * Created on Aug 11, 2004
 */

package edu.cmu.sphinx.result;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import edu.cmu.sphinx.util.LogMath;

/**
 * @author pgorniak
 *
 * A Sausage is a sequence of confusion sets, one for each position in an utterance. 
 * A confusion set is a set of words with their associated posteriors.
 */

public class Sausage {
    protected List confusionSets;
    
    /**
     * Construct a new sausage.
     * 
     * @param size The number of word slots in the sausage
     */
    public Sausage(int size) {
        confusionSets = new Vector(size);
        for (int i=0;i<size;i++) {
            confusionSets.add(new TreeMap());
        }
    }
    
    /**
     * Adds skip elements for each word slot in which the word posteriors do not
     * add up to linear 1.
     * 
     * @param logMath the log math object to use for probability computations
     */
    public void fillInBlanks(LogMath logMath) {
        for (ListIterator i=confusionSets.listIterator();i.hasNext();) {
            int index = i.nextIndex();
            SortedMap set = (SortedMap)i.next();
            float sum = LogMath.getLogZero();
            for (Iterator j=set.keySet().iterator();j.hasNext();) {
                sum = logMath.addAsLinear(sum,((Double)j.next()).floatValue());
            }
            if (sum < LogMath.getLogOne() - 10) {
                float remainder = logMath.subtractAsLinear(LogMath.getLogOne(),sum);
                addWordHypothesis(index,"<noop>",remainder);
            } else {
                SortedMap newSet = new TreeMap();
                for (Iterator j=set.keySet().iterator();j.hasNext();) {
                    Double oldProb = (Double)j.next();
                    Double newProb = new Double(oldProb.doubleValue() - sum);
                    newSet.put(newProb,set.get(oldProb));
                }
                confusionSets.set(index,newSet);
            }
        }
    }
    
    /**
     * Add a word hypothesis to a given word slot in the sausage.
     * 
     * @param position the position to add a hypothesis to
     * @param word the word to add
     * @param prob the posterior to attach to the word
     */
    public void addWordHypothesis(int position, String word, double prob) {
        //System.out.println("adding " + word + " " + prob + " at " + position);
        Double key = new Double(prob);
        Set wordSet = (Set)((Map)confusionSets.get(position)).get(key);
        if (wordSet == null) {
            wordSet = new HashSet();
            ((Map)confusionSets.get(position)).put(key,wordSet);
        }
        wordSet.add(word);
    }
    
    /**
     * Get a string representing the best path through the sausage.
     * 
     * @return best string
     */
    public String getBestHypothesisString() {
        String s = "";
        Iterator i = confusionSets.iterator();
        while (i.hasNext()) {
            SortedMap set = (SortedMap)i.next();             
            Set wordSet = (Set)set.get(set.lastKey());
            Iterator j = wordSet.iterator();
            while (j.hasNext()) {
                s += j.next();
                if (j.hasNext()) {
                    s += "/";
                }
            }
            if (i.hasNext()) {
                s += " ";
            }
        }
        return s;
    }
    
    /**
     * Get the word hypothesis with the highest posterior for a word slot
     * 
     * @param pos the word slot to look at
     * @return the word with the highest posterior in the slot
     */
    public Set getBestWordHypothesis(int pos) {
        SortedMap set = (SortedMap)confusionSets.get(pos);
        return (Set)set.get(set.lastKey());
    }

    /**
     * Get the the highest posterior for a word slot
     * 
     * @param pos the word slot to look at
     * @return the highest posterior in the slot
     */

    public double getBestWordHypothesisPosterior(int pos) {
        SortedMap set = (SortedMap)confusionSets.get(pos);
        return ((Double)set.lastKey()).doubleValue();        
    }
    
    /**
     * Get the confusion set stored in a given word slot.
     * 
     * @param pos the word slot to look at.
     * @return a map from Double posteriors to Sets of String words,
     *         sorted from lowest to highest.
     */
    public SortedMap getConfusionSet(int pos) {
        return (SortedMap)confusionSets.get(pos);
    }
    
    /**
     * size of this sausage in word slots.
     * 
     * @return The number of word slots in this sausage
     */
    public int size() {
        return confusionSets.size();
    }
    
    /**
     * Write this sausage to an aisee format text file.
     * @param fileName The file to write to.
     * @param title the title to give the graph.
     */
    public void dumpAISee(String fileName, String title) {
        try {
            System.err.println("Dumping " + title + " to " + fileName);
            FileWriter f = new FileWriter(fileName);
            f.write("graph: {\n");
            f.write("title: \"" + title + "\"\n");
            f.write("display_edge_labels: yes\n");
            f.write( "orientation: left_to_right\n");
            /*
            f.write( "colorentry 32: 25 225 0\n");
            f.write( "colorentry 33: 50 200 0\n");
            f.write( "colorentry 34: 75 175 0\n");
            f.write( "colorentry 35: 100 150 0\n");
            f.write( "colorentry 36: 125 125 0\n");
            f.write( "colorentry 37: 150 100 0\n");
            f.write( "colorentry 38: 175 75 0\n");
            f.write( "colorentry 39: 200 50 0\n");
            f.write( "colorentry 40: 225 25 0\n");
            f.write( "colorentry 41: 250 0 0\n");
            f.write( "color: black\n");
            f.write( "xspace: 10\n");
            f.write( "yspace: 10\n");
            */
            ListIterator i = confusionSets.listIterator();
            while (i.hasNext()) {
                int index = i.nextIndex();
                SortedMap set = (SortedMap)i.next();
                Iterator j = set.keySet().iterator();
                f.write("node: { title: \"" + index + "\" label: \"" + index + "\"}\n");
                while (j.hasNext()) {
                    Double prob = (Double)j.next();
                    String word = "";
                    Set wordSet = (Set)set.get(prob);
                    for (Iterator w = wordSet.iterator();w.hasNext();) {
                        word += w.next();
                        if (w.hasNext()) {
                            word += "/";
                        }
                    }
                    f.write("edge: { sourcename: \"" + index
                            + "\" targetname: \"" + (index + 1)
                            + "\" label: \"" + word + ":" + prob + "\" }\n");
                }
            }
            f.write("node: { title: \"" + size() + "\" label: \"" + size() + "\"}\n");            
            f.write("}\n");
            f.close();
        } catch (IOException e) {
            throw new Error(e.toString());
        }
    }
}
