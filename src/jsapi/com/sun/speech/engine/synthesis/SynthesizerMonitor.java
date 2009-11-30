/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Enumeration;

import javax.speech.EngineEvent;
import javax.speech.EngineListener;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerEvent;
import javax.speech.synthesis.SynthesizerListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sun.speech.engine.EngineMonitor;

/**
 * Simple GUI that monitors events and state changes of an
 * <code>Synthesizer</code>.  Used for debugging and testing purposes.
 */
public class SynthesizerMonitor extends EngineMonitor {
    /**
     * Label containing "queue empty"
     */
    protected JLabel queueEmptyLabel;

    /**
     * Label containing "queue not empty"
     */
    protected JLabel queueNotEmptyLabel;

    /**
     * Label containing "queue size"
     */
    protected JLabel queueSizeLabel;

    /**
     * Class constructor.
     *
     * @param synth the <code>Synthesizer</code> to monitor
     */
    public SynthesizerMonitor(Synthesizer synth) {
        super(synth);
    }

    // Inherited javadoc.
    //
    protected EngineListener getEngineListener() {
        if (engineListener == null) {
            engineListener = new SynthesizerMonitorEngineListener();
        }
        return engineListener;
    }

    /**
     * Gets the panel containing the labels for representing the
     * current engine state.  This augments the super class's panel
     * by adding synthesizer queue state.
     *
     * @return the panel containing the labels for representing the
     *   current engine state.
     */
    public Component getStatePanel() {
        if (statePanel == null) {
            statePanel = (JPanel) super.getStatePanel();
            JPanel queueStatePanel = new JPanel();
            queueStatePanel.setBorder(
                BorderFactory.createTitledBorder("Synthesizer State:"));
            queueStatePanel.setLayout(new GridLayout(4,1));        
            queueEmptyLabel = new JLabel("QUEUE_EMPTY");
            queueNotEmptyLabel = new JLabel("QUEUE_NOT_EMPTY");
            queueSizeLabel = new JLabel("Queue Size: XXX");
            queueStatePanel.add(queueEmptyLabel);
            queueStatePanel.add(queueNotEmptyLabel);
            queueStatePanel.add(queueSizeLabel);
            statePanel.add(queueStatePanel);
        }
        return statePanel;
    }

    // Inherited javadoc.
    //
    protected void updateGUIComponents() {
        super.updateGUIComponents();
        if (statePanel != null) {
            queueEmptyLabel.setEnabled(
                engine.testEngineState(Synthesizer.QUEUE_EMPTY));
            queueNotEmptyLabel.setEnabled(
                engine.testEngineState(Synthesizer.QUEUE_NOT_EMPTY));

	    Synthesizer synth = (Synthesizer) engine;
	    int queueSize = countElements(synth.enumerateQueue());
	    queueSizeLabel.setText("Queue Size: " + queueSize + "  ");
        }
    }

    /**
     * Counts the number of elements in the enumeration.
     *
     * @param e the enumeration
     *
     * @return the number of elements in the enumeration
     */
    private int countElements(Enumeration e) {
	int count = 0;
	while (e.hasMoreElements()) {
	    e.nextElement();
	    count++;
	}
	return count;
    }
    
    // Inherited javadoc.
    //
    protected String engineStateString(long state) {
        StringBuffer buf = new StringBuffer();

        appendBuffer(buf,super.engineStateString(state));
        
        if ((state & Synthesizer.QUEUE_EMPTY) != 0)
            appendBuffer(buf, "QUEUE_EMPTY");
        if ((state & Synthesizer.QUEUE_NOT_EMPTY) != 0)
            appendBuffer(buf, "QUEUE_NOT_EMPTY");
        
        return buf.toString();
    }

    // Inherited javadoc.
    //
    protected void handleEvent(EngineEvent e) {
        super.handleEvent(e);
    }
    
    /**
     * Handles engine events from the engine.  Extended to include
     * <code>SynthesizerEvents</code>.
     */
    class SynthesizerMonitorEngineListener
        extends EngineMonitorEngineListener implements SynthesizerListener {
        public SynthesizerMonitorEngineListener() {
        }
        public void queueEmptied(SynthesizerEvent e) {
            handleEvent(e);
        }
        public void queueUpdated(SynthesizerEvent e) {
            handleEvent(e);
        }
    }
}
