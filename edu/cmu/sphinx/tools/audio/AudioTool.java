/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.audio;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.sound.sampled.AudioFormat;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.tools.audio.AudioData;
import edu.cmu.sphinx.tools.audio.AudioPanel;
import edu.cmu.sphinx.tools.audio.AudioPlayer;
import edu.cmu.sphinx.tools.audio.RawRecorder;
import edu.cmu.sphinx.tools.audio.SpectrogramPanel;
import edu.cmu.sphinx.tools.audio.Utils;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;


/**
 * Records and displays the waveform and spectrogram of an audio signal.
 */
public class AudioTool {
    static final String CONTEXT = "AudioTool";
    static final String PREFS_CONTEXT = "/edu/cmu/sphinx/tools/audio/"
                                        + CONTEXT;
    static final String FILENAME_PREFERENCE = "filename";
    
    // -------------------------------
    // Component names
    // -----------------------------------
    // These names should match the correspondong names in the
    // spectrogram.config.xml
    
    static final String FRONT_END = "frontEnd";
    static final String DATA_SOURCE = "streamDataSource";
    static final String WINDOWER = "windower";
    
    static AudioData audio;
    static JFrame jframe;
    static AudioPanel audioPanel;
    static SpectrogramPanel spectrogramPanel;
    static JFileChooser fileChooser;
    static String filename; 
    static File file = null;
    static AudioPlayer player;
    static RawRecorder recorder;
    static boolean recording = false;
    static Preferences prefs;
    static float zoom = 1.0f;
    
    private static JMenuItem saveMenuItem;

    private static JButton playButton;
    private static JButton zoomInButton;
    private static JButton zoomOutButton;
    private static JButton zoomResetButton;

    /**
     * Gets a filename.
     */
    static public void getFilename(String title, int type) {
        int returnVal;
        
        fileChooser.setDialogTitle(title);
        fileChooser.setCurrentDirectory(file);
        fileChooser.setDialogType(type);

        if (type == JFileChooser.OPEN_DIALOG) {
            returnVal = fileChooser.showOpenDialog(jframe);
        } else {
            returnVal = fileChooser.showSaveDialog(jframe);
        }
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            filename = file.getAbsolutePath();
            prefs.put(FILENAME_PREFERENCE, filename);
        } 
    }

    /**
     */
    static public void populateAudio(String filename) {
        try {
            AudioData newAudio = Utils.readAudioFile(filename);
            if (newAudio == null) {
                newAudio = Utils.readRawFile(filename);
            }
	    zoomReset();
            audio.setAudioData(newAudio.getAudioData());
            player.play(audioPanel.getSelectionStart(),
                        audioPanel.getSelectionEnd());
        } catch (IOException e) {
            /* just ignore bad files. */
        }
    }
    
    /**
     */
    static public void getAudioFromFile(String filename) throws IOException {
        /* Supports alignment data.  The format of the alignment file
         * is as follows:
         *
         * input filename                String
         * number of (time tag) lines    int
         * time tag                      float String
         * time tag                      float String
         * time tag                      float String
         * ...
         *
         * Times are in seconds.
         */
        if (filename.endsWith(".align")) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename)));

            populateAudio(reader.readLine());
            
            int numPoints = Integer.parseInt(reader.readLine());
            float[] times = new float[numPoints];
            String[] labels = new String[numPoints];
            for (int i = 0; i < numPoints; i++) {
                StringTokenizer tokenizer = new StringTokenizer(
                    reader.readLine());
                while (tokenizer.hasMoreTokens()) {
                    times[i] = Float.parseFloat(tokenizer.nextToken());
                    labels[i] = tokenizer.nextToken();
                }
            }
            audioPanel.setLabels(times, labels);
            
            reader.close();
        } else {
            populateAudio(filename);
        }
    }
    

    /**
     * Zoom the panels according to the zoom scale.
     */
    private static void zoomPanels() {
	if (audioPanel != null) {
	    audioPanel.zoomSet(zoom);
	}
	if (spectrogramPanel != null) {
	    spectrogramPanel.zoomSet(zoom);
	}
    }

    /**
     * Zoom in.
     */
    private static void zoomIn() {
	zoom *= 2.0f;
	zoomPanels();
    }

    /**
     * Zoom out.
     */
    private static void zoomOut() {
	zoom /= 2.0f;
	zoomPanels();
    }

    /**
     * Reset zoom size.
     */
    private static void zoomReset() {
	zoom = 1.0f;
	zoomPanels();
    }


    /**
     * Creates the menu bar.
     */
    private static void createMenuBar(JFrame jframe) {
        JMenuBar menuBar = new JMenuBar();
        jframe.setJMenuBar(menuBar);

        JMenu menu = new JMenu("File");
        menuBar.add(menu);
        
        JMenuItem menuItem = new JMenuItem("Open...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getFilename("Open...", JFileChooser.OPEN_DIALOG);
                    if ((filename == null) || (filename.length() == 0)) {
                        return;
                    }
                    try {
                        getAudioFromFile(filename);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        menu.add(menuItem);
        
        saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
	saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if ((filename != null) && (filename.length() > 0)) {
                        try {
                            Utils.writeRawFile(audio, filename);
			    saveMenuItem.setEnabled(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        menu.add(saveMenuItem);
        
        menuItem = new JMenuItem("Save As...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control V"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getFilename("Save As...", JFileChooser.SAVE_DIALOG);
                    if ((filename == null) || (filename.length() == 0)) {
                        return;
                    }
                    try {
                        Utils.writeRawFile(audio, filename);
			saveMenuItem.setEnabled(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Quit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control Q"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    System.exit(0);
                }
            });
        menu.add(menuItem);



        
        menu = new JMenu("Edit");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Select All");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control A"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    audioPanel.selectAll();
                }
            });
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Crop");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control X"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    audioPanel.crop();
                }
            });
        menu.add(menuItem);
        



        menu = new JMenu("View");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Zoom In");
        menuItem.setAccelerator(KeyStroke.getKeyStroke('>'));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    zoomIn();
                }
            });
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Zoom Out");
        menuItem.setAccelerator(KeyStroke.getKeyStroke('<'));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    zoomOut();
                }
            });
        menu.add(menuItem);

        menuItem = new JMenuItem("Original Size");
        menuItem.setAccelerator(KeyStroke.getKeyStroke('!'));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    zoomReset();
                }
            });
        menu.add(menuItem);





        menu = new JMenu("Audio");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Play");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control P"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    player.play(audioPanel.getSelectionStart(),
                                audioPanel.getSelectionEnd());
                }
            });
        menu.add(menuItem);

        
        
        menuItem = new JMenuItem("Record Start/Stop");
        menuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
        menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (!recording) {
                        recording = true;
                        try {
                            recorder = new RawRecorder(
                                new AudioFormat(16000.0f, // sample rate
                                                16,       // sample size
                                                1,        // chan. (1 == mono)
                                                true,     // signed
                                                true));   // big endian
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        recorder.start();
			saveMenuItem.setEnabled(true);
                    } else {
                        recording = false;
                        audio.setAudioData(recorder.stop());
                        player.play(audioPanel.getSelectionStart(),
                                    audioPanel.getSelectionEnd());
                    }
                }
            });
        menu.add(menuItem);        
    }
    
    /**
     * Create the Panel where all the buttons are.
     *
     * @return a Panel with buttons on it.
     */
    private static JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
	FlowLayout layout = new FlowLayout();
	layout.setAlignment(FlowLayout.LEFT);
        buttonPanel.setLayout(layout);

        playButton = new JButton("Play");
        playButton.setEnabled(true);
        playButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    player.play(audioPanel.getSelectionStart(),
                                audioPanel.getSelectionEnd());
		}
	    });

        zoomInButton = new JButton("Zoom In");
        zoomInButton.setEnabled(true);
        zoomInButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    zoomIn();
		}
	    });

        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.setEnabled(true);
        zoomOutButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    zoomOut();
		}
	    });

        zoomResetButton = new JButton("Reset Size");
        zoomResetButton.setEnabled(true);
        zoomResetButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
                    zoomReset();
		}
	    });

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    System.exit(0);
		}
	    });

        buttonPanel.add(playButton);
        buttonPanel.add(zoomInButton);
        buttonPanel.add(zoomOutButton);
        buttonPanel.add(zoomResetButton);
        buttonPanel.add(exitButton);

        return buttonPanel;
    }


    /**
     * Main method.
     *
     * @param args argv[0] : The name of an audio file
     *             argv[1] : SphinxProperties file
     */
    static public void main(String[] args) {
        FrontEnd frontEnd;
        StreamDataSource dataSource;
        prefs = Preferences.userRoot().node(PREFS_CONTEXT);
        filename = prefs.get(FILENAME_PREFERENCE, "untitled.raw");
        file = new File(filename);

        try {
            URL url;
            if (args.length > 0) {
                filename = args[0];
            }
            if (args.length == 2) {
                url = new File(args[1]).toURI().toURL();
            } else {
                url = AudioTool.class.getResource("spectrogram.config.xml");
            }
            ConfigurationManager cm = new ConfigurationManager(url);

            
            frontEnd = (FrontEnd) cm.lookup(FRONT_END);
            dataSource = (StreamDataSource) cm.lookup(DATA_SOURCE);
            PropertySheet ps = cm.getPropertySheet(WINDOWER);
            float windowShiftInMs = ps.getFloat
                (RaisedCosineWindower.PROP_WINDOW_SHIFT_MS,
             RaisedCosineWindower.PROP_WINDOW_SHIFT_MS_DEFAULT);
            audio = new AudioData();

	    final JFrame jframe = new JFrame("AudioTool");
            fileChooser = new JFileChooser();
            createMenuBar(jframe);
            
            /* Scale the width according to the size of the
             * spectrogram.
             */
            float windowShiftInSamples = windowShiftInMs
                * audio.getAudioFormat().getSampleRate() / 1000.0f;
            audioPanel = new AudioPanel(audio,
                                        1.0f / windowShiftInSamples,
                                        0.004f);
            spectrogramPanel = new SpectrogramPanel(frontEnd, dataSource, audio);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
	    panel.add(createButtonPanel(), BorderLayout.NORTH);
            panel.add(audioPanel, BorderLayout.CENTER);
	    panel.add(spectrogramPanel, BorderLayout.SOUTH);
            
	    JScrollPane scroller = new JScrollPane(panel);

            player = new AudioPlayer(audio);
            player.start();
            
            getAudioFromFile(filename);
            
            jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setContentPane(scroller);
            jframe.pack();
            jframe.setSize(640,540);
            jframe.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }       
}
