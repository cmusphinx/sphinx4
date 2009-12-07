/**
 * Copyright 1998-2009 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.jsgf.rule;

import java.util.ArrayList;
import java.util.List;

public class Rule {
	public String ruleName;

	public boolean isPublic;
	public boolean isEnabled;
	public boolean hasChanged;

	public List<String> samples = new ArrayList<String>();

	public int lineno;

	public String toString() {
		return ruleName;
	}
}
