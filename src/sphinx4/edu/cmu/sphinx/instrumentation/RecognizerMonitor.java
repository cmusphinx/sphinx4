/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.instrumentation;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.util.props.*;

import java.util.List;

/**
 * Monitor the state transitions of a given recognizer. This monitor maintains lists of components that should be 'run'
 * when a recognizer state change is detected.
 */
public class RecognizerMonitor implements StateListener, Monitor {

    /** the sphinx property for the recognizer to monitor */
    @S4Component(type = Recognizer.class)
    public final static String PROP_RECOGNIZER = "recognizer";

    /** The sphinx property that defines all of the monitors to call when the recognizer is allocated */
    @S4ComponentList(type = Configurable.class)
    public final static String PROP_ALLOCATED_MONITORS = "allocatedMonitors";

    /** The sphinx property that defines all of the monitors to call when the recognizer is deallocated */
    @S4ComponentList(type = Configurable.class)
    public final static String PROP_DEALLOCATED_MONITORS = "deallocatedMonitors";


    // --------------------------
    // Configuration data
    // --------------------------
    Recognizer recognizer;
    List<Runnable> allocatedMonitors;
    List<Runnable> deallocatedMonitors;
    String name;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER);
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addStateListener(this);
        }

        allocatedMonitors = (List)ps.getComponentList(PROP_ALLOCATED_MONITORS);
        deallocatedMonitors = (List)ps.getComponentList(PROP_DEALLOCATED_MONITORS);
    }

    @Override
    public void statusChanged(Recognizer.State status) {
        List<Runnable> runnableList = null;
        if (status == State.ALLOCATED) {
            runnableList = allocatedMonitors;
        } else if (status == State.DEALLOCATED) {
            runnableList = deallocatedMonitors;
        }

        if (runnableList != null) {
            for (Runnable r : runnableList) {
                r.run();
            }
        }
    }
}
