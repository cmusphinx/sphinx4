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


package tests.frontend;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.Signal;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Displays the endpoints and energy values of speech.
 * This viewer takes in a series of Cepstra, and plots their energy
 * values, as well as any Cepstra that are Signals showing start and
 * end of speech.
 */
public class CepstraViewer extends JFrame {

    private JScrollPane scrollPane;
    private CepstraPanel canvas;
    private JTextField cepstraName;

    private java.util.List cepstraGroups;     // all CepstraGroups
    private CepstraGroup current;   // the currently showing CepstraGroup
    private int numberCepstra;
   

    /**
     * Creates a default CepstraViewer with the given title.
     *
     * @param title the title of the CepstraViewer frame
     */
    public CepstraViewer(String title) {
        super(title);
        setSize(500, 300); // default size
        scrollPane = new JScrollPane(createMainPanel());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        setVisible(true);
        numberCepstra = 0;
        cepstraGroups = new LinkedList();
        current = null;
    }

    /**
     * Creates the main JPanel.
     *
     * @return the created main JPanel
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        canvas = new CepstraPanel();
        JPanel controlPanel = new JPanel();
        JPanel topPanel = new JPanel();
        cepstraName = new JTextField();
        cepstraName.setEditable(false);
        topPanel.add(cepstraName);

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (current != null) {
                    int index = cepstraGroups.indexOf(current);
                    if (index < cepstraGroups.size() - 1) {
                        // current becomes the next
                        current = (CepstraGroup) cepstraGroups.get(index + 1);
                        updateCurrent();
                    }
                }
            }
        });

        JButton previousButton = new JButton("Previous");
        previousButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (current != null) {
                    int index = cepstraGroups.indexOf(current);
                    if (0 < index) {
                        current = (CepstraGroup) cepstraGroups.get(index - 1);
                        updateCurrent();
                    }
                }
            }
        });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        controlPanel.add(previousButton);
        controlPanel.add(nextButton);
        controlPanel.add(exitButton);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(canvas, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        return mainPanel;
    }


    /**
     * Updates this Viewer with the current CepstraGroup.
     */
    private void updateCurrent() {
        canvas.setCepstra(current.getCepstra());
        cepstraName.setText(current.getName());
    }

    /**
     * Adds the given CepstraGroup to the list of CepstraGroup 
     * to be viewed.
     *
     * @param cepstrum the Cepstrum to draw
     */
    public void addCepstraGroup(CepstraGroup cepstraGroup) {
        cepstraGroups.add(cepstraGroup);
        if (cepstraGroups.size() == 1) {
            current = cepstraGroup;
            updateCurrent();
        }
    }

    /**
     * Main method of the CeptraViewer.
     */
    public static void main(String[] argv) {
        CepstraViewer viewer = new CepstraViewer("Test viewer");
        viewer.show();
    }
}
