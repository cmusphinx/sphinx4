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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * A simple demonstration application for Sphinx-4, suitable for use
 * as a WebStart application. ZipCity listens for spoken US zip codes 
 * and shows the city/state associated with the code.
 */
public class ZipCity extends JFrame {
    private ZipDatabase zipDB;
    private ZipRecognizer zipRecognizer;

    private JTextField messageTextField;
    private JTextField zipField;
    private JTextField cityField;
    private JButton speakButton;
    private Color backgroundColor = new Color(0xff, 0xff, 0xff);

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
                if (zipRecognizer != null) {
                    zipRecognizer.shutdown();
                }
                System.exit(0);
            }
        });

        // when the 'speak' button is pressed, enable recognition
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

    /**
     * Perform any needed initializations and then enable the 'speak'
     * button, allowing recognition to proceed
     */
    public void go() {
        try {
            setMessage("Loading zip codes...");
            zipDB = new ZipDatabase();

            setMessage("Loading recognizer...");
            zipRecognizer = new ZipRecognizer();

            setMessage("Starting recognizer...");
            zipRecognizer.startup();

            zipRecognizer.addZipListener(new ZipListener() {
                public void notify(String zip) {
                    updateForm(zip);
                }
            });

            setMessage("ZipCity Version 1.0");
            speakButton.setEnabled(true);
        } catch (Throwable e) {
            setMessage("Error: " + e.getMessage());
        }
    }

    /**
     * Update the display with the new zip code information. The
     * zip info is retrieved and if available disabled on the gui
     *
     * @param zip the zip code (in the form XXXX)
     */
    private void updateForm(final String zip) {
        final ZipInfo zipInfo = zipDB.lookup(zip);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (zip == null) {
                    zipField.setText("????");
                    cityField.setText("");
                    setMessage("I didn't understand what you said");
                } else {
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
                }
                speakButton.setEnabled(true);
            }
        });
    }

    /** 
     * Returns an ImageIcon, or null if the path was invalid. 
     *
     * @param path the path to the image resource.
     * @param description a description of the resource
     *
     * @return the image icon or null if the resource could not be
     * found.
     */
    protected ImageIcon createImageIcon(String path, String description) {
        URL imgURL = ZipCity.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            return null;
        }
    }

    /**
     * Sets the application icon. The image icon is visible when the
     * application is iconified.
     */
    private void setApplicationIcon() {
        URL url = ZipCity.class.getResource("s4.jpg");
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(image);
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

    /**
     * Adds a labeled text field to the given panel
     *
     * @param panel the panel to receive the new text field
     * @param labelName the label for the text field
     * @param size the size (in character cells) of the text field
     *
     * @return the added text field
     */
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

    /**
     * Creates an image panel with the sphinx-4 logo image
     */
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

    /**
     * The main program for zip city.  Creates the ZipCity frame,
     * displays it, and  runs it.
     *
     * @param args program arguments (none necessary or required for
     * zipcity)
     */
    public static void main(String[] args) {
        ZipCity zipCity = new ZipCity();
        zipCity.setVisible(true);
        zipCity.go();
    }
}

