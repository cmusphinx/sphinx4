/*
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

package demo.sphinx.zipcity;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * A simple HelloDigits demo showing a simple speech application 
 * built using Sphinx-4. This application uses the Sphinx-4 endpointer,
 * which automatically segments incoming audio into utterances and silences.
 */

public class ZipCity extends JFrame {
    private boolean debug = true;
    private Color backgroundColor = new Color(0xff, 0xff, 0xff);
    private JTextField messageTextField;
    private JTextField zipField;
    private JTextField cityField;
    private JButton speakButton;

    private ZipDatabase zipDB;
    private ZipRecognizer zipRecognizer;

    /**
     * Constructs a ZipCity with the given title.
     *
     * @param title the title of this JFrame
     * @param live the Live instance that this GUI controls
     */
    public ZipCity() {
        super("ZipCity - a Sphinx-4 WebStart Demo");
        setSize(460, 435);
        setDefaultLookAndFeelDecorated(true);
        setApplicationIcon();
        getContentPane().add(createMainPanel(), BorderLayout.NORTH);
        getContentPane().add(createImagePanel(), BorderLayout.CENTER);
        getContentPane().add(createMessagePanel(), BorderLayout.SOUTH);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                zipRecognizer.shutdown();
                System.exit(0);
            }
        });

        speakButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (speakButton.isEnabled()) {
                    speakButton.setEnabled(false);
                    zipRecognizer.microphoneOn();
                    setMessage("Speak a zip code ...");
                }
            }
        });
    }

    public void go() {
        try {
        setMessage("Loading zip codes...");
            zipDB = new ZipDatabase();
            setMessage("Loading recognizer...");
            zipRecognizer = new ZipRecognizer();
            setMessage("Starting recognizer...");
            zipRecognizer.startup();
            setMessage("Ready ...");
            speakButton.setEnabled(true);
            zipRecognizer.addZipListener(new ZipListener() {
                public void notify(String zip) {
                    updateForm(zip);
                }
            });
        } catch (IOException ioe) {
            setMessage("Error: " + ioe.getMessage());
        }
    }

    private void updateForm(final String zip) {
        final ZipInfo zipInfo = zipDB.lookup(zip);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                zipField.setText(zip);
                if (zipInfo == null) {
                    cityField.setText("<Unknown>");
                    setMessage("Can't find that zip code in the database");
                } else {
                    String location = zipInfo.getCity() + ", " 
                        + zipInfo.getState();
                    cityField.setText(location);
                    setMessage("");
                }
                speakButton.setEnabled(true);
            }
        });
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = ZipCity.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Sets the application icon
     * @param path the path to the image
     */
    private void setApplicationIcon() {
        URL url = ZipCity.class.getResource("s4.jpg");
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(image);
    }


    /**
     * Prints out a debug message on System.out, if the 'debug' private
     * class variable is set to true.
     *
     * @param message the debug message to print
     */
    private void debugMessage(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    /**
     * Sets the message to be displayed at the bottom of the Frame.
     *
     * @param message message to be displayed
     */
    public void setMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messageTextField.setText(message);
            }
        });
    }


    /**
     * Enables or disables the "Speak" button.
     *
     * @param enable boolean to enable or disable
     */
    public void setSpeakButtonEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                speakButton.setEnabled(enabled);
            }
        });
    }


    /**
     * Returns a JPanel with the given layout and custom background color.
     *
     * @param layout the LayoutManager to use for the returned JPanel
     *
     * @return a JPanel
     */
    private JPanel getJPanel(LayoutManager layout) {
        JPanel panel = getJPanel();
        panel.setLayout(layout);
        return panel;
    }


    /**
     * Returns a JPanel with the custom background color.
     *
     * @return a JPanel
     */
    private JPanel getJPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
        return panel;
    }


    /**
     * Constructs the main Panel of this LiveFrame.
     *
     * @return the main Panel of this LiveFrame
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = getJPanel(new FlowLayout(FlowLayout.LEFT));
        speakButton = new JButton("Speak");
        speakButton.setEnabled(false);
        speakButton.setMnemonic('s');
        mainPanel.add(speakButton);
        zipField = addLabeledTextField(mainPanel, "Zip Code: ", 4);
        cityField = addLabeledTextField(mainPanel, "Location: ", 15);
        return mainPanel;
    }

    private JTextField addLabeledTextField(JPanel panel, 
                    String labelName, int size) {
        JLabel label = new JLabel(labelName);
        JTextField field = new JTextField(size);
        field.setEditable(false);
        Box box = Box.createHorizontalBox();
        box.add(label);
        box.add(field);
        panel.add(box);
        return field;
    }

    private JPanel createImagePanel() {
        JPanel panel = getJPanel(new FlowLayout());
        JLabel imageLabel = new JLabel(createImageIcon("s4.jpg", "s4-logo"));
        panel.add(imageLabel);
        return panel;
    }

    /**
     * Creates a Panel that contains a label for messages.
     * This Panel should be located at the bottom of this Frame.
     *
     * @return a Panel that contains a label for messages
     */
    private JPanel createMessagePanel() {
        JPanel messagePanel = getJPanel(new BorderLayout());
        messageTextField = new JTextField
            ("Please wait while I'm loading...");
        messageTextField.setBackground(backgroundColor);
        messageTextField.setEditable(false);
        messagePanel.add(messageTextField, BorderLayout.CENTER);
        return messagePanel;
    }

    public static void main(String[] args) {
        ZipCity zipCity = new ZipCity();
        zipCity.setVisible(true);
        zipCity.go();
    }
}

