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

import java.util.Iterator;

/**
 * Holds all the data for an utterance to be spoken. It is incrementally
 * modified by various UtteranceProcessor implementations. An utterance
 * contains a set of Features (essential a set of properties) and a set of
 * Relations. A Relation is an ordered set of Item graphs. The utterance
 * contains a set of features and implements FeatureSet so that applications
 * can set/get features directly from the utterance. If a feature query is not
 * found in the utterance feature set, the query is forwarded to the FeatureSet
 * of the voice associated with the utterance.
 */
public class Utterance {
    private FeatureSet features;
    private FeatureSet relations;

    /**
     * Creates an utterance with the given set of tokenized text.
     *
     * @param voice the voice associated with the utterance
     * @param tokenList the list of tokens for this utterance
     */
    public Utterance(CharTokenizer tokenizer) {
        features = new FeatureSet();
        relations = new FeatureSet();
        setTokenList(tokenizer);
    }

    /**
     * Creates a new relation with the given name and adds it to this
     * utterance.
     *
     * @param name the name of the new relation
     *
     * @return the newly created relation
     */
    public Relation createRelation(String name) {
        Relation relation = new Relation(name, this);
        relations.setObject(name, relation);
        return relation;
    }

    /**
     * Retrieves a relation from this utterance.
     *
     * @param name the name of the Relation
     *
     * @return the relation or null if the relation is not found
     */
    public Relation getRelation(String name) {
        return (Relation) relations.getObject(name);
    }

    /**
     * Determines if this utterance contains a relation with the given name.
     *
     * @param name the name of the relation of interest.
     */
    public boolean hasRelation(String name) {
        return relations.isPresent(name);
    }

    /**
     * Removes the named feature from this set of features.
     *
     * @param name the name of the feature of interest
     */
    public void remove(String name) {
        features.remove(name);
    }

    /**
     * Convenience method that sets the named feature as an int.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setInt(String name, int value) {
        features.setInt(name, value);
    }

    /**
     * Convenience method that sets the named feature as a float.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setFloat(String name, float value) {
        features.setFloat(name, value);
    }

    /**
     * Convenience method that sets the named feature as a String.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setString(String name, String value) {
        features.setString(name, value);
    }

    /**
     * Sets the named feature.
     *
     * @param name the name of the feature
     * @param value the value of the feature
     */
    public void setObject(String name, Object value) {
        features.setObject(name, value);
    }

    /**
     * Returns the Item in the given Relation associated with the given time.
     *
     * @param relation the name of the relation
     * @param time the time
     *
     * @throws IllegalStateException if the Segment durations have not been
     *         calculated in the Utterance or if the given relation is not
     *         present in the Utterance
     */
    public Item getItem(String relation, float time) {
        Relation segmentRelation = null;
        String pathName = null;

        if (relation.equals(Relation.WORD)) {
            pathName = "R:SylStructure.parent.parent.R:Word";
        } else if (relation.equals(Relation.TOKEN)) {
            pathName = "R:SylStructure.parent.parent.R:Token.parent";
        } else {
            throw new IllegalArgumentException(
                    "Utterance.getItem(): relation cannot be " + relation);
        }

        PathExtractor path = new PathExtractor(pathName, false);

        // get the Item in the Segment Relation with the given time
        Item segmentItem = getItem(segmentRelation, time);

        if (segmentItem != null) {
            return path.findItem(segmentItem);
        } else {
            return null;
        }
    }

    private static Item getItem(Relation segmentRelation, float time) {
        Item lastSegment = segmentRelation.getTail();
        // If given time is closer to the front than the end, search from
        // the front; otherwise, start search from end
        // this might not be the best strategy though.
        float lastSegmentEndTime = getSegmentEnd(lastSegment);
        if (time < 0 || lastSegmentEndTime < time) {
            return null;
        } else if (lastSegmentEndTime - time > time) {
            return findFromFront(segmentRelation, time);
        } else {
            return findFromEnd(segmentRelation, time);
        }
    }

    private static Item findFromEnd(Relation segmentRelation, float time) {
        Item item = segmentRelation.getTail();
        while (item != null && getSegmentEnd(item) > time) {
            item = item.getPrevious();
        }

        if (item != segmentRelation.getTail()) {
            item = item.getNext();
        }

        return item;
    }

    private static Item findFromFront(Relation segmentRelation, float time) {
        Item item = segmentRelation.getHead();
        while (item != null && time > getSegmentEnd(item)) {
            item = item.getNext();
        }
        return item;
    }

    private static float getSegmentEnd(Item segment) {
        FeatureSet segmentFeatureSet = segment.getFeatures();
        return segmentFeatureSet.getFloat("end");
    }

    /**
     * Sets the token list for this utterance. Note that this could be
     * optimized by turning the token list directly into the token relation.
     *
     * @param tokenList the tokenList
     *
     */
    private void setTokenList(Iterator<Token> tokenizer) {
        Relation relation = createRelation(Relation.TOKEN);
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            String tokenWord = token.getWord();

            if (tokenWord != null && tokenWord.length() > 0) {
                Item item = relation.appendItem();

                FeatureSet featureSet = item.getFeatures();
                featureSet.setString("name", tokenWord);
                featureSet.setString("whitespace", token.getWhitespace());
                featureSet.setString("prepunctuation",
                        token.getPrepunctuation());
                featureSet.setString("punc", token.getPostpunctuation());
                featureSet.setString("file_pos",
                        String.valueOf(token.getPosition()));
                featureSet.setString("line_number",
                        String.valueOf(token.getLineNumber()));

            }
        }
    }
}
