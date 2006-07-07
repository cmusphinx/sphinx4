/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import javax.speech.SpeechEvent;
import javax.speech.EngineProperties;
import javax.speech.SpeechError;

import java.util.EventObject;
import java.util.Enumeration;
import java.util.Vector;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Supports the JSAPI 1.0 <code>EngineProperties</code>
 * interface.
 */
public abstract class BaseEngineProperties
    implements EngineProperties, SpeechEventDispatcher {
    
    /**
     * List of <code>PropertyChangeListeners</code> registered for
     * <code>PropertyChangeEvents</code> on this object.
     */
    protected Vector propertyChangeListeners;

    /**
     * Class constructor.
     */
    protected BaseEngineProperties() {
        propertyChangeListeners = new Vector();
    }

    /**
     * Obtains the AWT <code>Component</code> that provides the
     * default user interface 
     * for setting the properties of the <code>Engine</code>
     * associated with this object.
     *
     * @return an AWT <code>Component</code> to manipulate this object
     */
    public java.awt.Component getControlComponent() {
        return null;
    }

    /**
     * Returns all properties to reasonable defaults
     * for the <code>Engine</code>.  A
     * <code>PropertyChangeEvent</code> is issued
     * for each property that changes as the reset takes effect.
     */
    public abstract void reset();
    
    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     *
     * @param listener the <code>PropertyChangeListener</code> to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (!propertyChangeListeners.contains(listener)) {
            propertyChangeListeners.addElement(listener);
        }
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener
     * list.
     * 
     * @param listener the <code>PropertyChangeListener</code> to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.removeElement(listener);
    }

    /**
     * Generates a
     * <code>PropertyChangeEvent</code> for an <code>Object</code> value
     * and posts it to the event queue.  Eventually
     * <code>firePropertyChangeEvent</code> will be called by
     * <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param propName the name of the property
     * @param oldValue the old value
     * @param newValue the new value
     *
     * @see #firePropertyChangeEvent
     * @see #dispatchSpeechEvent
     */
    protected void postPropertyChangeEvent(String propName,
                                           Object oldValue,
                                           Object newValue) {
        EventObject e = new PropertyChangeEvent(this,
                                                propName,
                                                oldValue,
                                                newValue);
        SpeechEvent se = new SpeechEventWrapper(e);
        SpeechEventUtilities.postSpeechEvent(this, se);
    }

    /**
     * Generates a
     * <code>PropertyChangeEvent</code> for a <code>float</code> value
     * and posts it to the event queue.  Eventually
     * <code>firePropertyChangeEvent</code> will be called by
     * <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param propName the name of the property
     * @param oldValue the old value
     * @param newValue the new value
     *
     * @see #firePropertyChangeEvent
     * @see #dispatchSpeechEvent
     */
    protected void postPropertyChangeEvent(String propName,
                                           float oldValue,
                                           float newValue) {
        EventObject e = new PropertyChangeEvent(this,
                                                propName, 
                                                new Float(oldValue), 
                                                new Float(newValue));
        SpeechEvent se = new SpeechEventWrapper(e);
        SpeechEventUtilities.postSpeechEvent(this, se);
    }

    /**
     * Generates a
     * <code>PropertyChangeEvent</code> for a <code>int</code> value
     * and posts it to the event queue.  Eventually
     * <code>firePropertyChangeEvent</code> will be called by
     * <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param propName the name of the property
     * @param oldValue the old value
     * @param newValue the new value
     *
     * @see #firePropertyChangeEvent
     * @see #dispatchSpeechEvent
     */
    protected void postPropertyChangeEvent(String propName,
                                           int oldValue,
                                           int newValue) {
        EventObject e = new PropertyChangeEvent(this,
                                                propName, 
                                                new Integer(oldValue), 
                                                new Integer(newValue));
        SpeechEvent se = new SpeechEventWrapper(e);
        SpeechEventUtilities.postSpeechEvent(this, se);
    }

    /**
     * Generates a
     * <code>PropertyChangeEvent</code> for a <code>boolean</code> value
     * and posts it to the event queue.  Eventually
     * <code>firePropertyChangeEvent</code> will be called by
     * <code>dispatchSpeechEvent</code> as a result of this action.
     *
     * @param propName the name of the property
     * @param oldValue the old value
     * @param newValue the new value
     *
     * @see #firePropertyChangeEvent
     * @see #dispatchSpeechEvent
     */
    protected void postPropertyChangeEvent(String propName,
                                           boolean oldValue,
                                           boolean newValue) {
        EventObject e = new PropertyChangeEvent(this,
                                                propName, 
                                                new Boolean(oldValue), 
                                                new Boolean(newValue));
        SpeechEvent se = new SpeechEventWrapper(e);
        SpeechEventUtilities.postSpeechEvent(this, se);
    }
    
    /**
     * Sends a <code>PropertyChangeEvent</code>
     * to all <code>PropertyChangeListeners</code> registered with
     * this object.  Called by <code>dispatchSpeechEvent</code>.
     *
     * @param event the <code>PropertyChangeEvent</code> to send
     *
     * @see #firePropertyChangeEvent
     * @see #dispatchSpeechEvent
     */
    public void firePropertyChangeEvent(PropertyChangeEvent event) {
        if (propertyChangeListeners == null) {
            return;
        }
        Enumeration E = propertyChangeListeners.elements();
        while (E.hasMoreElements()) {
            PropertyChangeListener pl =
                (PropertyChangeListener) E.nextElement();
            pl.propertyChange(event);
        }
    }

    /**
     * Dispatches a <code>PropertyChangeEvent</code>.
     * The dispatcher should notify all <code>PropertyChangeListeners</code>
     * from this method.  The <code>SpeechEvent</code> was added
     * via the various post methods of this class.
     *
     * @param event the <code>SpeechEvent</code> containing a
     *   <code>PropertyChangeEvent</code>
     *
     * @see #postPropertyChangeEvent
     */
    public void dispatchSpeechEvent(SpeechEvent event) {
        if (event instanceof SpeechEventWrapper) {
            SpeechEventWrapper se = (SpeechEventWrapper)event;
            PropertyChangeEvent pe =
                (PropertyChangeEvent)(se.getEventObject());
            firePropertyChangeEvent(pe);
        }
        else {
            throw new SpeechError(
                "BaseEngineProperties: speech event type error");
        }
    }
}
