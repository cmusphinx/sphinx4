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
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
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

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * A simple demonstration application for Sphinx-4, suitable for use
 * as a WebStart application. ZipCity listens for spoken US zip codes 
 * and shows the city/state associated with the code.
 */
public class ZipCity extends JFrame {
    private final static Color backgroundColor = new Color(0x42, 0x42, 0x42);
    private final static Color NORM_COLOR = new Color(0x72, 0x72, 0x82);
    private final static Color HIGHLIGHT_COLOR = new Color(0xAA, 0xBB, 0x33);
    private final static Font labelFont = new Font("SanSerif", Font.BOLD, 16);

    private JTextField messageTextField;
    private JButton speakButton;
    private JPanel mapPanel;
    private JPanel imagePanel;

    private ZipInfo currentInfo;
    private ZipRecognizer zipRecognizer;
    private ZipDatabase zipDB;

    /**
     * Constructs a ZipCity with the given title.
     *
     * @param title the title of this JFrame
     * @param live the Live instance that this GUI controls
     */
    public ZipCity() {
        super("ZipCity - a Sphinx-4 WebStart Demo");
        setSize(900, 520);
        setDefaultLookAndFeelDecorated(true);
        imagePanel = createImagePanel();
        mapPanel = createMapPanel();
        setApplicationIcon();

        getContentPane().add(imagePanel, BorderLayout.CENTER);
        getContentPane().add(createMainPanel(), BorderLayout.SOUTH);

        // exit if the window is closed

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
                    if (zipRecognizer.microphoneOn()) {
                        setMessage("Speak a zip code ...");
                    } else {
                        setMessage(
                        "Sorry, can't find the microphone on your computer.");
                    }
                }
            }
        });
    }


    /**
     *  Replaces the splash s4 logo with the map
     */
    public void displayMap() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getContentPane().remove(imagePanel);
                getContentPane().add(mapPanel, BorderLayout.CENTER);
                speakButton.setEnabled(true);
                validate();
                repaint();
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

            setMessage("Loading recognizer...(apologies to Alaska and Hawaii)");
            zipRecognizer = new ZipRecognizer();

            setMessage("Starting recognizer...");
            zipRecognizer.startup();

            zipRecognizer.addZipListener(new ZipListener() {
                public void notify(String zip) {
                    updateMap(zip);
                }
            });

            displayMap();
            setMessage("ZipCity Version 2.0 - Ready");
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
    private void updateMap(final String zip) {
        final ZipInfo zipInfo = zipDB.lookup(zip);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (zip == null) {
                    setMessage("I didn't understand what you said");
                } else {
                    if (zipInfo == null) {
                        setMessage("Can't find " + zip  + " in the database");
                    } else {
                        String location = zipInfo.getCity() + ", " 
                            + zipInfo.getState();
                        setMessage("");
                        currentInfo = zipInfo;
                    }
                }
                mapPanel.repaint();
                speakButton.setEnabled(true);
            }
        });
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
        mainPanel.add(createMessagePanel());
        return mainPanel;
    }

    /**
     * Constructs the image Panel 
     *
     * @return the image panel
     */
    private JPanel createImagePanel() {
        JPanel panel = getJPanel(new FlowLayout());
        JLabel imageLabel = new JLabel(createImageIcon("s4.jpg", "s4-logo"));
        panel.add(imageLabel);
        return panel;
    }


    /**
     * Creates the map panel
     *
     * @return the map panel
     */
    private JPanel createMapPanel() {
        return new MapPanel();
    }

    /**
     * A panel that draws a map of the US based upon the
     * zip code database
     */
    class MapPanel extends JPanel {

        private final static float MIN_LAT = 24.0f;
        private final static float MAX_LAT = 52f;
        private final static float MAX_LONG = 126f;
        private final static float MIN_LONG = 65f;
        private final static float RANGE_LAT = MAX_LAT - MIN_LAT;
        private final static float RANGE_LONG = MAX_LONG - MIN_LONG;
        private final static int NORMAL_SIZE = 2;
        private final static int HIGHLIGHT_SIZE = 10;

        private final static float DEFAULT_LAT = 25;
        private final static float DEFAULT_LONG = 120;

        private final static int WIDTH_OFFSET = 10;
        private final static int HEIGHT_OFFSET = 8;
        private final static int MARGIN = 10;
        private Graphics g; 
        private Dimension size;


        /**
         * Creates the map panel
         */
        MapPanel() {
            setFont(labelFont);
            setBackground(backgroundColor);
        }


        /**
         * Updates the map
         */
        public void paintComponent(Graphics graphics) {
            g = graphics;
            size = getSize();
            super.paintComponent(g);
            if (zipDB != null) {
                g.setColor(NORM_COLOR);
                for (Iterator i = zipDB.iterator();  i.hasNext(); ) {
                    ZipInfo zi = (ZipInfo) i.next();
                    plot(zi, false);
                }
                if (currentInfo != null) {
                    g.setColor(HIGHLIGHT_COLOR);
                    plot(currentInfo, true);
                }

            }
        }

        /**
         * Plots a zip info with given size
         *
         * @param zi the zip info to plot
         * @param highlight if true this info should be highlighted
         *
         */
        void plot(ZipInfo zi, boolean highlight) {
            //System.out.println("ll " + zi.getLongitude() + " " +
            //       zi.getLatitude());
            if (highlight) {
                drawZipInfo(zi);
            } else {
                int x = mapx(zi.getLongitude());
                int y = mapy(zi.getLatitude());
                plot(x,y, NORMAL_SIZE);
            }
        }

        /**
         * Draws the zip info on the map
         *
         * @param zi the zip info
         */
        void drawZipInfo( ZipInfo zi) {
            int x, y;
            String label = zi.getCity() + ", " + zi.getState() + " " +
                zi.getZip();
            Dimension d = getStringDimension(g, label);

            if (zi.getLatitude() < MIN_LAT || zi.getLatitude() > MAX_LAT 
                    || zi.getLongitude() < MIN_LONG ||
                    zi.getLongitude() > MAX_LONG) {
                x = mapx(DEFAULT_LONG);
                y = mapy(DEFAULT_LAT);
            } else {
                x = mapx(zi.getLongitude());
                y = mapy(zi.getLatitude());
                plot(x,y, HIGHLIGHT_SIZE);
            }
            int xpos = x - WIDTH_OFFSET;
            int ypos = y - d.height / 2;

            if (xpos + d.width + MARGIN > size.width) {
                xpos -= (xpos + d.width - size.width) + MARGIN;
            }
            g.drawString(label, xpos, ypos);
        }

        /**
         * plot a point with the given pixel size
         *
         * @param x the x position of the point
         * @param y the y position of the point
         * @param size the size of the point
         */
        void plot(int x, int y, int size) {
             g.fillOval(x, y, size, size);
        }

        /**
         * Maps the given x position to the map panel
         * @param x the x position
         *
         * @return the x position mapped onto the map panel
         */
        private int mapx(float x) {
            return size.width - 
                (int) (size.width * (x - MIN_LONG) / RANGE_LONG);
        }

        /**
         * Maps the given y position to the map panel
         * @param x the y position
         *
         * @return the y position mapped onto the map panel
         */
        private int mapy(float y)  {
            return size.height - 
                (int) (size.height * (y - MIN_LAT) / RANGE_LAT);
        }


        /**
         * Gets the width and height in pixels of the given string
         *
         * @param g the current graphics context
         * @param s the string of interest
         * @return the dimension, in pixels of the string
         */
        private Dimension getStringDimension(Graphics g, String s) {
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D r2d = fm.getStringBounds(s, g);
            return new Dimension((int) (r2d.getWidth() + .5),
                                 (int) (r2d.getHeight() + .5));
        }
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
            ("Please wait while I'm loading...", 40);
        messageTextField.setBackground(backgroundColor);
        messageTextField.setForeground(HIGHLIGHT_COLOR);
        messageTextField.setEditable(false);
        messageTextField.setBorder(new EmptyBorder(1,1,1,1));
        messageTextField.setFont(labelFont);
        messagePanel.add(messageTextField, BorderLayout.CENTER);
        return messagePanel;
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


