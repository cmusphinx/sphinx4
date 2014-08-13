/*
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.alignment;

import java.util.List;

/**
 *
 * @author Alexander Solovets
 */
public interface WordExpander {
    List<String> expand(String text);
}
