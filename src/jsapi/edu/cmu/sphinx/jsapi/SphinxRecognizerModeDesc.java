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

import java.util.Locale;

import javax.speech.Engine;
import javax.speech.EngineCreate;
import javax.speech.EngineException;
import javax.speech.EngineModeDesc;
import javax.speech.recognition.RecognizerModeDesc;

/**
 * Provides information about a specific operating mode of a sphinx recognition
 * engine.
 * <p>
 * This class creates a bridge between the sphinx configuration in an
 * XML configuration file and the configuration settings needed for
 * the Java Speech API. 
 * </p>
 */
public class SphinxRecognizerModeDesc extends RecognizerModeDesc
        implements EngineCreate {

    /** Name of the sphinx configuration file if any. */
    private final String configFile;

    /**
     * Constructs a new object. 
     */
    public SphinxRecognizerModeDesc() {
        this(null);
    }

    /**
     * Constructs a new object.
     * @param config name of the configuration file as a resource, maybe
     * 	             <code>null</code>. In that case the default
     *               configuration at <code>/sphinx4.config.xml</code>
     *               is expected. 
     */
    public SphinxRecognizerModeDesc(String config) {
        super("Sphinx 4",      // engine name
              null,            // mode name
              Locale.US,
              Boolean.FALSE,   // running?
              Boolean.TRUE,    // dictationGrammarSupported
              null);           // profile[]
        if (config == null) {
            configFile = "/sphinx4.config.xml";
        } else {
            configFile = config;
        }
    }

    /**
     * Retrieves the configuration file.
     * @return the name of the configuration file,
     *          <code>/sphinx4.config.xml</code> if none specified
     */
    public String getConfigFile() {
        return configFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(EngineModeDesc descriptor) {
        if (descriptor instanceof SphinxRecognizerModeDesc) {
            SphinxRecognizerModeDesc other = (SphinxRecognizerModeDesc) descriptor;
            if (!configFile.equals(other.getConfigFile())) {
                return false;
            }
        }
        return super.match(descriptor);
    }

    /**
     * Creates an engine with the properties specified by this
     * SphinxRecognizerModeDesc
     *
     * @return a SphinxRecognizer
     * @throws IllegalArgumentException if the properties of the SphinxRecognizerModeDesc do not refer to a known engine
     *                                  or engine mode.
     * @throws EngineException          if the engine could not be properly created.
     * @throws SecurityException        if the caller does not have permission to create an engine.
     */
    public Engine createEngine()
            throws IllegalArgumentException, EngineException, SecurityException {
        return new SphinxRecognizer(this);
    }
}
