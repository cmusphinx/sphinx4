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

package edu.cmu.sphinx.decoder.search;


import edu.cmu.sphinx.util.SphinxProperties;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: woelfel
 * Date: Jun 16, 2003
 * Time: 11:45:05 AM
 * To change this template use Options | File Templates.
 */
public class AlternateHypothesisManager {

    public AlternateHypothesisManager(SphinxProperties props) {
        this.props = props;
    }

    private Map viterbiLoserMap = new HashMap();

    protected SphinxProperties props;
    protected double logBase;

    public SphinxProperties getProperties() {
        return props;
    }

    /**
     * Collects adds alternate predecessors for a token
     * that would have lost because of viterbi.
     *
     * @param token - a token that has an alternate lower
     * scoring predecessor that still might be of interest
     *
     * @param predecessor - a predecessor that scores lower
     * than token.getPredecessor().
     */

    public void addAlternatePredecessor(Token token, Token predecessor) {
        assert predecessor != token.getPredecessor();
        List list = (List) viterbiLoserMap.get(token);
        if (list == null) {
            list = new ArrayList();
            viterbiLoserMap.put(token, list);
        }
        list.add(predecessor);
    }

    /**
     * Returns a list of alternate predecessors for a token.
     *
     * @param token - a token that may have alternate lower
     * scoring predecessor that still might be of interest
     *
     * @return A list of predecessors that scores lower
     * than token.getPredecessor().
     */
    public List getAlternatePredecessors(Token token) {
        return (List) viterbiLoserMap.get(token);
    }

    /**
     * Purge all but max number of alternate preceding token
     * hypotheses.
     */
    public void purge() {

        int max = props.getInt("edu.cmu.sphinx.result.Lattice.maxEdgesToNode",10)-1;
        Iterator iterator = viterbiLoserMap.keySet().iterator();

        while (iterator.hasNext()) {
            Object key = iterator.next();
            List list = (List) viterbiLoserMap.get(key);
            Collections.sort(list, Token.COMPARATOR);
            List newList = list.subList(0, list.size() > max ? max : list.size());
            viterbiLoserMap.put(key, newList);
        }
    }


    public void changeSuccessor(Token newSuccessor, Token oldSuccessor) {
        Object list = viterbiLoserMap.get(oldSuccessor);
        viterbiLoserMap.put(newSuccessor, list);
        viterbiLoserMap.remove(oldSuccessor);
    }

}

