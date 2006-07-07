/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine;

import java.awt.BorderLayout;
import java.awt.Point;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;

/**
 * Simple GUI for monitoring events of an <code>Engine</code>.  Used
 * for debugging and testing purposes.
 */
public class EngineEventPanel extends JPanel {
    /**
     * The area where engine events are posted.
     */
    protected JTextArea textArea;

    /**
     * The scroll pane containing the <code>textArea</code>.
     *
     * @see #textArea
     */
    protected JScrollPane scroller;

    /**
     * The button for clearing the <code>textArea</code>.
     *
     * @see #textArea
     */
    protected JButton clearButton;

    /**
     * Class constructor.
     */
    public EngineEventPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Events:"));
        
	clearButton = new JButton("Clear");
	clearButton.setMnemonic('C');
	clearButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent evt) {
                clearText();
	    }
	});
        
        textArea = new JTextArea();
        scroller = new JScrollPane(textArea);
        
        add(scroller,BorderLayout.CENTER);
        add(clearButton,BorderLayout.SOUTH);
    }

    /**
     * Clears the text in the text area.
     */
    public void clearText() {
        textArea.setText("");
    }

    /**
     * Sets the text in the text area.
     *
     * @param s the new text
     */
    public void setText(String s) {
        textArea.setText(s);
    }

    /**
     * Appends text to the text area and scrolls the text area so the
     * new text is visible.
     *
     * @param s the text to append
     */
    public void addText(String s) {
        textArea.append(s);
        Point pt = new Point(0, textArea.getHeight() - 1);
        scroller.getViewport().setViewPosition(pt);        
    }
}
