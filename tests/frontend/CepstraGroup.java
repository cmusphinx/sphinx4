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


package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.Utterance;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Represents a group of Cepstrum.
 */
public class CepstraGroup {

    private Cepstrum[] cepstra;
    private String name;
   
    /**
     * Creates a default CepstraGroup with the given array of cepstra
     * and name.
     *
     * @param cepstra the array of cepstra
     * @param name a name for this CepstraGroup
     */
    public CepstraGroup(Cepstrum[] cepstra, String name) {
        this.cepstra = cepstra;
        this.name = name;
    }

    /**
     * Returns the array of Cepstrum in this group.
     */
    public Cepstrum[] getCepstra() {
        return cepstra;
    }

    /**
     * Returns the name of this CepstraGroup
     */
    public String getName() {
        return name;
    }
}
