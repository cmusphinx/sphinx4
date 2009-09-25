/*
 * Copyright 2007 LORIA, France.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.linguist.acoustic.tiedstate.HTK;

/**
 * This is simply a GMMDiag with a label (=an HMM name (String) and a state number) !
 * 
 * @author cerisara
 *
 */
public class HMMState {
	public int gmmidx=-1;
	
	public float getLogLike() {
		return gmm.getLogLike();
	}
	public HMMState(GMMDiag g, Lab l) {
		lab = l;
		gmm = g;
	}
	public void setLab(Lab l) {
		lab = l;
	}
	public Lab getLab() {
		return lab;
	}
	public Lab lab;
	public GMMDiag gmm;
}
