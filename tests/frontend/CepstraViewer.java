/**
 * [[[copyright]]]
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
    private int lastEnergy;
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
        lastEnergy = 0;
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
        canvas.repaint();
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
     * A Panel to draw the Cepstra.
     */
    class CepstraPanel extends JPanel {

        private int pixelsPerYUnit = 10;
        private int maxHeight = getHeight();

        /**
         * If the Cepstrum contains data,
         * it will plot the energy value of the Cepstrum, otherwise,
         * if it is a SPEECH_START, it will draw a green line,
         * if it is a SPEECH_END Cepstrum, it will draw a red line.
         */
        public void paint(Graphics g) {
            clear(g);
            drawGrid(g);

            // draw the current CepstraGroup
            if (current != null) {
                int x = 1;
                Cepstrum[] cepstra = current.getCepstra();
                for (int i = 0; i < cepstra.length; i++) {
                    drawCepstrum(cepstra[i], g, x);
                    x += 2;
                }
                setSize(x, maxHeight);
                setPreferredSize(getSize());
                revalidate();
            }
        }

        /**
         * Clears this JPanel, essentially fills the background with white.
         */
        private void clear(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        /**
         * Draws the given Cepstrum at the given x position.
         *
         * @param cepstrum Cepstrum to draw
         * @param g the Graphics context to use
         * @param the x position to draw at
         */
        private void drawCepstrum(Cepstrum cepstrum, Graphics g, int x) {
            if (cepstrum.hasContent()) {
                // draw the cepstrum's energy
                int energy = (int) (cepstrum.getEnergy() * pixelsPerYUnit);
                g.setColor(Color.BLACK);
                g.drawLine(x, getHeight() - lastEnergy,
                           x+1, getHeight() - energy);
                lastEnergy = energy;
                
                // check for maxHeight
                if (energy > maxHeight) {
                    maxHeight = energy + 10;
                }
            } else {
                // draw the SPEECH_START and SPEECH_END
                Signal signal = cepstrum.getSignal();
                if (signal.equals(Signal.SPEECH_START)) {
                    drawSpeechLine(x, Color.GREEN, g);
                } else if (signal.equals(Signal.SPEECH_END)) {
                    drawSpeechLine(x, Color.RED, g);
                } else if (signal.equals(Signal.UTTERANCE_START) ||
                           signal.equals(Signal.UTTERANCE_END)) {
                    drawSpeechLine(x, Color.BLACK, g);
                }
            }
        }
        
        /**
         * Draws a vertical line at the given x coordinate with the
         * given color.
         *
         * @param xCoordinate the x coordinate to draw the line
         * @param lineColor the color of the line
         * @param g the Graphics context to draw the line
         */
        private void drawSpeechLine(int xCoordinate, Color lineColor, 
                                    Graphics g) {
            Color oldColor = g.getColor();
            g.setColor(lineColor);
            g.drawLine(xCoordinate, 0, xCoordinate, getHeight());
            g.setColor(oldColor);
        }

        /**
         * Draws the basic grid of this JPanel.
         *
         * @param g the Graphics context to draw the grid
         */
        private void drawGrid(Graphics g) {
            Color oldColor = g.getColor();
            g.setColor(Color.LIGHT_GRAY);
            for (int y = getHeight(); y > 0 ; y -= pixelsPerYUnit) {
                g.drawLine(0, y, getWidth(), y);
            }
            g.setColor(oldColor);
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
