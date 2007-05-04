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
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.util.props.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    List allocatedMonitors;
    List deallocatedMonitors;
    String name;


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getConfigurationInfo()
    */
    public static Map getConfigurationInfo() {
        Map info = new HashMap();

        info.put(new String("PROP_RECOGNIZER_TYPE"), new String("COMPONENT"));
        info.put(new String("PROP_RECOGNIZER_CLASSTYPE"), new String("edu.cmu.sphinx.recognizer.Recognizer"));
        info.put(new String("PROP_ALLOCATED_MONITORS_TYPE"), new String("COMPONENT_LIST"));
        info.put(new String("PROP_ALLOCATED_MONITORS_CLASSTYPE"), new String("edu.cmu.sphinx.instrumentation.Monitor"));
        info.put(new String("PROP_DEALLOCATED_MONITORS_TYPE"), new String("COMPONENT_LIST"));
        info.put(new String("PROP_DEALLOCATED_MONITORS_CLASSTYPE"), new String("edu.cmu.sphinx.instrumentation.Monitor"));
        return info;
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
    *      edu.cmu.sphinx.util.props.Registry)
    */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_ALLOCATED_MONITORS, PropertyType.COMPONENT_LIST);
        registry.register(PROP_DEALLOCATED_MONITORS,
                PropertyType.COMPONENT_LIST);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        Recognizer newRecognizer = (Recognizer) ps.getComponent(
                PROP_RECOGNIZER);
        if (recognizer == null) {
            recognizer = newRecognizer;
            recognizer.addStateListener(this);
        } else if (recognizer != newRecognizer) {
            recognizer.removeStateListener(this);
            recognizer = newRecognizer;
            recognizer.addStateListener(this);
        }
        allocatedMonitors = ps.getComponentList(PROP_ALLOCATED_MONITORS
        );
        deallocatedMonitors = ps.getComponentList(PROP_DEALLOCATED_MONITORS
        );
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#getName()
    */
    public String getName() {
        return name;
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.recognizer.StateListener#statusChanged(edu.cmu.sphinx.recognizer.RecognizerState)
    */
    public void statusChanged(RecognizerState status) {
        List runnableList = null;
        if (status == RecognizerState.ALLOCATED) {
            runnableList = allocatedMonitors;
        } else if (status == RecognizerState.DEALLOCATED) {
            runnableList = deallocatedMonitors;
        }
        if (runnableList != null) {
            for (Iterator i = runnableList.iterator(); i.hasNext();) {
                Runnable r = (Runnable) i.next();
                r.run();
            }
        }
    }
}
