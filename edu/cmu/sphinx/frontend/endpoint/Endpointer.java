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


package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.*;


/**
 * Implements an energy-based endpointer that is based on a simple
 * two event windowing algorithm.
 *
 * This endpointer looks at the energy levels of the signal,
 * given by input Cepstrum, to determine the speech start and
 * speech end. The Signals, Signal.SPEECH_START and Signal.SPEECH_END,
 * are inserted at the speech endpoints.
 *
 * This endpointer maintains several configurable parameters:
 * <pre>
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.startLow
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.startHigh
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.endLow
 * </pre>
 *
 * The above parameters are usually set by the user.
 * <pre>
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.startWindow
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.startOffset
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.endWindow
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.endOffset
 *
 * edu.cmu.sphinx.frontend.FastEnergyEndpointer.maxDropout
 * </pre>
 * When the energy level is above <code>startHigh</code> for
 * <code>startWindow</code> number of frames, then speech has started.
 * Speech start will be <code>startOffset</code> number of frames
 * before speech went above <code>startLow</code>.
 *
 * When the energy level is below <code>endLow</code> for
 * <code>endWindow</code> number of frames, then speech has ended.
 * Speech end will be <code>endOffset</code> number of frames after
 * speech went below <code>endLow</code>.
 *
 * The <code>maxDropout</code> parameter deals with spikes before
 * speech starts. If the energy previously when above startLow, but
 * returns to below <code>startLow</code> without entering speech,
 * then it is a spike. If the spike is less than <code>maxDropout</code>
 * number of frames from speech, the spike is part of speech.
 * Otherwise, it is not part of speech.
 *
 * At the end of speech side, spikes are always consider part of
 * speech unless it is contiguously more than <code>endWindow</code>
 * frames from speech.
 */
public interface Endpointer extends CepstrumSource {

    /**
     * Constructs an FastEnergyEndpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this FastEnergyEndpointer
     * @param context the context of the SphinxProperties this
     *    FastEnergyEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this FastEnergyEndpointer
     *    gets Cepstrum from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           CepstrumSource predecessor) throws IOException;
}
