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
 * Created on Aug 10, 2004
 */
package edu.cmu.sphinx.result;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

class ClusterComparator implements Comparator {
    
    /**
     * Compares to clusters according to their topological relationship. Relies
     * on strong assumptions about the possible constituents of clusters which
     * will only be valid during the sausage creation process.
     * 
     * @param o1 the first cluster (must be a List)
     * @param o2 the second cluster (must be a List)
     */
    public int compare(Object o1, Object o2) {
        List cluster1 = (List) o1;
        List cluster2 = (List) o2;
        Iterator i = cluster1.iterator();
        while (i.hasNext()) {
            Node n1 = (Node)i.next();
            Iterator i2 = cluster2.iterator();
            while (i2.hasNext()) {
                Node n2 = (Node)i2.next();
                if (n1.isAncestorOf(n2)) {
                    return -1;
                } else if (n2.isAncestorOf(n1)) {
                    return 1;
                }
            }
        }
        return 0;
    }
}

/**
 * The SausageMaker takes word lattices as input and turns them into sausages 
 * (Confusion Networks) according to Mangu, Brill and Stolcke, "Finding 
 * Consensus in Speech Recognition: word error minimization and other 
 * applicatiosn of confusion networks", Computer Speech and Language, 2000. 
 * 
 * @author pgorniak
 *
 */
public class SausageMaker implements ConfidenceScorer, Configurable {
    
    /**
     * Sphinx property that defines the language model weight.
     */
    public final static String PROP_LANGUAGE_WEIGHT = "languageWeight";

    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;
    
    private String name;
    private float languageWeight;
    
    protected Lattice lattice;
    
    /**
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_LANGUAGE_WEIGHT, PropertyType.FLOAT);
    }


    /**
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        languageWeight = ps.getFloat(PROP_LANGUAGE_WEIGHT,
                                     PROP_LANGUAGE_WEIGHT_DEFAULT);
    }

    /**
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Construct an empty sausage maker
     *
     */
    public SausageMaker() {
    }
    
    /**
     * Construct a sausage maker
     *
     * @param l the lattice to construct a sausage from
     */
    public SausageMaker(Lattice l) {
        lattice = l;
    }

    /**
     * Perform the inter word clustering stage of the algorithm
     * 
     * @param clusters the current cluster set
     */
    protected void interWordCluster(List clusters) {
        while(interWordClusterStep(clusters));
    }

    /**
     * Perform one inter word clustering step of the algorithm
     * 
     * @param clusters the current cluster set
     */
    protected boolean interWordClusterStep(List clusters) {
        List toBeMerged1 = null;
        List toBeMerged2 = null;
        double maxSim = Double.NEGATIVE_INFINITY;
        ListIterator i = clusters.listIterator();
        while (i.hasNext()) {
            List c1 = (List)i.next();
            if (!i.hasNext()) {
                break;
            }
            ListIterator j = clusters.listIterator(i.nextIndex());
            while (j.hasNext()) {
                List c2 = (List)j.next();
                double sim = interClusterDistance(c1,c2);
                if (sim > maxSim) {
                    maxSim = sim;
                    toBeMerged1 = c1;
                    toBeMerged2 = c2;
                }
            }
        }
        if (toBeMerged1 != null) {
            clusters.remove(toBeMerged2);
            toBeMerged1.addAll(toBeMerged2);
            return true;
        }
        return false;
    }
    
    /**
     * Find the string edit distance between to lists of objects.
     * Objects are compared using .equals()
     * TODO: could be moved to a general utility class
     * 
     * @param p1 the first list 
     * @param p2 the second list
     * @return the string edit distance between the two lists
     */
    protected int stringEditDistance(List p1, List p2) {
        if (p1.size() == 0) {
            return p2.size();
        }
        if (p2.size() == 0) {
            return p1.size();
        }
        int [][] distances = new int[p1.size()][p2.size()];
        distances[0][0] = 0;
        for (int i=0;i<p1.size();i++) {
            distances[i][0] = i;
        }
        for (int j=0;j<p2.size();j++) {
            distances[0][j] = j;
        }
        for (int i=1;i<p1.size();i++) {
            for (int j=1;j<p2.size();j++) {
                int min = Math.min(distances[i-1][j-1]
                                       + (p1.get(i).equals(p2.get(j)) ? 0 : 1),
                                   distances[i-1][j] + 1);
                min = Math.min(min,distances[i][j-1] + 1);
            }            
        }
        return distances[p1.size()-1][p2.size()-1];
    }
    
    /**
     * Compute the phonetic similarity of two lattice nodes, based on the string
     * edit distance between their most likely pronunciations.
     * TODO: maybe move to Node.java?
     * 
     * @param n1 the first node
     * @param n2 the second node
     * @return the phonetic similarity, between 0 and 1
     */
    protected double computePhoneticSimilarity(Node n1, Node n2) {
        Pronunciation p1 = n1.getWord().getMostLikelyPronunciation();
        Pronunciation p2 = n2.getWord().getMostLikelyPronunciation();
        double sim = stringEditDistance(Arrays.asList(p1.getUnits()),
                        Arrays.asList(p2.getUnits()));
        sim /= (double)(p1.getUnits().length + p2.getUnits().length);
        return 1-sim;
    }
    
    /**
     * Return the total probability mass of the subcluster of nodes of the given
     * cluster that all have the given word as their word.
     * 
     * @param cluster the cluster to subcluster from
     * @param word the word to subcluster by
     * @return the log probability mass of the subcluster formed by the word
     */
    protected double wordSubClusterProbability(List cluster, String word) {
        return clusterProbability(makeWordSubCluster(cluster,word));
    }
    
    /**
     * Calculate the sum of posteriors in this cluster.
     * 
     * @param cluster the cluster to sum over
     * @return the probability sum
     */
    protected double clusterProbability(List cluster) {
        float p = LogMath.getLogZero();
        Iterator i = cluster.iterator();
        while (i.hasNext()) {
            p = lattice.getLogMath().addAsLinear(p,(float)((Node)i.next()).getPosterior());
        }
        return p;
    }
    
    /**
     * Form a subcluster by extracting all nodes corresponding to a given word.
     * 
     * @param cluster the parent cluster
     * @param word the word to cluster by
     * @return the subcluster.
     */
    protected List makeWordSubCluster(List cluster, String word) {
        Vector sub = new Vector();
        Iterator i = cluster.iterator();
        while (i.hasNext()) {
            Node n = (Node)i.next();
            if (n.getWord().getSpelling().equals(word)) {
                sub.add(n);
            }
        }
        return sub;
    }
    
    /**
     * Calculate the distance between two clusters
     * 
     * @param cluster1 the first cluster
     * @param cluster2 the second cluster
     * @return the inter cluster similarity, or Double.NEGATIVE_INFINITY if 
     *         these clusters should never be clustered together.
     */
    protected double interClusterDistance(List cluster1, List cluster2) {
        if (areClustersInRelation(cluster1,cluster2)) {
            return Double.NEGATIVE_INFINITY;
        }
        float totalSim = LogMath.getLogZero();
        float wordPairCount = (float)0.0;
        HashSet wordsSeen1 = new HashSet();
        
        Iterator i1 = cluster1.iterator();
        while (i1.hasNext()) {
            Node node1 = (Node)i1.next();
            String word1 = node1.getWord().getSpelling();
            if (wordsSeen1.contains(word1)) {
                continue;
            }
            wordsSeen1.add(word1);
            HashSet wordsSeen2 = new HashSet();
            Iterator i2 = cluster2.iterator();
            while (i2.hasNext()) {
                Node node2 = (Node)i2.next();
                String word2 = node2.getWord().getSpelling();
                if (wordsSeen2.contains(word2)) {
                    continue;
                }
                wordsSeen2.add(word2);
                float sim = (float)computePhoneticSimilarity(node1,node2);
                sim = lattice.getLogMath().linearToLog(sim);
                sim += wordSubClusterProbability(cluster1,word1);
                sim += wordSubClusterProbability(cluster2,word2);
                totalSim = lattice.getLogMath().addAsLinear(totalSim,sim);
                wordPairCount++;
            }
        }                        
        return totalSim - lattice.getLogMath().logToLinear(wordPairCount);
    }
    
    /**
     * Check whether these to clusters stand in a relation to each other.
     * Two clusters are related if a member of one is an ancestor of a member
     * of the other cluster.
     * 
     * @param cluster1 the first cluster
     * @param cluster2 the second cluster
     * @return true if the clusters are related
     */
    protected boolean areClustersInRelation(List cluster1, List cluster2) {
        Iterator i = cluster1.iterator();
        while (i.hasNext()) {
            Iterator j = cluster2.iterator();
            Node n1 = (Node)i.next();
            while (j.hasNext()) {
                if (n1.hasAncestralRelationship((Node)j.next())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Calculate the distance between two clusters, forcing them to have the same
     * words in them, and to not be related to each other.
     * 
     * @param cluster1 the first cluster
     * @param cluster2 the second cluster
     * @return The intra cluster distance, or Double.NEGATIVE_INFINITY if the clusters
     *         should never be clustered together.
     */
    protected double intraClusterDistance(List cluster1, List cluster2) {
        double maxSim = Double.NEGATIVE_INFINITY;
        Iterator i1 = cluster1.iterator();
        while (i1.hasNext()) {
            Node node1 = (Node)i1.next();
            Iterator i2 = cluster2.iterator();
            while (i2.hasNext()) {
                Node node2 = (Node)i2.next();
                if (!node1.getWord().getSpelling().equals(node2.getWord().getSpelling())) {
                    return Double.NEGATIVE_INFINITY;
                }
                if (node1.hasAncestralRelationship(node2)) {
                    return Double.NEGATIVE_INFINITY;
                }
                double overlap = 0.0;
                if (node1.getBeginTime() <= node2.getBeginTime() &&
                        node1.getEndTime() >= node2.getBeginTime()) {
                    overlap = node1.getEndTime() - node2.getBeginTime();
                    if (node1.getEndTime() > node2.getEndTime()) {
                        overlap -= node2.getEndTime() - node1.getEndTime(); 
                    }
                } else if(node2.getBeginTime() <= node1.getBeginTime() &&
                        node2.getEndTime() >= node1.getBeginTime()) {
                    overlap = node2.getEndTime() - node1.getBeginTime();                    
                    if (node2.getEndTime() > node1.getEndTime()) {
                        overlap -= node1.getEndTime() - node2.getEndTime(); 
                    }
                }
                if (overlap > 0.0) {
                    overlap = lattice.getLogMath().logToLinear((float)overlap);
                    overlap += node1.getPosterior() + node2.getPosterior();
                    if (overlap > maxSim) {
                        maxSim = overlap;
                    }
                }
            }
        }        
        return maxSim;
    }
    
    /**
     * print out a cluster for debugging
     * 
     * @param cluster
     */
    protected void printCluster(List cluster) {
        ListIterator j = cluster.listIterator();
        while (j.hasNext()) {
            System.out.print(" " + j.next());                
        }
	System.out.println();
    }
    
    /**
     * print out a list of clusters for debugging
     * 
     * @param clusters
     */
    protected void printClusters(List clusters) {
        ListIterator i = clusters.listIterator();
        while (i.hasNext()) {
            System.out.print("----cluster " + i.nextIndex() + " : ");
            printCluster((List)i.next());
        }
        System.out.println("----");
    }
    
    /**
     * Perform the intra word clustering stage of the algorithm
     * 
     * @param clusters the current list of clusters
     */
    protected void intraWordCluster(List clusters) {
        while (intraWordClusterStep(clusters));
    }
    
    /**
     * Perform a step of the intra word clustering stage
     * 
     * @param clusters the current list of clusters
     * @return did two clusters get merged?
     */
    protected boolean intraWordClusterStep(List clusters) {
        List toBeMerged1 = null;
        List toBeMerged2 = null;
        double maxSim = Double.NEGATIVE_INFINITY;
        ListIterator i = clusters.listIterator();
        while (i.hasNext()) {
            List c1 = (List)i.next();
            if (!i.hasNext()) {
                break;
            }
            ListIterator j = clusters.listIterator(i.nextIndex());
            while (j.hasNext()) {
                List c2 = (List)j.next();
                double sim = intraClusterDistance(c1,c2);
                if (sim > maxSim) {
                    maxSim = sim;
                    toBeMerged1 = c1;
                    toBeMerged2 = c2;
                }
            }
        }
        if (toBeMerged1 != null) {
            clusters.remove(toBeMerged2);
            toBeMerged1.addAll(toBeMerged2);
            return true;
        }
        return false;
    }
    
    /**
     * Turn the lattice contained in this sausage maker into a sausage object.
     * 
     * @return the sausage producing by collapsing the lattice.
     */
    public Sausage makeSausage() {
        //TODO: this first loop should be taken out as soon as node have
        //proper starting times without it.
        Iterator n = lattice.getNodes().iterator();
        while (n.hasNext()) {
            ((Node)n.next()).calculateBeginTime();
        }
        Vector clusters = new Vector(lattice.getNodes().size());
        Collection nodes = lattice.nodes.values();
        Iterator i = nodes.iterator();
        while(i.hasNext()) {
            Vector bucket = new Vector(1);
            bucket.add(i.next());
            clusters.add(bucket);
        }
        intraWordCluster(clusters);
        interWordCluster(clusters);
        Collections.sort(clusters,new ClusterComparator());
        Sausage sausage = new Sausage(clusters.size());
        ListIterator c1 = clusters.listIterator();
        while (c1.hasNext()) {
            HashSet seenWords = new HashSet();
            int index = c1.nextIndex();
            List cluster = ((List)c1.next());
            Iterator c2 = cluster.iterator();
            while (c2.hasNext()) {
                Node node = (Node)c2.next();
                Word word = node.getWord();
                if (seenWords.contains(word.getSpelling())) {
                    continue;
                }
                seenWords.add(word.getSpelling());
                SimpleWordResult swr = new SimpleWordResult
                    (node,
                     wordSubClusterProbability(cluster,word.getSpelling()),
                     lattice.getLogMath());
                sausage.addWordHypothesis(index,swr);
            }
        }
        sausage.fillInBlanks(lattice.getLogMath());
        return sausage;
    }
    
    /**
     * @see edu.cmu.sphinx.result.ConfidenceScorer#score(edu.cmu.sphinx.result.Result)
     */
    public ConfidenceResult score(Result result) {
        lattice = new Lattice(result);
        LatticeOptimizer lop = new LatticeOptimizer(lattice);
        lop.optimize();
        lattice.computeNodePosteriors(languageWeight);
        return makeSausage();
    }
}
