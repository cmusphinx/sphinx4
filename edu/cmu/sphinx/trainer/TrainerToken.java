/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.trainer;


import edu.cmu.sphinx.decoder.search.*;
import edu.cmu.sphinx.frontend.*;

import java.util.Collection;

/**
 * Provides mechanisms for handling tokens in the trainer.
 */
public class TrainerToken /* extends Token */ {
    private Collection linkToParent;
    private Collection linkToChild;
    private float alpha;
    private float beta;

    private SentenceHMMNode state;
    private boolean isEmitting;
    private Feature dataVector;
    private int timeStamp;
}
