
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

package demo.jsapi.cardmatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;


/**
 * The GUI class for the CardMatch demo.
 */
public class CardMatchFrame extends JFrame {

    private Dimension dimension = new Dimension(800, 700);
    private Color backgroundColor = new Color(220, 220, 220);

    private JPanel cardPanel;
    private JToggleButton speakButton;
    private JTextField resultsTextField;

    private Recorder recorder;
    
    private List cards;


    /**
     * Constructs a CardMatchFrame with the given title.
     *
     * @param title the title of the frame
     */
    public CardMatchFrame(String title, Recorder recorder, List cards) {
	super(title);
	this.recorder = recorder;
        this.cards = cards;
	setSize(dimension);
	getContentPane().add(createMainPanel(), BorderLayout.CENTER);

	// add a listener for closing this JFrame and quitting the program
        addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });
    }


    /**
     * Sets the test file TextField.
     *
     * @param testFile the test file
     */
    public void setResultTextField(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                resultsTextField.setText(result);
            }
        });
    }


    /**
     * Updates the card panel.
     */
    public void updateCardPanel() {
        cardPanel.removeAll();
        // add the check-boxes
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            cardPanel.add(card.getJToggleButton());
        }
        invalidate();
        validate();
        repaint();
    }


    /**
     * Causes the speak button to selected or deselected.
     *
     * @param selected to select or deselect the speak button
     */
    public void setSpeakButtonSelected(final boolean selected) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                speakButton.setSelected(selected);
            }
        });
    }


    /**
     * Returns a JPanel with the custom background color.
     *
     * @return a JPanel
     */
    private JPanel getJPanel(LayoutManager manager) {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
	panel.setLayout(manager);
        return panel;
    }


    /**
     * Creates a JTextArea for the Statistics text.
     *
     * @return a JTextArea for the Statistics text
     */
    private JTextArea getTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(backgroundColor);
        textArea.setAlignmentX(JTextArea.RIGHT_ALIGNMENT);
        return textArea;
    }


    /**
     * Creates the main panel that contains the top
     * card and buttons panels, and the bottom results panel.
     */
    private JPanel createMainPanel() {
	JPanel mainPanel = getJPanel(new BorderLayout());
	mainPanel.add(createCardPanel(), BorderLayout.CENTER);
	mainPanel.add(createResultsPanel(), BorderLayout.SOUTH);
	return mainPanel;
    }


    /**
     * Creates the card panel.
     */
    private JPanel createCardPanel() {
	cardPanel = getJPanel(new GridLayout(2, 3));
        assert cards.size() == 6;
        updateCardPanel();
     	return cardPanel;
    }


    /**
     * Handles the pressing of the "Speak" button.
     */
    private void speakButtonPressed() {
	if (!recorder.isRecording()) {
	    if (!recorder.startRecording()) {
		System.out.println("Error turning microphone on.");
                speakButton.setSelected(false);
	    }
	}
    }


    /**
     * Handles the pressing of the "Stop" button.
     */
    private void stopButtonPressed() {
	if (recorder.isRecording()) {
	    if (!recorder.stopRecording()) {
		System.out.println("Error turning microphone off.");
	    }
	}
    }


    /**
     * Creates the results panel.
     */
    private JPanel createResultsPanel() {
	JPanel resultsPanel = getJPanel(new BorderLayout());
	JTextArea textArea = getTextArea("You said: ");
	
       	resultsTextField = new JTextField();

	resultsPanel.add(textArea, BorderLayout.WEST);
	resultsPanel.add(resultsTextField, BorderLayout.CENTER);

	speakButton = new JToggleButton("Speak");
        speakButton.requestFocus();
	speakButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (speakButton.isSelected()) {
                    speakButtonPressed();
		} else {
                    stopButtonPressed();
                }
	    }
        });

        resultsPanel.add(speakButton, BorderLayout.EAST);

	return resultsPanel;
    }


}
