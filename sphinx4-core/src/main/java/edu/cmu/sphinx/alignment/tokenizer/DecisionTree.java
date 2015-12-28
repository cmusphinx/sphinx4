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
package edu.cmu.sphinx.alignment.tokenizer;

import java.io.*;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * Implementation of a Classification and Regression Tree (CART) that is used
 * more like a binary decision tree, with each node containing a decision or a
 * final value. The decision nodes in the CART trees operate on an Item and
 * have the following format:
 *
 * <pre>
 *   NODE feat operand value qfalse
 * </pre>
 *
 * <p>
 * Where <code>feat</code> is an string that represents a feature to pass to
 * the <code>findFeature</code> method of an item.
 *
 * <p>
 * The <code>value</code> represents the value to be compared against the
 * feature obtained from the item via the <code>feat</code> string. The
 * <code>operand</code> is the operation to do the comparison. The available
 * operands are as follows:
 *
 * <ul>
 * <li>&lt; - the feature is less than value
 * <li>=- the feature is equal to the value
 * <li>&gt;- the feature is greater than the value
 * <li>MATCHES - the feature matches the regular expression stored in value
 * <li>IN - [[[TODO: still guessing because none of the CART's in Flite seem to
 * use IN]]] the value is in the list defined by the feature.
 * </ul>
 *
 * <p>
 * [[[TODO: provide support for the IN operator.]]]
 *
 * <p>
 * For &lt; and &gt;, this CART coerces the value and feature to float's. For =,
 * this CART coerces the value and feature to string and checks for string
 * equality. For MATCHES, this CART uses the value as a regular expression and
 * compares the obtained feature to that.
 *
 * <p>
 * A CART is represented by an array in this implementation. The
 * <code>qfalse</code> value represents the index of the array to go to if the
 * comparison does not match. In this implementation, qtrue index is always
 * implied, and represents the next element in the array. The root node of the
 * CART is the first element in the array.
 *
 * <p>
 * The interpretations always start at the root node of the CART and continue
 * until a final node is found. The final nodes have the following form:
 *
 * <pre>
 *   LEAF value
 * </pre>
 *
 * <p>
 * Where <code>value</code> represents the value of the node. Reaching a final
 * node indicates the interpretation is over and the value of the node is the
 * interpretation result.
 */
public class DecisionTree {
    /** Logger instance. */
    private static final Logger logger = Logger.getLogger(DecisionTree.class.getSimpleName());
    /**
     * Entry in file represents the total number of nodes in the file. This
     * should be at the top of the file. The format should be "TOTAL n" where n
     * is an integer value.
     */
    final static String TOTAL = "TOTAL";

    /**
     * Entry in file represents a node. The format should be
     * "NODE feat op val f" where 'feat' represents a feature, op represents an
     * operand, val is the value, and f is the index of the node to go to is
     * there isn't a match.
     */
    final static String NODE = "NODE";

    /**
     * Entry in file represents a final node. The format should be "LEAF val"
     * where val represents the value.
     */
    final static String LEAF = "LEAF";

    /**
     * OPERAND_MATCHES
     */
    final static String OPERAND_MATCHES = "MATCHES";

    /**
     * The CART. Entries can be DecisionNode or LeafNode. An ArrayList could be
     * used here -- I chose not to because I thought it might be quicker to
     * avoid dealing with the dynamic resizing.
     */
    Node[] cart = null;

    /**
     * The number of nodes in the CART.
     */
    transient int curNode = 0;

    /**
     * Creates a new CART by reading from the given URL.
     *
     * @param url the location of the CART data
     *
     * @throws IOException if errors occur while reading the data
     */
    public DecisionTree(URL url) throws IOException {
        BufferedReader reader;
        String line;

        reader = new BufferedReader(new InputStreamReader(url.openStream()));
        line = reader.readLine();
        while (line != null) {
            if (!line.startsWith("***")) {
                parseAndAdd(line);
            }
            line = reader.readLine();
        }
        reader.close();
    }

    /**
     * Creates a new CART by reading from the given reader.
     *
     * @param reader the source of the CART data
     * @param nodes the number of nodes to read for this cart
     *
     * @throws IOException if errors occur while reading the data
     */
    public DecisionTree(BufferedReader reader, int nodes) throws IOException {
        this(nodes);
        String line;
        for (int i = 0; i < nodes; i++) {
            line = reader.readLine();
            if (!line.startsWith("***")) {
                parseAndAdd(line);
            }
        }
    }

    /**
     * Creates a new CART that will be populated with nodes later.
     *
     * @param numNodes the number of nodes
     */
    private DecisionTree(int numNodes) {
        cart = new Node[numNodes];
    }

    /**
     * Dump the CART tree as a dot file.
     * <p>
     * The dot tool is part of the graphviz distribution at <a
     * href="http://www.graphviz.org/">http://www.graphviz.org/</a>. If
     * installed, call it as "dot -O -Tpdf *.dot" from the console to generate
     * pdfs.
     * </p>
     *
     * @param out The PrintWriter to write to.
     */
    public void dumpDot(PrintWriter out) {
        out.write("digraph \"" + "CART Tree" + "\" {\n");
        out.write("rankdir = LR\n");

        for (Node n : cart) {
            out.println("\t\"node" + n.hashCode() + "\" [ label=\""
                    + n.toString() + "\", color=" + dumpDotNodeColor(n)
                    + ", shape=" + dumpDotNodeShape(n) + " ]\n");
            if (n instanceof DecisionNode) {
                DecisionNode dn = (DecisionNode) n;
                if (dn.qtrue < cart.length && cart[dn.qtrue] != null) {
                    out.write("\t\"node" + n.hashCode() + "\" -> \"node"
                            + cart[dn.qtrue].hashCode()
                            + "\" [ label=" + "TRUE" + " ]\n");
                }
                if (dn.qfalse < cart.length && cart[dn.qfalse] != null) {
                    out.write("\t\"node" + n.hashCode() + "\" -> \"node"
                            + cart[dn.qfalse].hashCode()
                            + "\" [ label=" + "FALSE" + " ]\n");
                }
            }
        }

        out.write("}\n");
        out.close();
    }

    protected String dumpDotNodeColor(Node n) {
        if (n instanceof LeafNode) {
            return "green";
        }
        return "red";
    }

    protected String dumpDotNodeShape(Node n) {
        return "box";
    }

    /**
     * Creates a node from the given input line and add it to the CART. It
     * expects the TOTAL line to come before any of the nodes.
     *
     * @param line a line of input to parse
     */
    protected void parseAndAdd(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        String type = tokenizer.nextToken();
        if (type.equals(LEAF) || type.equals(NODE)) {
            cart[curNode] = getNode(type, tokenizer, curNode);
            cart[curNode].setCreationLine(line);
            curNode++;
        } else if (type.equals(TOTAL)) {
            cart = new Node[Integer.parseInt(tokenizer.nextToken())];
            curNode = 0;
        } else {
            throw new Error("Invalid CART type: " + type);
        }
    }

    /**
     * Gets the node based upon the type and tokenizer.
     *
     * @param type <code>NODE</code> or <code>LEAF</code>
     * @param tokenizer the StringTokenizer containing the data to get
     * @param currentNode the index of the current node we're looking at
     *
     * @return the node
     */
    protected Node getNode(String type, StringTokenizer tokenizer,
            int currentNode) {
        if (type.equals(NODE)) {
            String feature = tokenizer.nextToken();
            String operand = tokenizer.nextToken();
            Object value = parseValue(tokenizer.nextToken());
            int qfalse = Integer.parseInt(tokenizer.nextToken());
            if (operand.equals(OPERAND_MATCHES)) {
                return new MatchingNode(feature, value.toString(),
                        currentNode + 1, qfalse);
            } else {
                return new ComparisonNode(feature, value, operand,
                        currentNode + 1, qfalse);
            }
        } else if (type.equals(LEAF)) {
            return new LeafNode(parseValue(tokenizer.nextToken()));
        }

        return null;
    }

    /**
     * Coerces a string into a value.
     *
     * @param string of the form "type(value)"; for example, "Float(2.3)"
     *
     * @return the value
     */
    protected Object parseValue(String string) {
        int openParen = string.indexOf("(");
        String type = string.substring(0, openParen);
        String value = string.substring(openParen + 1, string.length() - 1);
        if (type.equals("String")) {
            return value;
        } else if (type.equals("Float")) {
            return new Float(Float.parseFloat(value));
        } else if (type.equals("Integer")) {
            return new Integer(Integer.parseInt(value));
        } else if (type.equals("List")) {
            StringTokenizer tok = new StringTokenizer(value, ",");
            int size = tok.countTokens();

            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                float fval = Float.parseFloat(tok.nextToken());
                values[i] = Math.round(fval);
            }
            return values;
        } else {
            throw new Error("Unknown type: " + type);
        }
    }

    /**
     * Passes the given item through this CART and returns the interpretation.
     *
     * @param item the item to analyze
     *
     * @return the interpretation
     */
    public Object interpret(Item item) {
        int nodeIndex = 0;
        DecisionNode decision;

        while (!(cart[nodeIndex] instanceof LeafNode)) {
            decision = (DecisionNode) cart[nodeIndex];
            nodeIndex = decision.getNextNode(item);
        }
        logger.fine("LEAF " + cart[nodeIndex].getValue());
        return ((LeafNode) cart[nodeIndex]).getValue();
    }

    /**
     * A node for the CART.
     */
    static abstract class Node {
        /**
         * The value of this node.
         */
        protected Object value;

        /**
         * Create a new Node with the given value.
         */
        public Node(Object value) {
            this.value = value;
        }

        /**
         * Get the value.
         */
        public Object getValue() {
            return value;
        }

        /**
         * Return a string representation of the type of the value.
         */
        public String getValueString() {
            if (value == null) {
                return "NULL()";
            } else if (value instanceof String) {
                return "String(" + value.toString() + ")";
            } else if (value instanceof Float) {
                return "Float(" + value.toString() + ")";
            } else if (value instanceof Integer) {
                return "Integer(" + value.toString() + ")";
            } else {
                return value.getClass().toString() + "(" + value.toString()
                        + ")";
            }
        }

        /**
         * sets the line of text used to create this node.
         *
         * @param line the creation line
         */
        public void setCreationLine(String line) {}
    }

    /**
     * A decision node that determines the next Node to go to in the CART.
     */
    abstract static class DecisionNode extends Node {
        /**
         * The feature used to find a value from an Item.
         */
        private PathExtractor path;

        /**
         * Index of Node to go to if the comparison doesn't match.
         */
        protected int qfalse;

        /**
         * Index of Node to go to if the comparison matches.
         */
        protected int qtrue;

        /**
         * The feature used to find a value from an Item.
         */
        public String getFeature() {
            return path.toString();
        }

        /**
         * Find the feature associated with this DecisionNode and the given
         * item
         *
         * @param item the item to start from
         * @return the object representing the feature
         */
        public Object findFeature(Item item) {
            return path.findFeature(item);
        }

        /**
         * Returns the next node based upon the descision determined at this
         * node
         *
         * @param item the current item.
         * @return the index of the next node
         */
        public final int getNextNode(Item item) {
            return getNextNode(findFeature(item));
        }

        /**
         * Create a new DecisionNode.
         *
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         * @param qtrue the Node index to go to if the comparison matches
         * @param qfalse the Node machine index to go to upon no match
         */
        public DecisionNode(String feature, Object value, int qtrue, int qfalse) {
            super(value);
            this.path = new PathExtractor(feature, true);
            this.qtrue = qtrue;
            this.qfalse = qfalse;
        }

        /**
         * Get the next Node to go to in the CART. The return value is an index
         * in the CART.
         */
        abstract public int getNextNode(Object val);
    }

    /**
     * A decision Node that compares two values.
     */
    static class ComparisonNode extends DecisionNode {
        /**
         * LESS_THAN
         */
        final static String LESS_THAN = "<";

        /**
         * EQUALS
         */
        final static String EQUALS = "=";

        /**
         * GREATER_THAN
         */
        final static String GREATER_THAN = ">";

        /**
         * The comparison type. One of LESS_THAN, GREATER_THAN, or EQUAL_TO.
         */
        String comparisonType;

        /**
         * Create a new ComparisonNode with the given values.
         *
         * @param feature the string used to get a value from an Item
         * @param value the value to compare to
         * @param comparisonType one of LESS_THAN, EQUAL_TO, or GREATER_THAN
         * @param qtrue the Node index to go to if the comparison matches
         * @param qfalse the Node index to go to upon no match
         */
        public ComparisonNode(String feature, Object value,
                String comparisonType, int qtrue, int qfalse) {
            super(feature, value, qtrue, qfalse);
            if (!comparisonType.equals(LESS_THAN)
                    && !comparisonType.equals(EQUALS)
                    && !comparisonType.equals(GREATER_THAN)) {
                throw new Error("Invalid comparison type: " + comparisonType);
            } else {
                this.comparisonType = comparisonType;
            }
        }

        /**
         * Compare the given value and return the appropriate Node index.
         * IMPLEMENTATION NOTE: LESS_THAN and GREATER_THAN, the Node's value
         * and the value passed in are converted to floating point values. For
         * EQUAL, the Node's value and the value passed in are treated as
         * String compares. This is the way of Flite, so be it Flite.
         *
         * @param val the value to compare
         */
        public int getNextNode(Object val) {
            boolean yes = false;
            int ret;

            if (comparisonType.equals(LESS_THAN)
                    || comparisonType.equals(GREATER_THAN)) {
                float cart_fval;
                float fval;
                if (value instanceof Float) {
                    cart_fval = ((Float) value).floatValue();
                } else {
                    cart_fval = Float.parseFloat(value.toString());
                }
                if (val instanceof Float) {
                    fval = ((Float) val).floatValue();
                } else {
                    fval = Float.parseFloat(val.toString());
                }
                if (comparisonType.equals(LESS_THAN)) {
                    yes = (fval < cart_fval);
                } else {
                    yes = (fval > cart_fval);
                }
            } else { // comparisonType = "="
                String sval = val.toString();
                String cart_sval = value.toString();
                yes = sval.equals(cart_sval);
            }
            if (yes) {
                ret = qtrue;
            } else {
                ret = qfalse;
            }
            logger.fine(trace(val, yes, ret));
            return ret;
        }

        private String trace(Object value, boolean match, int next) {
            return "NODE " + getFeature() + " [" + value + "] "
                    + comparisonType + " [" + getValue() + "] "
                    + (match ? "Yes" : "No") + " next " + next;
        }

        /**
         * Get a string representation of this Node.
         */
        public String toString() {
            return "NODE " + getFeature() + " " + comparisonType + " "
                    + getValueString() + " " + Integer.toString(qtrue) + " "
                    + Integer.toString(qfalse);
        }
    }

    /**
     * A Node that checks for a regular expression match.
     */
    static class MatchingNode extends DecisionNode {
        Pattern pattern;

        /**
         * Create a new MatchingNode with the given values.
         *
         * @param feature the string used to get a value from an Item
         * @param regex the regular expression
         * @param qtrue the Node index to go to if the comparison matches
         * @param qfalse the Node index to go to upon no match
         */
        public MatchingNode(String feature, String regex, int qtrue, int qfalse) {
            super(feature, regex, qtrue, qfalse);
            this.pattern = Pattern.compile(regex);
        }

        /**
         * Compare the given value and return the appropriate CART index.
         *
         * @param val the value to compare -- this must be a String
         */
        public int getNextNode(Object val) {
            return pattern.matcher((String) val).matches() ? qtrue : qfalse;
        }

        /**
         * Get a string representation of this Node.
         */
        public String toString() {
            StringBuffer buf =
                    new StringBuffer(NODE + " " + getFeature() + " "
                            + OPERAND_MATCHES);
            buf.append(getValueString() + " ");
            buf.append(Integer.toString(qtrue) + " ");
            buf.append(Integer.toString(qfalse));
            return buf.toString();
        }
    }

    /**
     * The final Node of a CART. This just a marker class.
     */
    static class LeafNode extends Node {
        /**
         * Create a new LeafNode with the given value.
         *
         * @param the value of this LeafNode
         */
        public LeafNode(Object value) {
            super(value);
        }

        /**
         * Get a string representation of this Node.
         */
        public String toString() {
            return "LEAF " + getValueString();
        }
    }
}
