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

package edu.cmu.sphinx.linguist.acoustic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 * Represents  the context for a unit
 */
public class  LeftRightContext extends Context implements Serializable {
    String stringRepresentation = null;
    Unit[] leftContext = null;
    Unit[] rightContext = null;
    private static boolean CACHING_CONTEXTS = true;
    private static Map cache;

    static {
	if (CACHING_CONTEXTS) {
	    cache = new HashMap();
	}
    }
    

    /**
     * Creates a LeftRightContext
     *
     * @param leftContext the left context or null if no left context
     * @param rightContext the right context or null if no right context
     */
    private LeftRightContext(Unit[] leftContext, Unit[] rightContext) {
        this.leftContext = leftContext;
        this.rightContext = rightContext;
    }


    /**
     * Provides a string representation of a context
     */
    public String toString() {
	if (stringRepresentation == null) {
	    stringRepresentation = getContextName(leftContext) + "," +
				   getContextName(rightContext);
	}
	return stringRepresentation;
    }


    /**
     * Factory method for creating a left/right context
     *
     * @param leftContext the left context or null if no left context
     * @param rightContext the right context or null if no right context
     *
     * @return a left right context
     */
    public static LeftRightContext get(Unit[] leftContext,Unit[] rightContext) {
	LeftRightContext context = null;
	if (CACHING_CONTEXTS) {
	    String name = getContextName(leftContext) + "," +
		getContextName(rightContext);
	    
	    context = (LeftRightContext) cache.get(name);
	    if (context == null) {
		context = new LeftRightContext(leftContext, rightContext);
		cache.put(name, context);
	    }
	} else {
	    context = new LeftRightContext(leftContext, rightContext);
	}
	return context;
    }

    /**
     * Retrieves the left context for this unit
     * 
     * @return the left context
     */
    public Unit[] getLeftContext() {
	return leftContext;
    }

    /**
     * Retrieves the right context for this unit
     * 
     * @return the right context
     */
    public Unit[] getRightContext() {
	return rightContext;
    }


    /**
     * Gets the context name for a particular array of units
     *
     * @param context the context
     *
     * @return the context name
     */
    public static String getContextName(Unit[] context) {
	StringBuffer sb = new StringBuffer();
        if (context == null) {
            sb.append("*");
        } else if (context.length == 0) {
            sb.append("(empty)");
        } else  {
            for (int i = 0; context != null && i < context.length; i++) {
                String name = null;
                if (context[i] != null) {
                    name = context[i].getName();
                }
                sb.append(name);
                if (i < context.length - 1) {
                    sb.append(".");
                }
            }
	}
	return sb.toString();
    }

    /**
     * Checks to see if there is a partial match with the given
     * context. If both contexts are LeftRightContexts then  a left or
     * right context that is null is considered a wild card and
     * matches anything, othewise the contexts must match exactly.
     * Anything matches the Context.EMPTY_CONTEXT
     *
     * @param context the context to check
     *
     * @return true if there is a partial match
     */
    public boolean isPartialMatch(Context context) {
	if (context instanceof LeftRightContext) {
	    LeftRightContext lrContext = (LeftRightContext) context;
	    Unit[] lc = lrContext.getLeftContext();
	    Unit[] rc = lrContext.getRightContext();

	    if (lc != null && leftContext != null &&
		    !Unit.isContextMatch(lc, leftContext)) {
		return false;
	    }
	    if (rc != null && rightContext != null && 
		    !Unit.isContextMatch(rc, rightContext)) {
		return false;
	    }
	    return true;
	} else if (context == Context.EMPTY_CONTEXT &&
		leftContext == null &&
		rightContext == null) {
	    return true;
	}
	return false;
    }

}
