/* Copyright 1999,2004 Sun Microsystems, Inc.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.tools.tags;

import java.util.StringTokenizer;

import org.mozilla.javascript.Scriptable;

/**  
 * An ObjectTagsParser is an ActionTagsParser that handles registration of
 * Java object instances.  With this class, an application can use the put
 * method to register a Java object instance with the parser.  Once this has
 * been done, ECMAScript action tags can reference and manipulate that
 * object by name.
 *
 * @see #put
 */
public class ObjectTagsParser extends ActionTagsParser {

    /**
     * Create a new ObjectParser.
     */
    public ObjectTagsParser() {
    }

    /**
     * Put the given object with the given name in the global
     * namespace of the parser.  If the name already exists in
     * the global namespace, it is replaced with the new object.
     *
     * @param name the name of the object
     * @param object the object
     */
    public void put(String name, Object object) {
        put(global, name, object);
    }

    /**
     * Put the given object with the given name in the given scope.
     * If the name already exists, it is replaced with the new object.
     *
     * @param scope the scope
     * @param name the name of the object
     * @param object the object
     */
    protected void put(Scriptable scope, String name, Object object) {
        scope.put(name, scope, object);
    }
}
