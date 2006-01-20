package edu.cmu.sphinx.linguist.language.grammar;

import edu.cmu.sphinx.util.props.Registry;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Jan 10, 2006
 * Time: 9:42:36 AM
 */
public class BatchForcedAlignerGrammar extends ForcedAlignerGrammar implements GrammarInterface {

    /**
     * Property that defines the reference file containing the transcripts used to create the froced align grammar
     */
    public final static String PROP_REF_FILE = "refFile";

    protected String refFile;
    protected Hashtable grammars;
    protected String currentUttName = "";
    protected Grammar currentGrammar = null;

    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
    *      edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry) throws PropertyException {
        super.register(name,registry);
        registry.register(PROP_REF_FILE, PropertyType.STRING);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        refFile = ps.getString(PROP_REF_FILE,"<refFile not set>");
        grammars = new Hashtable();
        currentGrammar = (Grammar) grammars.get(currentUttName);
    }

    protected GrammarNode createGrammar() {
            // TODO: FlatLinguist requires the initial grammar node
            // to contain a single silence. We'll do that for now,
            // but once the FlatLinguist is fixed, this should be
            // returned to its former method of creating an empty
            // initial grammar node
            //          initialNode = createGrammarNode(initialID, false);

        initialNode = null;
        finalNode = createGrammarNode(true);
        try {
            LineNumberReader in = new LineNumberReader( new FileReader( refFile) );
            String line;
            while( !(line=in.readLine()).equals( "" ) ) {
                int i = line.indexOf('(');
                String uttName = line.substring( i+1,line.indexOf(')') );
                String transcript = line.substring(0,i).trim();
                if( !transcript.equals("") ) {
                    initialNode = createGrammarNode(Dictionary.SILENCE_SPELLING);
                    createForcedAlignerGrammar( initialNode,finalNode,transcript );
                    grammars.put(uttName,initialNode);
                    currentUttName=uttName;
                }
            }
        } catch (FileNotFoundException e) {
            throw new Error( e );
        } catch (IOException e) {
            throw new Error( e );
        } catch (NoSuchMethodException e) {
            throw new Error( e );
        }
        return initialNode;
    }

    public GrammarNode getInitialNode() {
        return initialNode;
    }

    public void setUtterance( String utteranceName ) {
        initialNode = (GrammarNode) grammars.get(utteranceName);
        assert initialNode != null;
    }
}
