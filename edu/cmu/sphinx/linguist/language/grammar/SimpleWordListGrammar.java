/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.linguist.language.grammar;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import edu.cmu.sphinx.util.ExtendedStreamTokenizer;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

/**
 * Defines a grammar based upon a list of words in a file.
 * The format of the file is just one word per line. For example,
 * for an isolated digits grammar the file will simply look like:
 * <pre>
 * zero
 * one
 * two
 * three
 * four
 * five
 * six
 * seven
 * eight
 * nine
 * </pre>
 * The path to the file is defined by the {@link #PROP_PATH PROP_PATH}
 * property. If the {@link #PROP_LOOP PROP_LOOP} property is true,
 * the grammar created will be a looping grammar.
 * Using the above digits grammar example,
 * setting PROP_LOOP to true will make it a connected-digits grammar.
 * <p>
 * All probabilities are maintained in LogMath log base.
 */
public class SimpleWordListGrammar extends Grammar implements Configurable {
    /**
     * Sphinx property that defines the location of the word list grammar
     */
    public final static String PROP_PATH = "path";
    /**
     * The default value for PROP_PATH.
     */
    public final static String PROP_PATH_DEFAULT = "spelling.gram";
    /**
     * Sphinx property that if true, indicates that this is a looping grammar
     */
    public final static String PROP_LOOP = "isLooping";
    /**
     * The default value for PROP_LOOP.
     */
    public final static boolean PROP_LOOP_DEFAULT = true;
    
    /**
     * Sphinx property that defines the logMath component. 
     */
    
    public final static String PROP_LOG_MATH = "logMath";
    
    
    // ---------------------
    // Configurable data
    // ---------------------
    private String name;
    private String path;
    private boolean isLooping;
    private LogMath logMath;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name, registry);
        registry.register(PROP_PATH, PropertyType.STRING);
        registry.register(PROP_LOOP, PropertyType.BOOLEAN);
        registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        path = ps.getString(PROP_PATH, PROP_PATH_DEFAULT);
        isLooping = ps.getBoolean(PROP_LOOP, PROP_LOOP_DEFAULT);
        logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
    }


    /**
     * Create class from reference text (not implemented).
     * 
     * @param bogusText
     *                dummy variable
     * 
     * @throws NoSuchMethogException
     *                 if called with reference sentence
     */
    protected GrammarNode createGrammar(String bogusText)
            throws NoSuchMethodException {
        throw new NoSuchMethodException("Does not create "
                + "grammar with reference text");
    }

    /**
     * Creates the grammar.
     *  
     */
    protected GrammarNode createGrammar() throws IOException {
        int identity = 0;
        ExtendedStreamTokenizer tok = new ExtendedStreamTokenizer(path, true);
        GrammarNode initialNode = createGrammarNode(identity++, "<sil>");
        GrammarNode branchNode = createGrammarNode(identity++, false);
        GrammarNode finalNode = createGrammarNode(identity++, "<sil>");
        finalNode.setFinalNode(true);
        List wordGrammarNodes = new LinkedList();
        while (!tok.isEOF()) {
            String word;
            while ((word = tok.getString()) != null) {
                word = word.toLowerCase();
                GrammarNode wordNode = createGrammarNode(identity++, word);
                wordGrammarNodes.add(wordNode);
            }
        }
        // now connect all the GrammarNodes together
        initialNode.add(branchNode, LogMath.getLogOne());
        float branchScore = logMath.linearToLog(
                1.0 / wordGrammarNodes.size());
        for (Iterator i = wordGrammarNodes.iterator(); i.hasNext();) {
            GrammarNode wordNode = (GrammarNode) i.next();
            branchNode.add(wordNode, branchScore);
            wordNode.add(finalNode, LogMath.getLogOne());
            if (isLooping) {
                wordNode.add(branchNode, LogMath.getLogOne());
            }
        }
        
        return initialNode;
    }
}
