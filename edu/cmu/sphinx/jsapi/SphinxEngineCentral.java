/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.jsapi;

import javax.speech.EngineCentral;
import javax.speech.EngineList;
import javax.speech.EngineModeDesc;


/**
 * Provides a list of SphinxRecognizerModeDesc objects that define the
 * available operating modes of the sphinx recognition engine.  An
 * instance of this SphinxEngineCentral object is registered with the
 * javax.speeech.Central class.  When requested by the Central class,
 * this object provides a list of EngineModeDesc objects that
 * describes the available operating modes of the engine.
 */
public class SphinxEngineCentral implements EngineCentral {

    static private SphinxRecognizerModeDesc sphinxModeDesc 
		= new SphinxRecognizerModeDesc();

    /**
     * Create an EngineList containin and EngineModeDesc object for
     * each mode of operation of the Sphinx speech engine. 
     *
     * @param require describes the constraints to be placed on
     * engines placed on the engine list. null matches all engines.
     * Note that require is guaranteed to be of type
     * RecognizerModeDesc.
     *
     * @return a list of RecognizerModeDesc objects describing the
     * available sphinx recognition engines that match <code> require
     * </code>. Returns <code> null </code> if no engines match.
     */
    public EngineList createEngineList(EngineModeDesc require) {
        if (require == null || sphinxModeDesc.match(require)) {
            EngineList el = new EngineList();
            el.addElement(sphinxModeDesc);
            return el;
        }
        return null;
    }
}
