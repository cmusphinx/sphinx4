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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface that Manages a feature or item path. Allows navigation to the
 * corresponding feature or item. This class in controlled by the following
 * system properties:
 *
 * <pre>
 *   com.sun.speech.freetts.interpretCartPaths - default false
 *   com.sun.speech.freetts.lazyCartCompile - default true
 * </pre>
 *
 * com.sun.speech.freetts.interpretCartPaths
 *
 * Instances of this class will optionally pre-compile the paths. Pre-compiling
 * paths reduces the processing time and objects needed to extract a feature or
 * an item based upon a path.
 */
public class PathExtractor {
    /** Logger instance. */
    private static final Logger LOGGER = Logger
            .getLogger(PathExtractor.class.getName());

    /**
     * If this system property is set to true, paths will not be compiled.
     */
    public final static String INTERPRET_PATHS_PROPERTY =
            "com.sun.speech.freetts.interpretCartPaths";

    /**
     * If this system property is set to true, CART feature/item paths will
     * only be compiled as needed.
     */
    public final static String LAZY_COMPILE_PROPERTY =
            "com.sun.speech.freetts.lazyCartCompile";

    private final static boolean INTERPRET_PATHS = System.getProperty(
            INTERPRET_PATHS_PROPERTY, "false").equals("true");
    private final static boolean LAZY_COMPILE = System.getProperty(
            LAZY_COMPILE_PROPERTY, "true").equals("true");

    private String pathAndFeature;
    private String path;
    private String feature;
    private Object[] compiledPath;

    /**
     * Creates a path for the given feature.
     * @param pathAndFeature string to use
     * @param wantFeature do we need features
     */
    public PathExtractor(String pathAndFeature, boolean wantFeature) {
        this.pathAndFeature = pathAndFeature;
        if (INTERPRET_PATHS) {
            path = pathAndFeature;
            return;
        }

        if (wantFeature) {
            int lastDot = pathAndFeature.lastIndexOf(".");
            // string can be of the form "p.feature" or just "feature"

            if (lastDot == -1) {
                feature = pathAndFeature;
                path = null;
            } else {
                feature = pathAndFeature.substring(lastDot + 1);
                path = pathAndFeature.substring(0, lastDot);
            }
        } else {
            this.path = pathAndFeature;
        }

        if (!LAZY_COMPILE) {
            compiledPath = compile(path);
        }
    }

    /**
     * Finds the item associated with this Path.
     *
     * @param item the item to start at
     * @return the item associated with the path or null
     */
    public Item findItem(Item item) {

        if (INTERPRET_PATHS) {
            return item.findItem(path);
        }

        if (compiledPath == null) {
            compiledPath = compile(path);
        }

        Item pitem = item;

        for (int i = 0; pitem != null && i < compiledPath.length;) {
            OpEnum op = (OpEnum) compiledPath[i++];
            if (op == OpEnum.NEXT) {
                pitem = pitem.getNext();
            } else if (op == OpEnum.PREV) {
                pitem = pitem.getPrevious();
            } else if (op == OpEnum.NEXT_NEXT) {
                pitem = pitem.getNext();
                if (pitem != null) {
                    pitem = pitem.getNext();
                }
            } else if (op == OpEnum.PREV_PREV) {
                pitem = pitem.getPrevious();
                if (pitem != null) {
                    pitem = pitem.getPrevious();
                }
            } else if (op == OpEnum.PARENT) {
                pitem = pitem.getParent();
            } else if (op == OpEnum.DAUGHTER) {
                pitem = pitem.getDaughter();
            } else if (op == OpEnum.LAST_DAUGHTER) {
                pitem = pitem.getLastDaughter();
            } else if (op == OpEnum.RELATION) {
                String relationName = (String) compiledPath[i++];
                pitem =
                        pitem.getSharedContents()
                                .getItemRelation(relationName);
            } else {
                System.out.println("findItem: bad feature " + op + " in "
                        + path);
            }
        }
        return pitem;
    }

    /**
     * Finds the feature associated with this Path.
     *
     * @param item the item to start at
     * @return the feature associated or "0" if the feature was not found.
     */
    public Object findFeature(Item item) {

        if (INTERPRET_PATHS) {
            return item.findFeature(path);
        }

        Item pitem = findItem(item);
        Object results = null;
        if (pitem != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("findFeature: Item [" + pitem + "], feature '"
                        + feature + "'");
            }
            results = pitem.getFeatures().getObject(feature);
        }

        results = (results == null) ? "0" : results;
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("findFeature: ...results = '" + results + "'");
        }
        return results;
    }

    /**
     * Compiles the given path into the compiled form
     *
     * @param path the path to compile
     * @return the compiled form which is in the form of an array path
     *         traversal enums and associated strings
     */
    private Object[] compile(String path) {
        if (path == null) {
            return new Object[0];
        }

        List<Object> list = new ArrayList<Object>();
        StringTokenizer tok = new StringTokenizer(path, ":.");

        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            OpEnum op = OpEnum.getInstance(token);
            if (op == null) {
                throw new Error("Bad path compiled " + path);
            }

            list.add(op);

            if (op == OpEnum.RELATION) {
                list.add(tok.nextToken());
            }
        }
        return list.toArray();
    }

    // inherited for Object

    public String toString() {
        return pathAndFeature;
    }

    // TODO: add these to the interface should we support binary
    // files
    /*
     * public void writeBinary(); public void readBinary();
     */
}


/**
 * An enumerated type associated with path operations.
 */
class OpEnum {
    static private Map<String, OpEnum> map = new HashMap<String, OpEnum>();

    public final static OpEnum NEXT = new OpEnum("n");
    public final static OpEnum PREV = new OpEnum("p");
    public final static OpEnum NEXT_NEXT = new OpEnum("nn");
    public final static OpEnum PREV_PREV = new OpEnum("pp");
    public final static OpEnum PARENT = new OpEnum("parent");
    public final static OpEnum DAUGHTER = new OpEnum("daughter");
    public final static OpEnum LAST_DAUGHTER = new OpEnum("daughtern");
    public final static OpEnum RELATION = new OpEnum("R");

    private String name;

    /**
     * Creates a new OpEnum.. There is a limited set of OpEnums
     *
     * @param name the path name for this Enum
     */
    private OpEnum(String name) {
        this.name = name;
        map.put(name, this);
    }

    /**
     * gets an OpEnum thats associated with the given name.
     *
     * @param name the name of the OpEnum of interest
     */
    public static OpEnum getInstance(String name) {
        return (OpEnum) map.get(name);
    }

    // inherited from Object
    public String toString() {
        return name;
    }
}
