/*
 * Copyright 1999-2012 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.api;

import java.util.ArrayList;
import javax.sound.sampled.AudioInputStream;

import edu.cmu.sphinx.result.WordResult;

public interface Aligner {
	
	public ArrayList<WordResult> align(AudioInputStream audio, String text);

}
