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
 * Created on Nov 27, 2004
 *
 */
package edu.cmu.sphinx.result;

import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.*;
import javolution.util.FastSet;

import java.util.*;

/**
 * Parent to all sausage makers.
 * 
 * @author pgorniak
 *
 */
public abstract class AbstractSausageMaker implements ConfidenceScorer, Configurable {

    /**
     * A Cluster is a set of Nodes together with their earliest start time
     * and latest end time. A SausageMaker builds up a sequence of such clusters
     * that then gets turned into a Sausage.
     * @see Node
     * @see Sausage
     * @see SausageMaker
     */
    class Cluster {
        public int startTime;
        public int endTime;
        private List elements = new LinkedList();
        public Cluster(Node n) {
            startTime = n.getBeginTime();
            endTime = n.getEndTime();
            elements.add(n);
        }

        public Cluster(int start,int end) {
            startTime = start;
            endTime = end;
        }

        public void add(Node n) {
            if (n.getBeginTime() < startTime) {
                startTime = n.getBeginTime();
            }
            if (n.getEndTime() > endTime) {
                endTime = n.getEndTime();
            }
            elements.add(n);
        }

        public void add(Cluster c) {
            if (c.startTime < startTime) {
                startTime = c.startTime;
            }
            if (c.endTime > endTime) {
                endTime = c.endTime;
            }
            elements.addAll(c.getElements());
        }

        public Iterator iterator() {
            return elements.iterator();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("s: " + startTime + " e: " + endTime + "[");
            Iterator i = elements.iterator();
            while (i.hasNext()) {
                sb.append(i.next());
                if (i.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        }
        /**
         * @return Returns the elements.
         */
        public List getElements() {
            return elements;
        }
        /**
         * @param elements The elements to set.
         */
        public void setElements(List elements) {
            this.elements = elements;
        }
    }

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
            Cluster cluster1 = (Cluster) o1;
            Cluster cluster2 = (Cluster) o2;
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
     * Sphinx property that defines the language model weight.
     */
    public final static String PROP_LANGUAGE_WEIGHT = "languageWeight";

    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;

    private String name;
    protected float languageWeight;

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

    protected static int getOverlap(Node n, int startTime, int endTime) {
        return Math.min(n.getEndTime(),endTime) -
               Math.max(n.getBeginTime(),startTime);
    }

    protected static int getOverlap(Node n1, Node n2) {
        return Math.min(n1.getEndTime(),n2.getEndTime()) -
               Math.max(n1.getBeginTime(),n2.getBeginTime());
    }

    /**
     * Returns true if the two given clusters has time overlaps.
     *
     * @param cluster1 the first cluster to examine
     * @param cluster2 the second cluster to examine
     *
     * @return true if the clusters has overlap, false if they don't
     */
    protected boolean hasOverlap(Cluster cluster1, Cluster cluster2) {
        return (cluster1.startTime < cluster2.endTime &&
                cluster2.startTime < cluster1.endTime);
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
     * Return the total probability mass of the subcluster of nodes of the given
     * cluster that all have the given word as their word.
     * 
     * @param cluster the cluster to subcluster from
     * @param word the word to subcluster by
     * @return the log probability mass of the subcluster formed by the word
     */
    protected double wordSubClusterProbability(Cluster cluster, String word) {
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
     * Calculate the sum of posteriors in this cluster.
     * 
     * @param cluster the cluster to sum over
     * @return the probability sum
     */
    protected double clusterProbability(Cluster cluster) {
        return clusterProbability(cluster.elements);
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
     * Form a subcluster by extracting all nodes corresponding to a given word.
     * 
     * @param cluster the parent cluster
     * @param word the word to cluster by
     * @return the subcluster.
     */
    protected Cluster makeWordSubCluster(Cluster cluster, String word) {
        List l = makeWordSubCluster(cluster.elements,word);
        Cluster c = new Cluster(cluster.startTime,cluster.endTime);
        c.elements = l;
        return c;
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
            System.out.println(i.next());
        }
        System.out.println("----");
    }

    /**
     * Turn a list of lattice node clusters into a Sausage object.
     * @param clusters the list of node clusters in topologically correct order
     * @return the Sausage corresponding to the cluster list
     */
    protected Sausage sausageFromClusters(List clusters) {
        Sausage sausage = new Sausage(clusters.size());
        ListIterator c1 = clusters.listIterator();
        while (c1.hasNext()) {
            FastSet seenWords = new FastSet();
            int index = c1.nextIndex();
            Cluster cluster = ((Cluster)c1.next());
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
}
