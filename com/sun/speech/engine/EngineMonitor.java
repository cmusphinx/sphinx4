/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import java.util.Date;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JLabel;

import javax.speech.Engine;
import javax.speech.EngineListener;
import javax.speech.EngineEvent;
import javax.speech.EngineErrorEvent;

/**
 * Simple GUI for monitoring events and state changes of an
 * <code>Engine</code>.  Used for debugging and testing purposes.
 */
public class EngineMonitor {
    /**
     * The <code>Engine</code> to monitor.
     */
    protected Engine engine;

    /**
     * The <code>EngineListener</code> registered with the engine.
     */
    protected EngineListener engineListener;

    /**
     * The panel used to post engine events.
     */
    protected EngineEventPanel eventPanel;

    /**
     * The panel containing the current engine states.
     */
    protected JPanel statePanel;

    /**
     * The label containing the string "deallocated".
     */
    protected JLabel deallocatedLabel;

    /**
     * The label containing the string "allocating resources".
     */
    protected JLabel allocatingResourcesLabel;

    /**
     * The label containing the string "allocated".
     */
    protected JLabel allocatedLabel;

    /**
     * The label containing the string "deallocating resources".
     */
    protected JLabel deallocatingResourcesLabel;

    /**
     * The label containing the string "paused".
     */
    protected JLabel pausedLabel;

    /**
     * The label containing the string "resumed".
     */
    protected JLabel resumedLabel;

    /**
     * Class constructor.
     *
     * @param eng the <code>Engine</code> to watch
     */
    public EngineMonitor(Engine eng) {
        this.engine = eng;
        engine.addEngineListener(getEngineListener());
    }

    /**
     * Creates the engine listener if necessary, and then returns it.
     * There should be only one.
     *
     * @return the engine listener
     */
    protected EngineListener getEngineListener() {
        if (engineListener == null) {
            engineListener = new EngineMonitorEngineListener();
        }
        return engineListener;
    }

    /**
     * Gets the panel containing the area to post engine events in.
     *
     * @return the panel containing the area to post engine events in
     */
    public Component getEventPanel() {
        if (eventPanel == null) {
            eventPanel = new EngineEventPanel();
        }
        return eventPanel;
    }

    /**
     * Gets the panel containing the labels for representing the
     * current engine state.
     *
     * @return the panel containing the labels for representing the
     *   current engine state.
     */
    public Component getStatePanel() {
        if (statePanel == null) {
	    JPanel newStatePanel = new JPanel();
            newStatePanel.setLayout(new GridLayout(1,2));
            
            JPanel engineStatePanel = new JPanel();
            engineStatePanel.setLayout(new GridLayout(4,2));
            engineStatePanel.setBorder(
                BorderFactory.createTitledBorder("Engine State:"));
            
            deallocatedLabel = new JLabel("DEALLOCATED");
            allocatingResourcesLabel = new JLabel("ALLOCATING_RESOURCES");
            allocatedLabel = new JLabel("ALLOCATED");
            deallocatingResourcesLabel = new JLabel("DEALLOCATING_RESOURCES");
            pausedLabel = new JLabel("PAUSED");
            resumedLabel = new JLabel("RESUMED");
            
            engineStatePanel.add(deallocatedLabel);
            engineStatePanel.add(pausedLabel);
            engineStatePanel.add(allocatedLabel);
            engineStatePanel.add(resumedLabel);
            engineStatePanel.add(deallocatingResourcesLabel);
            engineStatePanel.add(new JLabel(""));
            engineStatePanel.add(allocatingResourcesLabel);
            newStatePanel.add(engineStatePanel);
	    statePanel = newStatePanel;
        }
        return statePanel;
    }

    /**
     * Handles an event from the engine.
     *
     * @param e the event from the engine
     */
    protected void handleEvent(EngineEvent e) {
        if (eventPanel != null) {
            eventPanel.addText(new Date().toString() + ": "
                               + e.toString()
                               + "\n");
            eventPanel.addText("   Old state: "
                               + engineStateString(e.getOldEngineState())
                               + "\n");
            eventPanel.addText("   New state: "
                               + engineStateString(e.getNewEngineState())
                               + "\n");
        }
        updateGUIComponents();
    }

    /**
     * Checks the current state of the engine and makes sure the GUI
     * components reflect this state accurately.
     */
    protected void updateGUIComponents() {
        updateEngineStateComponents();
    }
    
    /**
     * Checks the current state of the engine and makes sure the GUI
     * components reflect this state accurately.
     */
    protected void updateEngineStateComponents() {
        if (statePanel != null) {
            deallocatedLabel.setEnabled(
                engine.testEngineState(Engine.DEALLOCATED));
            allocatingResourcesLabel.setEnabled(
                engine.testEngineState(Engine.ALLOCATING_RESOURCES));
            allocatedLabel.setEnabled(
                engine.testEngineState(Engine.ALLOCATED));
            deallocatingResourcesLabel.setEnabled(
                engine.testEngineState(Engine.DEALLOCATING_RESOURCES));
            pausedLabel.setEnabled(
                engine.testEngineState(Engine.PAUSED));
            resumedLabel.setEnabled(
                engine.testEngineState(Engine.RESUMED));
        }
    }
    
    /**
     * Returns a <code>String</code> representing the
     * <code>state</code>.
     *
     * @param state the state to turn into a <code>String</code>
     *
     * @return  a <code>String</code> representing the
     *   <code>state</code>
     */
    protected String engineStateString(long state) {
        StringBuffer buf = new StringBuffer();

        if ((state & Engine.DEALLOCATED) != 0)
            appendBuffer(buf, "DEALLOCATED");
        if ((state & Engine.ALLOCATING_RESOURCES) != 0)
            appendBuffer(buf, "ALLOCATING_RESOURCES");
        if ((state & Engine.ALLOCATED) != 0)
            appendBuffer(buf, "ALLOCATED");
        if ((state & Engine.DEALLOCATING_RESOURCES) != 0)
            appendBuffer(buf, "DEALLOCATING_RESOURCES");
        
        if ((state & Engine.PAUSED) != 0)
            appendBuffer(buf, "PAUSED");
        if ((state & Engine.RESUMED) != 0)
            appendBuffer(buf, "RESUMED");
        
        return buf.toString();
    }

    /**
     * Adds a <code>String</code> to a buffer, with each
     * <code>String</code> being separated by a ":".
     *
     * @param b the buffer to which to append <code>s</code to
     * @param s the <code>String</code> to append to <code>b</code>
     */
    protected void appendBuffer(StringBuffer b, String s) {
        if (b.length() > 0)
            b.append(":");
        b.append(s);
    }
    
    /**
     * Handles engine events from the engine.
     */
    protected class EngineMonitorEngineListener implements EngineListener {
        public EngineMonitorEngineListener() {
        }
        public void enginePaused(EngineEvent e) {
            handleEvent(e);
        }
        public void engineResumed(EngineEvent e)  {
            handleEvent(e);
        }
        public void engineAllocated(EngineEvent e) {
            handleEvent(e);
        }
        public void engineDeallocated(EngineEvent e) {
            handleEvent(e);
        }
        public void engineAllocatingResources(EngineEvent e) {
            handleEvent(e);
        }
        public void engineDeallocatingResources(EngineEvent e) {
            handleEvent(e);
        }
        public void engineError(EngineErrorEvent e) {
            handleEvent(e);
        }
    }
}
