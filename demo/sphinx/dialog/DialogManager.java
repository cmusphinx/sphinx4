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

package demo.sphinx.dialog;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Iterator;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleParse;
import javax.speech.recognition.RuleGrammar;



public class DialogManager implements Configurable {
    /**
     * Sphinx property that defines the name of the grammar component 
     * to be used by this dialog manager
     */
    public final static String PROP_JSGF_GRAMMAR = "jsgfGrammar";

    /**
     * Sphinx property that defines the name of the microphone to be used 
     * by this dialog manager
     */
    public final static String PROP_MICROPHONE = "microphone";

    /**
     * Sphinx property that defines the name of the recognizer to be used by
     * this dialog manager
     */
    public final static String PROP_RECOGNIZER = "recognizer";

    // ------------------------------------
    // Configuration data
    // ------------------------------------
    private JSGFGrammar grammar;
    private Logger logger;
    private Recognizer recognizer;
    private Microphone microphone;

    // ------------------------------------
    // local data
    // ------------------------------------
    private DialogNode initialNode;
    private Map nodeMap;
    private String name;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_JSGF_GRAMMAR, PropertyType.COMPONENT);
        registry.register(PROP_MICROPHONE, PropertyType.COMPONENT);
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        grammar = 
            (JSGFGrammar) ps.getComponent(PROP_JSGF_GRAMMAR, JSGFGrammar.class);
        microphone = 
            (Microphone) ps.getComponent(PROP_MICROPHONE, Microphone.class);
        recognizer = 
            (Recognizer) ps.getComponent(PROP_RECOGNIZER, Recognizer.class);
    }


    /**
     * Adds a new node to the dialog manager. 
     *
     * @param the name of the node
     * @param behavior the application specified behavior for the node
     */
    public void addNode(String name, DialogNodeBehavior behavior) {
        DialogNode node = new DialogNode(name, behavior);
        putNode(node);
    }

    /**
     * Sets the name of the initial node for the dialog manager
     *
     * @param name the name of the initial node. Must be the name of a
     * previously added dialog node.
     */
    public void setInitialNode(String name) {
        if (getNode(name) == null) {
            throw new IllegalArgumentException("Unknown node " + name);
        }
        initialNode = getNode(name);
    }

    /**
     * Gets the recognizer and the dialog nodes ready to run
     *
     * @throws IOException if an error occurs while allocating the
     * recognizer.
     */
    public void allocate() throws IOException {
        recognizer.allocate();

        for (Iterator i = nodeMap.values().iterator(); i.hasNext(); ) {
            DialogNode node = (DialogNode) i.next();
            node.init();
        }
    }

    /**
     * Releases all resources allocated by the dialog manager
     */
    public void deallocate() {
        recognizer.deallocate();
    }

    /**
     * Invokes the dialog manager. The dialog manager begin to process
     * the dialog states starting at the initial node. This method
     * will not return until the dialog manager is finished processing
     * states
     */
    public void go() {
        DialogNode lastNode = null;
        DialogNode curNode = initialNode;

        try {
            while (true) {

                if (curNode != lastNode) {
                    if (lastNode != null) {
                        lastNode.exit();
                    }
                    curNode.enter();
                    lastNode = curNode;
                } 
                String nextStateName  = curNode.recognize();
                if (nextStateName == null || nextStateName.length() == 0) {
                    continue;
                } else {
                    DialogNode node = (DialogNode) nodeMap.get(nextStateName);
                    if (node == null) {
                        warn("Can't transition to unknown state " 
                                + nextStateName);
                    } else {
                        curNode = node;
                    }
                }
            }
        } catch (GrammarException ge) {
            error("grammar problem in state " + curNode.getName() 
                    + " " + ge);
        } catch (IOException ioe) {
            error("problem loading grammar in state " + curNode.getName() 
                    + " " + ioe);
        }
    }


    /**
     * Returns the name of this component
     *
     * @return the name of the component.
     */
    public String getName() {
        return name;
    }


    /**
     * Gets the dialog node with the given name
     *
     * @param name the name of the node
     */
    private DialogNode getNode(String name) {
        return (DialogNode) nodeMap.get(name);
    }

    /**
     * Puts a node into the node map
     *
     * @param node the node to place into the node map
     */
    private void putNode(DialogNode node) {
        nodeMap.put(node.getName(), node);
    }


    /**
     * Issues a warning message
     *
     * @param s the message
     */
    private void warn(String s) {
        System.out.println("Warning: " + s);
    }

    /**
     * Issues an error message
     *
     * @param s the message
     */
    private void error(String s) {
        System.out.println("Error: " + s);
    }

    /**
     * Issues a tracing message
     *
     * @parma s the message
     */
    private void trace(String s) {
        System.out.println("Trace: " + s);
    }

    /**
     * Represents a node in the dialog
     */
    class   DialogNode {
        private DialogNodeBehavior behavior;
        private String name;

        /**
         * Creates a dialog node with the given name an application
         * behavior
         *
         * @param name the name of the node
         *
         * @param behavior the application behavor for the node
         *
         */
        DialogNode(String name, DialogNodeBehavior behavior) {
            this.behavior = behavior;
            this.name = name;
        }


        /**
         * Initializes the node
         */
        
        void init() {
            behavior.onInit(this);
        }

        /**
         * Enters the node, prepares it for recognition
         */
        void enter() throws IOException {
            trace("Entering " + name);
            behavior.onEntry();
        }

        /**
         * Performs recognition at the node.
         *
         * @return the result tag
         */
        String recognize() throws GrammarException {
            trace("Recognize " + name);
            Result result = recognizer.recognize();
            return behavior.onRecognize(result);
        }

        /**
         * Exits the node
         */
        void exit() {
            trace("Exiting " + name);
            behavior.onExit();
        }

        /**
         * Gets the name of the node
         *
         * @return the name of the node
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the JSGF Grammar for the dialog manager that
         * contains this node
         *
         * @return the grammar
         */
        public JSGFGrammar getGrammar() {
            return grammar;
        }

        /**
         * Traces a message
         *
         * @param msg the message to trace
         */
        public void trace(String msg) {
            DialogManager.this.trace(msg);
        }
    }
}

/**
* Provides the default behavior for dialog node. Applications will
* typically extend this class and override methods as appropriate
*/
class DialogNodeBehavior {
    private DialogManager.DialogNode node;

    /**
     * Called during the initialization phase
     *
     * @param node the dialog node that the behavior is attached to
     */
    public void onInit(DialogManager.DialogNode node) {
        this.node = node;
    }

    /**
     * Called when this node becomes the active node
     */
    public void onEntry() throws IOException {
        trace("Entering " + getName());
    }

    /*
     * Called with the recognition results. Should return a string
     * representing the name of the next node.
     */
    public String onRecognize(Result result) throws GrammarException {
        String tagString =  getTagString(result);
        trace("Recognize result: " + result.getBestFinalResultNoFiller());
        trace("Recognize tag   : " + tagString);
        return tagString;
    }

    /**
     * Called when this node is no lnoger the active node
     */
    public void onExit() {
        trace("Exiting " + getName());
    }

    /**
     * Returns the name for this node
     *
     * @return the name of the node
     */
    public String getName() {
        return node.getName();
    }

    /**
     * Returns the string representation of this object
     *
     * @return the string representation of this object
     */
    public String toString() {
        return "Node " + getName();
    }

    /**
     * Retrieves the grammar associated with this ndoe
     *
     * @return the grammar
     */
    public JSGFGrammar getGrammar() {
        return node.getGrammar();
    }

    /**
     * Retrieves the rule parse for the given result
     *
     * @param the recognition result
     * @return the rule parse for the result
     * @throws GrammarException if there is an error while parsing the
     * result
     */
    RuleParse getRuleParse(Result result) throws GrammarException {
        String resultText = result.getBestFinalResultNoFiller();
        RuleGrammar ruleGrammar = getGrammar().getRuleGrammar();
        RuleParse ruleParse = ruleGrammar.parse(resultText, null);
        return ruleParse;
    }

    /**
     * Gets a space delimited string of tags representing the result
     *
     * @param result the recognition result
     * @return the tag string
     * @throws GrammarException if there is an error while parsing the
     * result
     */
    String getTagString(Result result) throws GrammarException {
        String tagString = null;
        RuleParse ruleParse = getRuleParse(result);
        if (ruleParse != null) {
            String[] tags = ruleParse.getTags();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < tags.length; i++) {
                sb.append(tags[i]);
                if (i < tags.length -1) {
                    sb.append(" ");
                }
            }
            tagString = sb.toString();
        }
        return tagString;
    }

    /**
     * Outputs a trace message
     *
     * @param the trace message
     */
    void trace(String msg) {
        node.trace(msg);
    }
}


/**
 * A Dialog node behavior that loads a completely new
 * grammar upon entry into the node
 */
class NewGrammarDialogNodeBehavior extends DialogNodeBehavior {
    private String grammarName;

    /**
     * creates a  NewGrammarDialogNodeBehavior 
     *
     * @param grammarName the grammar name
     */
    public NewGrammarDialogNodeBehavior(String grammarName) {
        this.grammarName = grammarName;
    }

    /**
     * Called with the dialog manager enters this entry
     */
    public void onEntry() throws IOException {
        super.onEntry();
        getGrammar().loadJSGF(grammarName);
    }

    /**
     * Returns the name of the grammar
     *
     * @return the grammar name
     */
    public String getGrammarName() {
        return grammarName;
    }
}
