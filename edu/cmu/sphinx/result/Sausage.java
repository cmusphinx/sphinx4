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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import edu.cmu.sphinx.util.LogMath;

/**
 * A Sausage is a sequence of confusion sets, one for each position in an utterance. 
 * 
 * @author pgorniak
 */

public class Sausage implements ConfidenceResult {
    protected List confusionSets;
    private LogMath logMath;
    
    /**
     * Construct a new sausage.
     * 
     * @param size The number of word slots in the sausage
     */
    public Sausage(int size, LogMath logMath) {
        confusionSets = new Vector(size);
        for (int i=0;i<size;i++) {
            confusionSets.add(new ConfusionSet());
        }
        this.logMath = logMath;
    }
    
    /**
     * Get an iterator for the sausage. The iterator will return
     * SortedMaps, which are confusion sets mapping Double posteriors
     * to Sets of word Strings.
     * 
     * @return an iterator that steps through confusion sets
     */
    public Iterator confusionSetIterator() {
        return confusionSets.iterator();
    }
    
    /**
     * Adds skip elements for each word slot in which the word posteriors do not
     * add up to linear 1.
     * 
     * @param logMath the log math object to use for probability computations
     */
    public void fillInBlanks() {
        for (ListIterator i=confusionSets.listIterator();i.hasNext();) {
            int index = i.nextIndex();
            ConfusionSet set = (ConfusionSet)i.next();
            float sum = logMath.getLogZero();
            for (Iterator j=set.keySet().iterator();j.hasNext();) {
                sum = logMath.addAsLinear(sum,((Double)j.next()).floatValue());
            }
            if (sum < logMath.getLogOne() - 10) {
                float remainder = logMath.subtractAsLinear(LogMath.getLogOne(),sum);
                addWordHypothesis(index,"<noop>",remainder);
            } else {
                ConfusionSet newSet = new ConfusionSet();
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
     */
    public void addWordHypothesis(int position, WordResult word) {
        getConfusionSet(position).addWordHypothesis(word);
    }

    public void addWordHypothesis(int position, String word, 
                                  double confidence) {
        WordResult wr = new SimpleWordResult(word, confidence, logMath);
        addWordHypothesis(position,wr);
    }
    
    /**
     * @see edu.cmu.sphinx.result.ConfidenceResult#getBestHypothesis()
     */
    public Path getBestHypothesis() {
        WordResultPath path = new WordResultPath(logMath);
        Iterator i = confusionSetIterator();
        while (i.hasNext()) {
            ConfusionSet cs = (ConfusionSet)i.next();
            path.add(cs.getBestHypothesis());
        }
        return path;
    }
    
    /**
     * Get a string representing the best path through the sausage.
     * 
     * @return best string
     */
    public String getBestHypothesisString() {
        return getBestHypothesis().toString();
    }
    
    /**
     * Get the word hypothesis with the highest posterior for a word slot
     * 
     * @param pos the word slot to look at
     * @return the word with the highest posterior in the slot
     */
    public Set getBestWordHypothesis(int pos) {        
        ConfusionSet set = (ConfusionSet)confusionSets.get(pos);
        return (Set)set.get(set.lastKey());
    }

    /**
     * Get the the highest posterior for a word slot
     * 
     * @param pos the word slot to look at
     * @return the highest posterior in the slot
     */

    public double getBestWordHypothesisPosterior(int pos) {
        ConfusionSet set = (ConfusionSet)confusionSets.get(pos);
        return ((Double)set.lastKey()).doubleValue();        
    }
    
    /**
     * Get the confusion set stored in a given word slot.
     * 
     * @param pos the word slot to look at.
     * @return a map from Double posteriors to Sets of String words,
     *         sorted from lowest to highest.
     */
    public ConfusionSet getConfusionSet(int pos) {
        return (ConfusionSet)confusionSets.get(pos);
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
            ListIterator i = confusionSets.listIterator();
            while (i.hasNext()) {
                int index = i.nextIndex();
                ConfusionSet set = (ConfusionSet)i.next();
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
