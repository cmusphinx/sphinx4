/*
 * 
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
package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.util.props.Configurable;

/**
 * Creates new active lists.
 */
public interface ActiveListFactory  extends Configurable {
    /**
     * property that sets the desired (or target) size for this
     * active list.  This is sometimes referred to as the beam size
     */
    public final static String PROP_ABSOLUTE_BEAM_WIDTH  ="absoluteBeamWidth";

    /**
     * The default value for the PROP_ABSOLUTE_BEAM_WIDTH property
     */
    public final static int PROP_ABSOLUTE_BEAM_WIDTH_DEFAULT = 2000;

    /**
     * Property that sets the minimum score relative to the maximum
     * score in the list for pruning.  Tokens with a score less than
     * relativeBeamWidth * maximumScore will be pruned from the list
     */

    public final static String PROP_RELATIVE_BEAM_WIDTH = "relativeBeamWidth";

    /**
     * The default value for the PROP_RELATIVE_BEAM_WIDTH property
     */
    public final static double PROP_RELATIVE_BEAM_WIDTH_DEFAULT = 0.0;

    /**
     * Property that indicates whether or not the active list will
     * implement 'strict pruning'.  When strict pruning is enabled,
     * the active list will not remove tokens from the active list
     * until they have been completely scored.  If strict pruning is
     * not enabled, tokens can be removed from the active list based
     * upon their entry scores. The default setting is false
     * (disabled).
     */

    public final static String PROP_STRICT_PRUNING = "strictPruning";

    /**
     * The default for the PROP_STRICT_PRUNING property
     */
    public final static boolean PROP_STRICT_PRUNING_DEFAULT = true;
    
    /**
     * Creates a new active list of a particular type
     * 
     * @return the active list
     */
    ActiveList newInstance();
}
