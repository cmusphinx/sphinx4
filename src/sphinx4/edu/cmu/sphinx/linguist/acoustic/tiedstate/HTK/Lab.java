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
 * represents a label, i.e. a model name + a state number
 * 
 * @author cerisara
 *
 */
public class Lab {
	private String nomHMM;
	private int numState=-1;
	private int deb=-1, fin=-1;
	
	public Lab() {
	}
	public Lab(String s) {
		setName(s);
	}
	public Lab(String s, int n) {
		setName(s); setStateIdx(n);
	}
	// copy-constructor
	public Lab(Lab ref) {
		setDeb(ref.getDeb());
		setFin(ref.getFin());
		setName(ref.getName());
		setStateIdx(ref.getState());
	}
	
	public String getName() {
		return nomHMM;
	}
	public int getState() {
		return numState;
	}
	public int getDeb() {return deb;}
	public int getFin() {return fin;}
	public void setName(String s) {
		nomHMM = s;
	}
	public void setStateIdx(int i) {
		numState = i;
	}
	public void setDeb(int i) {
		deb=i;
	}
	public void setFin(int i) {
		fin=i;
	}
	public boolean isEqual(Lab l) {
		if (l.getState()!=-1 && getState()!=-1) {
			return l.getName().equals(getName()) && l.getState()==getState();
		} else {
			return l.getName().equals(getName());
		}
	}
	
	public String toString() {
		String r = "";
		if (deb>=0&&fin>=deb)
			r+=deb+" "+fin+ ' ';
		r+=nomHMM;
		if (numState>=0)
			r+="["+numState+ ']';
		return r;
	}
}
