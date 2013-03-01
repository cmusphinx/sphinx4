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
 * Provides a list of SphinxRecognizerModeDesc objects that define the available operating modes of the sphinx
 * recognition engine.  An instance of this SphinxEngineCentral object is registered with the javax.speeech.Central
 * class.  When requested by the Central class, this object provides a list of EngineModeDesc objects that describes the
 * available operating modes of the engine.
 * <p>
 * By default the list of engines returned by {@link #createEngineList(EngineModeDesc)}
 * contains only one element of {@link SphinxRecognizerModeDesc}. The default
 * implementation works for english. Other engine description may be added by
 * {@link #registerEngineModeDesc(SphinxRecognizerModeDesc)}. This allows
 * for creating other engines with different sphinx settings.
 * </p>
 */
public class SphinxEngineCentral implements EngineCentral {

    private static final SphinxRecognizerModeDesc DEFAULT_MODE_DESCRIPTOR
            = new SphinxRecognizerModeDesc();

    private static final EngineList REGISTERED_MODE_DESCS;

    static {
        REGISTERED_MODE_DESCS = new EngineList();
        REGISTERED_MODE_DESCS.add(DEFAULT_MODE_DESCRIPTOR);
    }

    /**
     * Registers the given engine mode descriptor to the list of supported
     * engine mode descriptors by this engine central
     * @param desc the descriptor to register.
     */
    @SuppressWarnings("unchecked")
    public static void registerEngineModeDesc(SphinxRecognizerModeDesc desc) {
        REGISTERED_MODE_DESCS.add(desc);
    }

    /**
     * Create an EngineList containing and EngineModeDesc object for each mode of operation of the Sphinx speech engine.
     *
     * @param require describes the constraints to be placed on engines placed on the engine list. null matches all
     *                engines. Note that require is guaranteed to be of type RecognizerModeDesc.
     * @return a list of RecognizerModeDesc objects describing the available sphinx recognition engines that match
     *         <code> require </code>. Returns <code> null </code> if no engines match.
     */
    @SuppressWarnings("unchecked")
    public EngineList createEngineList(EngineModeDesc require) {
        EngineList el = new EngineList();
        for (Object item : REGISTERED_MODE_DESCS) {
            SphinxRecognizerModeDesc desc = (SphinxRecognizerModeDesc) item;
            if (require == null || desc.match(require)) {
                el.addElement(DEFAULT_MODE_DESCRIPTOR);
                return el;
            }
        }
        if (el.isEmpty()) {
            return null;
        }
        return el;
    }
}
