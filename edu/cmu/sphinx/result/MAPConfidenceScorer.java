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
 */
package edu.cmu.sphinx.result;

import edu.cmu.sphinx.decoder.search.Token;

import edu.cmu.sphinx.linguist.WordSearchState;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.util.Collection;
import java.util.Iterator;

/**
 * Computes confidences for the highest scoring path in a Result.
 * The highest scoring path refers to the path with the maximum
 * a posteriori (MAP) probability, which is why this class is so named.
 */
public class MAPConfidenceScorer implements ConfidenceScorer, Configurable {

    /**
     * Sphinx property that defines the language model weight.
     */
    public final static String PROP_LANGUAGE_WEIGHT = "languageWeight";

    /**
     * The default value for the PROP_LANGUAGE_WEIGHT property
     */
    public final static float PROP_LANGUAGE_WEIGHT_DEFAULT  = 1.0f;
    
    /**
     * Sphinx property that specifies whether to dump the lattice.
     */
    public final static String PROP_DUMP_LATTICE = "dumpLattice";

    /**
     * The default value of PROP_DUMP_LATTICE.
     */
    public final static boolean PROP_DUMP_LATTICE_DEFAULT = false;

    /**
     * Sphinx property that specifies whether to dump the sausage.
     */
    public final static String PROP_DUMP_SAUSAGE = "dumpSausage";

    /**
     * The default value of PROP_DUMP_SAUSAGE.
     */
    public final static boolean PROP_DUMP_SAUSAGE_DEFAULT = false;


    private String name;
    private float languageWeight;
    private boolean dumpLattice;
    private boolean dumpSausage;

    
    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
        throws PropertyException {
        this.name = name;
        registry.register(PROP_LANGUAGE_WEIGHT, PropertyType.FLOAT);
        registry.register(PROP_DUMP_LATTICE, PropertyType.BOOLEAN);
        registry.register(PROP_DUMP_SAUSAGE, PropertyType.BOOLEAN);
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        languageWeight = ps.getFloat(PROP_LANGUAGE_WEIGHT,
                                     PROP_LANGUAGE_WEIGHT_DEFAULT);
        dumpLattice = ps.getBoolean(PROP_DUMP_LATTICE,
                                    PROP_DUMP_LATTICE_DEFAULT);
        dumpSausage = ps.getBoolean(PROP_DUMP_SAUSAGE,
                                    PROP_DUMP_SAUSAGE_DEFAULT);
    }


    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }


    /**
     * Computes confidences for a Result and returns a ConfidenceResult,
     * a compact representation of all the hypothesis contained in the
     * result together with their per-word and per-path confidences.
     *
     * @param result the result to compute confidences for
     * @return a confidence result
     */
    public ConfidenceResult score(Result result) {
        Lattice lattice = new Lattice(result);
        LatticeOptimizer lop = new LatticeOptimizer(lattice);
        lop.optimize();
        lattice.computeNodePosteriors(languageWeight);
        SausageMaker sm = new SausageMaker(lattice);
        Sausage s = sm.makeSausage();

        if (dumpLattice) {
            lattice.dumpAISee("mapLattice.gdl", "MAP Lattice");
        }
        if (dumpSausage) {
            s.dumpAISee("mapSausage.gdl", "MAP Sausage");
        }

        ConfidenceResult sausage = (ConfidenceResult) s;
        WordResultPath mapPath = new WordResultPath();
        Token mapToken = result.getBestToken();

        /* start with the last slot */
        int slot = sausage.size() - 1;

        while (mapToken != null) {

            /* find a word token */
            while (!mapToken.isWord()) {
                mapToken = mapToken.getPredecessor();
            }

            /* if a word is found */
            if (mapToken != null) {
                String word = mapToken.getWord().getSpelling();
                WordResult wr = null;
                ConfusionSet cs = null;

                /* track through all the slots to find the word */
                while (slot >= 0 && wr == null) {
                    cs = sausage.getConfusionSet(slot);
                    wr = cs.getWordResult(word);
                    if (wr == null) {
                        if (slot > 0) {
                            slot--;
                        } else if (slot == 0) {
                            break;
                        }
                    }
                }
                if (wr != null) {
                    mapPath.add(0, wr);
                } else {
                    cs.dump("Slot " + slot);
                    throw new Error
                        ("Can't find WordResult in ConfidenceResult slot " +
                         slot + " for word " + word);
                }
                slot--;
                mapToken = mapToken.getPredecessor();
            } else {
                break;
            }
        }

        return (new MAPConfidenceResult(sausage, mapPath));
    }

    /**
     * The confidence result for the highest scoring path.
     */
    class MAPConfidenceResult implements ConfidenceResult {

        private ConfidenceResult sausage;
        private Path mapPath;

        /**
         * Constructs a MAPConfidenceResult.
         *
         * @param sausage the sausge that this MAPConfidenceResult is based on
         * @param mapPath the maximum posterior probability path
         */
        public MAPConfidenceResult(ConfidenceResult sausage, Path mapPath) {
            this.sausage = sausage;
            this.mapPath = mapPath;
        }

        /**
         * Returns the path with the maximum posterior probability path.
         * This path should be the same as that returned by 
         * Result.getBestToken().
         */
        public Path getBestHypothesis() {
            return mapPath;
        }

        /**
         * Get the number of word slots contained in this result
         *
         * @return length of the result
         */
        public int size() {
            return sausage.size();
        }

        /**
         * Iterator through the confusion sets in this result.
         *
         * @return confusion set iterator
         */
        public Iterator confusionSetIterator() {
            return sausage.confusionSetIterator();
        }

        /**
         * Get the nth confusion set in this result
         *
         * @param i the index of the confusion set to get
         * @return the requested confusion set
         */
        public ConfusionSet getConfusionSet(int i) {
            return sausage.getConfusionSet(i);
        }
    }
}
