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
public class CepstraPanel extends JPanel {
    
    private int pixelsPerYUnit = 10;
    private int majorYInterval = 5;
    private int maxHeight = getHeight();
    private int y0Pixel = getHeight();
    
    private Color majorYIntervalColor = Color.GRAY;
    private Color minorYIntervalColor = Color.LIGHT_GRAY;

    private int lastEnergy = 0;
    private Cepstrum[] cepstra = null;

    /**
     * Sets the Cepstrum array to draw.
     * 
     * @param cepstra the Cepstrum array to draw
     */
    public void setCepstra(Cepstrum[] cepstra) {
        this.cepstra = cepstra;
        if (cepstra.length > 0) {
            float lowestEnergy = cepstra[0].getEnergy();
            // find the cepstrum with the lowest energy
            for (int i = 0; i < cepstra.length; i++) {
                if (cepstra[i].hasContent()) {
                    if (cepstra[i].getEnergy() < lowestEnergy) {
                        lowestEnergy = cepstra[i].getEnergy();
                    }
                    System.out.println(cepstra[i].getEnergy());
                }
            }
            y0Pixel = getHeight();
            if (lowestEnergy < 0) {
                y0Pixel = getHeight() - 
                    ((int) (lowestEnergy * -1) + 1) * pixelsPerYUnit;
            }
        }
        repaint();
    }
    
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
        if (cepstra != null) {
            int x = 1;
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
            g.drawLine(x, y0Pixel - lastEnergy, x+1, y0Pixel - energy);

            lastEnergy = energy;
            
            // check for maxHeight
            if (energy > maxHeight) {
                maxHeight = energy + 10;
            }
        } else {
            // draw the SPEECH_START and SPEECH_END
            Signal signal = cepstrum.getSignal();
            if (signal.equals(Signal.SPEECH_START)) {
                drawSpeechLine(x, Color.ORANGE, g);
            } else if (signal.equals(Signal.SPEECH_END)) {
                drawSpeechLine(x, Color.RED, g);
            } else if (signal.equals(Signal.UTTERANCE_START)) {
                drawSpeechLine(x, Color.BLUE, g);
            } else if (signal.equals(Signal.UTTERANCE_END)) {
                drawSpeechLine(x, Color.GREEN, g);
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
        boolean atYInterval = false;
        
        Color oldColor = g.getColor();
        g.setColor(minorYIntervalColor);
        
        // draw the grids from zero to the top of the Panel
        for (int y = y0Pixel, x = 0; y > 0;
             y -= pixelsPerYUnit, x %= majorYInterval) {
            drawGridLine(g, y, x++);
        }
        // draw the grids from zero to the bottom of the Panel
        for (int y = y0Pixel, x = 0; y < getHeight(); 
             y += pixelsPerYUnit, x %= majorYInterval) {
            drawGridLine(g, y, x++);
        }
        g.setColor(oldColor);
    }

    private void drawGridLine(Graphics g, int y, int x) {
        if (y == y0Pixel) {
            g.setColor(Color.CYAN);
        } else if (x == 0) {
            g.setColor(majorYIntervalColor);
        }
        g.drawLine(0, y, getWidth(), y);
        g.setColor(minorYIntervalColor);
    }
}
