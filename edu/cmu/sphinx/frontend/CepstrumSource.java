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


package edu.cmu.sphinx.frontend;

import java.io.IOException;


/**
 * A CepstrumSource produces Cepstrum(a).
 */
public interface CepstrumSource extends DataSource {

    /**
     * Returns the next Cepstrum object produced by this CepstrumSource.
     * The Cepstra objects of an Utterance should be preceded by
     * a Cepstrum object with Signal.UTTERANCE_START and ended by
     * a Cepstrum object with Signal.UTTERANCE_END.
     *
     * @return the next available Cepstrum object, returns null if no
     *     Cepstrum object is available
     *
     * @throws java.io.IOException
     */
    public Cepstrum getCepstrum() throws IOException;
}
