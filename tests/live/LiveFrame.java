/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package tests.live;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 * The GUI class for the LiveDecoder
 */
public class LiveFrame extends JFrame {

    private Live live;
    private boolean debug = false;
    private boolean handsFree = false;
    
    // Dimension of this LiveFrame
    private Dimension dimension = new Dimension(500, 700);
    private Color backgroundColor = new Color(220, 220, 220);

    private Font globalFont;
    private String globalFontFace = "Arial";

    private JComboBox decoderComboBox;

    private JTextField messageTextField;
    private JTextField testFileField;

    private JTextArea textToSayArea;
    private JTextArea textHeardArea;

    private JTextArea wordAccuracyTextArea;
    private JTextArea sentenceAccuracyTextArea;
    private JTextArea speedTextArea;
    private JTextArea cumulativeSpeedTextArea;

    private JButton speakButton;
    private JButton stopButton;
    private JButton nextButton;
    private JButton playButton;
    private JButton clearButton;


    /**
     * Constructs a LiveFrame with the given title.
     *
     * @param title the title of this JFrame
     * @param live the Live instance that this GUI controls
     */
    public LiveFrame(String title, Live live) {
        super(title);
        this.live = live;
        handsFree = Boolean.getBoolean("handsFree");

        setSize(dimension);
        setDefaultLookAndFeelDecorated(true);
        getContentPane().add(createMainPanel(), BorderLayout.CENTER);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                LiveFrame.this.live.terminate();
                System.exit(0);
            }
        });

        setGlobalFontSize(20);
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
     * Sets the test file TextField.
     *
     * @param testFile the test file
     */
    public void setTestFile(final String testFile) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                testFileField.setText(testFile);
            }
        });
    }


    /**
     * Return the text to say (that is, the reference string).
     *
     * @return the reference string
     */
    public String getReference() {
        return textToSayArea.getText();
    }

    
    /**
     * Sets the name of the decoder at the decoder ComboBox.
     *
     * @param decoderName the name of the decoder to display at the ComboBox
     */
    public void setDecoderComboBox(final String decoderName) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                decoderComboBox.setSelectedItem(decoderName);
            }
        });        
    }


    /**
     * Sets the reference text label.
     *
     * @param reference the reference text
     */
    public void setReferenceLabel(final String reference) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textToSayArea.setText(reference);
            }
        });
    }


    /**
     * Sets the "Recognition" results.
     *
     * @param hypothesis the recognition result
     */
    public void setRecognitionLabel(final String hypothesis) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                textHeardArea.setText(hypothesis);
            }
        });
    }


    /**
     * Displays the word accuracy.
     *
     * @param wordAccuracy the word accuracy string
     */
    public void setWordAccuracyLabel(final String wordAccuracy) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                wordAccuracyTextArea.setText(wordAccuracy);
            }
        });
    }


    /**
     * Displays the sentence accuracy.
     *
     * @param sentenceAccuracy the sentence accuracy string
     */
    public void setSentenceAccuracyLabel(final String sentenceAccuracy) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sentenceAccuracyTextArea.setText(sentenceAccuracy);
            };
        });
    }


    /**
     * Displays the speed.
     *
     * @param speed the speed string
     */
    public void setSpeedLabel(final String speed) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                speedTextArea.setText(speed);
            }
        });
    }

    
    /**
     * Displays the cumulative speed.
     *
     * @param cumulativeSpeed the cumulative speed string
     */
    public void setCumulativeSpeedLabel(final String cumulativeSpeed) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cumulativeSpeedTextArea.setText(cumulativeSpeed);
            }
        });
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
     * Enables or disables the "Next" button.
     *
     * @param enable boolean to enable or disable
     */
    public void setNextButtonEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (nextButton != null) {
                    nextButton.setEnabled(enabled);
                }
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
     * Enables or disables the "Stop" button.
     *
     * @param enable boolean to enable or disable
     */
    public void setStopButtonEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                stopButton.setEnabled(enabled);
            }
        });
    }

    /**
     * Enables or disables the "Play" button.
     *
     * @param enable boolean to enable or disable
     */
    public void setPlayButtonEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playButton.setEnabled(enabled);
            }
        });
    }

    /**
     * Enables or disables the "Clear" button.
     *
     * @param enable boolean to enable or disable
     */
    public void setClearButtonEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clearButton.setEnabled(enabled);
            }
        });
    }


    /**
     * Enables or disables the Decoder ComboBox.
     */
    public void setDecoderComboBoxEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    decoderComboBox.setEnabled(enabled);
                }
            });
    }


    /**
     * Enters speaking (i.e., recording) mode, sets the various GUI objects
     * to the correct state.
     */
    public void enterSpeakingMode() {
        debugMessage("Entering speaking mode.");
        setGUISpeakingState(true);
        setMessage("Wait...");
        
        // update GUI states
        setRecognitionLabel("");

        // start recording
        debugMessage("Turning speaker on.");
        if (live.startRecording()) {
            debugMessage("Speaker turned on.");
            setMessage("OK, start speaking...");
            live.decode();
        } else {
            debugMessage("Can't turn speaker on.");
            setMessage("Error opening the audio device");
            setGUISpeakingState(false);
        }
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
        JPanel mainPanel = getJPanel(new BorderLayout());
        mainPanel.add(createTopPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(createMessagePanel(), BorderLayout.SOUTH);
        return mainPanel;
    }


    /**
     * Constructs the top Panel of this LiveFrame.
     * It contains a ComboBox of a list of all Decoders,
     * and a label specify the test file.
     *
     * @return the top Panel of this LiveFrame
     */
    private JPanel createTopPanel() {
        GridBagLayout gridBag = new GridBagLayout();
        JPanel topPanel = getJPanel(gridBag);
        GridBagConstraints c = new GridBagConstraints();

        // add "Decoder: " label
        JLabel decoderLabel = new JLabel("Decoder: ");
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 5;
        c.insets = new Insets(15, 7, 0, 0);
        gridBag.setConstraints(decoderLabel, c);
        topPanel.add(decoderLabel);

        // add ComboBox
        decoderComboBox = new DecoderComboBox(live.getDecoderList());
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(decoderComboBox, c);
        topPanel.add(decoderComboBox);
        c.fill = GridBagConstraints.NONE;

        // add "Test file: " label
        JLabel testLabel = new JLabel("Test file: ");
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        c.insets = new Insets(5, 7, 5, 0);
        gridBag.setConstraints(testLabel, c);
        topPanel.add(testLabel);

        // add test file field
        testFileField = new JTextField(); // live.getDecoder().getTestFile());
        testFileField.setEditable(false);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 5;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(testFileField, c);
        topPanel.add(testFileField);
        c.fill = GridBagConstraints.NONE;

        // add "New..." button
        JButton newTestFileButton = new JButton("New...");
        c.gridx = 4;
        c.gridy = 1;
        gridBag.setConstraints(newTestFileButton, c);
        topPanel.add(newTestFileButton);

        newTestFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseTestFile();
            }
        });

        return topPanel;
    }


    /**
     * Shows a FileDialog, and updates the testFileField if a file
     * is selected.
     */
    private void chooseTestFile() {
        JFileChooser chooser = new JFileChooser(live.getCurrentDirectory());
        int returnValue = chooser.showDialog(this, "Select");
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String testFile = chooser.getSelectedFile().getName();
            testFileField.setText(testFile);
            live.setTestFile(testFile);
        }
        live.setCurrentDirectory(chooser.getCurrentDirectory());
    }


    /**
     * Creates the center Panel of this Frame. It contains
     * the buttons like "Speak", "Stop", "Quit", as well
     * as text display what to say, and what was recognized.
     *
     * @return the center Panel
     */
    private JPanel createCenterPanel() {
        JPanel centerPanel = getJPanel(new BorderLayout());
        centerPanel.add(createTextPanel(), BorderLayout.CENTER);
        centerPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        return centerPanel;
    }


    /**
     * Create the Panel where the text you are supposed to say
     * and the recognized text appears.
     *
     * @return the text Panel
     */
    private JPanel createTextPanel() {
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        JPanel textPanel = getJPanel(gridBag);

        textToSayArea = new JTextArea();
        textToSayArea.setEditable(true);
        JScrollPane sayPane = getMainTextJScrollPane("Say: ", textToSayArea);
        // setReferenceLabel(live.getDecoder().getNextReference());

        textHeardArea = new JTextArea();
        textHeardArea.setEditable(false);
        JScrollPane heardPane = getMainTextJScrollPane
            ("Recognized: ", textHeardArea);

        JPanel statisticsPanel = createStatisticsPanel();

        // add the "Say: "... JPanel
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        gridBag.setConstraints(sayPane, c);
        textPanel.add(sayPane);

        // add the "Heard: "... JPanel
        c.gridy = 1;
        gridBag.setConstraints(heardPane, c);
        textPanel.add(heardPane);

        // add the statistics JPanel
        c.gridy = 2;
        gridBag.setConstraints(statisticsPanel, c);
        textPanel.add(statisticsPanel);

        return textPanel;
    }


    /**
     * Creates the JPanel where recognition statistics are displayed.
     *
     * @return the recognition statistics JPanel
     */
    private JPanel createStatisticsPanel() {
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        JPanel mainPanel = getJPanel(gridBag);
        mainPanel.setBorder
            (BorderFactory.createTitledBorder("Statistics: "));

        JPanel statisticsPanel = getJPanel(new GridLayout(0, 2));
        
        wordAccuracyTextArea = getStatisticsTextArea("0%");
        sentenceAccuracyTextArea = getStatisticsTextArea("0%");
        speedTextArea = getStatisticsTextArea("0 X RT");
        cumulativeSpeedTextArea = getStatisticsTextArea("0 X RT");

        statisticsPanel.add(getStatisticsTextArea("Word Accuracy:"));
        statisticsPanel.add(wordAccuracyTextArea);
        statisticsPanel.add(getStatisticsTextArea("Sentence Accuracy:"));
        statisticsPanel.add(sentenceAccuracyTextArea);
        statisticsPanel.add(getStatisticsTextArea("Speed:"));
        statisticsPanel.add(speedTextArea);
        statisticsPanel.add(getStatisticsTextArea("Cumulative Speed:"));
        statisticsPanel.add(cumulativeSpeedTextArea);

        clearButton = new JButton("Clear");

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                live.resetStatistics();
                setRecognitionLabel("");
            }
        });

        // add the left statistics JPanel
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 5;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(statisticsPanel, c);
        mainPanel.add(statisticsPanel);
        
        // add the right "Clear" button
        c.gridx = 5;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        gridBag.setConstraints(clearButton, c);
        mainPanel.add(clearButton);

        return mainPanel;
    }


    /**
     * Creates a JTextArea for the Statistics text.
     *
     * @param statistic the default statistic text shown at the text area
     *
     * @return a JTextArea for the Statistics text
     */
    private JTextArea getStatisticsTextArea(String statistic) {
        JTextArea textArea = new JTextArea(statistic);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(backgroundColor);
        textArea.setAlignmentX(JTextArea.RIGHT_ALIGNMENT);
        textArea.setMargin(new Insets(10,5,0,0));
        return textArea;
    }


    /**
     * Creates the main JScrollPane with the given JTextArea 
     * used to display the text to say and the text recognized.
     *
     * @param title title of the scrollPane
     * @param textArea textArea to display text
     *
     * @return the main JScrollPane
     */
    private JScrollPane getMainTextJScrollPane(String title, 
                                               JTextArea textArea) {
        textArea.setBackground(Color.WHITE);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane areaScrollPane = new JScrollPane(textArea);
        areaScrollPane.setBackground(backgroundColor);
        areaScrollPane.setVerticalScrollBarPolicy
            (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setBorder
            (BorderFactory.createCompoundBorder
             (BorderFactory.createCompoundBorder
              (BorderFactory.createTitledBorder(title),
               BorderFactory.createEmptyBorder(5,5,5,5)),
              areaScrollPane.getBorder()));
        
        return areaScrollPane;
    }


    /**
     * Create the Panel where all the buttons are.
     *
     * @return a Panel with buttons on it.
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = getJPanel(new FlowLayout());

        speakButton = new JButton("Speak");
        speakButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (speakButton.isEnabled()) {
                    Thread t = new Thread() {
                        public void run() {
                            enterSpeakingMode();
                        }
                    };

                    t.start();
                }
            }
        });

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    stopButton.setEnabled(false);
                    setMessage("Stop speaking");
                    live.stopRecording();
                }
            });

        if (!handsFree) {
            nextButton = new JButton("Next");
            nextButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setReferenceLabel(live.getNextReference());
                        setRecognitionLabel("");
                    }
                });
        }

        playButton = new JButton("Play");
        playButton.setEnabled(false);
        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setRecognitionLabel("");
                live.playUtterance();
            }
        });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // lets just exit, no need to terminate.
                // live.terminate();
                System.exit(0);
            }
        });

        buttonPanel.add(speakButton);
        buttonPanel.add(stopButton);
        if (nextButton != null) {
            buttonPanel.add(nextButton);
        }
        buttonPanel.add(playButton);
        buttonPanel.add(exitButton);

        return buttonPanel;
    }


    /**
     * Sets the state of the various relevant Swing GUI objects,
     * depending on whether we are in the 'speaking' state or not.
     *
     * @param speaking true for speaking state, false for a non-speaking state
     */
    private void setGUISpeakingState(final boolean speaking) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                speakButton.setEnabled(!speaking);
                stopButton.setEnabled(speaking);
                playButton.setEnabled(!speaking);
                decoderComboBox.setEnabled(!speaking);
            }
        });
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
     * Changes the font size of all components in this JFrame.
     * 
     * @param fontSize the new font size
     */
    private void setGlobalFontSize(int fontSize) {
        if (globalFont == null) {
            globalFont = getFont();
        }
        globalFont = new Font(globalFontFace, Font.PLAIN, fontSize);

        String[] guis = {"Button.font", "ComboBox.font", "Frame.font",
                         "Label.font", "List.font",
                         "Menu.font", "MenuItem.font",
                         "TextArea.font", "TextField.font",
                         "TitledBorder.font", "ToggleButton.font",
                         "ToolTip.font"};
        
        for (int i = 0; i < guis.length; i++) {
            UIManager.put(guis[i], globalFont);
        }

        setFont(globalFont);
        
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    /**
     * A ComboBox that shows which decoder we are using.
     */
    class DecoderComboBox extends JComboBox {

        private int lastIndex;

        /**
         * Constructs a DecoderComboBox with the given ComboBoxModel.
         *
         * @param model the ComboBoxModel that contains a list
         *    of all the decoder name
         */
        public DecoderComboBox(ComboBoxModel model) {
            super(model);
            lastIndex = -1;
            addActionListener(new ComboBoxActionListener());
        }

        class ComboBoxActionListener implements ActionListener {
            
            /**
             * Invoked when an action is performed on this DecoderComboBox.
             *
             * @param e the ActionEvent performed
             */
            public void actionPerformed(ActionEvent e) {
                if (getSelectedIndex() != lastIndex) {
                    Thread t = new Thread() {
                        public void run() {
                            String decoderName = (String) getSelectedItem();
                            try {
                                live.setDecoder(decoderName);
                                lastIndex = getSelectedIndex();
                            } catch (IOException ioe) {
                                setMessage("Error switching  to " 
                                        + decoderName);
                            }
                        }
                    };
                    t.start();
                }
            }
        }
    }
}

