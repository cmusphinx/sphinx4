/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.audio;

import java.io.IOException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import javax.sound.sampled.AudioFormat;

/**
 * Provides an interface to view and play back various forms
 * of an audio signal.
 */
public class AudioPanel extends JPanel 
    implements MouseMotionListener, MouseListener {
    private AudioData audio;    
    private float xScale;
    private float yScale;
    private int xDragStart = 0;
    private int xDragEnd = 0;

    /**
     * Creates a new AudioPanel.  The scale factors represent how
     * much to scale the audio.  A scaleX factor of 1.0f means one pixel
     * per sample, and a scaleY factor of 1.0f means one pixel per resolution
     * of the sample (e.g., a scale of 1.0f would take 2**16 pixels).
     *
     * @param audioData the AudioData to draw
     * @param scaleX how much to scale the width of the audio
     * @param scaleY how much to scale the height
     */
    public AudioPanel(AudioData audioData,
                      float scaleX,
                      float scaleY) {
        this.audio = audioData;
        this.xScale = scaleX;
        this.yScale = scaleY;
        
	int width = (int) (audio.getAudioData().length * xScale);
	int height = (int) ((1 << 16) * yScale);

        setPreferredSize(new Dimension(width, height));
        setBackground(Color.white);

	audio.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent event) {
		    int width = (int)(audio.getAudioData().length * xScale);
		    int height = (int) ((1 << 16) * yScale);

		    setPreferredSize(new Dimension(width, height));
		    Dimension sz = getSize();
		    repaint(0, 0, 0, sz.width, sz.height);
		}
	    });

        addMouseMotionListener(this);
	addMouseListener(this);
        setFocusable(true);
        requestFocus();
    }

    /**
     * Repaints the component with the given Graphics.
     *
     * @param g the Graphics to use to repaint the component.
     */
    public void paintComponent(Graphics g) {
        int pos, index;
	int length;

        super.paintComponent(g);
        
	Dimension sz = getSize();
        int gZero = sz.height / 2;
        short[] audioData = audio.getAudioData();

	/**
	 * Only draw what is in the viewport.
	 */
	JViewport viewport = getViewport();
	if (viewport != null) {
	    Rectangle r = viewport.getViewRect();
	    pos = (int) r.getX();
	    length = (int) r.getWidth();
	} else {
	    pos = 0;
	    length = (int) (audioData.length * xScale);
	}

        /**
         * Fill in the whole image with white.
         */
	g.setColor(Color.WHITE);
        g.fillRect(pos, 0, length, sz.height - 1);

        /**
         * Now fill in the audio selection area as gray.
         */
        index = Math.max(0, audio.getSelectionStart());
        int selectionStart = (int) (index * xScale);
        index = audio.getSelectionEnd();
        if (index == -1) {
            index = audioData.length - 1;
        }
        int selectionEnd = (int) (index * xScale);
	g.setColor(Color.LIGHT_GRAY);
        g.fillRect(selectionStart, 0,
                   selectionEnd - selectionStart, sz.height - 1);

        /* Now scale the audio data and draw it.
         */
        int[] x = new int[length];
        int[] y = new int[length];
	for (int i = 0; i < length; i++) {
            x[i] = pos;
	    index = (int) (pos / xScale);
	    if (index < audioData.length) {
		y[i] = gZero - (int) (audioData[index] * yScale);
	    } else {
		break;
	    }
	    pos++;
        }
	g.setColor(Color.RED);
        g.drawPolyline(x, y, length);
    }

    /**
     * Finds the JViewport enclosing this component.
     */
    private JViewport getViewport() {
	Container p = getParent();
	if (p instanceof JViewport) {
	    Container gp = p.getParent();
	    if (gp instanceof JScrollPane) {
		JScrollPane scroller = (JScrollPane) gp;
		JViewport viewport = scroller.getViewport();
		if (viewport == null || viewport.getView() != this) {
		    return null;
		} else {
		    return viewport;
		}
	    }
	}
	return null;
    }

    /**
     * When the mouse is pressed, we update the selection in the
     * audio.
     *
     * @param evt the mouse pressed event
     */
    public void mousePressed(MouseEvent evt) {
	xDragStart = Math.max(0, evt.getX());
        audio.setSelectionStart((int) (xDragStart / xScale));
        audio.setSelectionEnd((int) (xDragStart / xScale));
    }

    /**
     * When the mouse is dragged, we update the selection in the
     * audio.
     *
     * @param evt the mouse dragged event
     */
    public void mouseDragged(MouseEvent evt) {
	xDragEnd = evt.getX();
        if (xDragEnd < (int) (audio.getSelectionStart() * xScale)) {
            audio.setSelectionStart((int) (xDragEnd / xScale));
        } else {
            audio.setSelectionEnd((int) (xDragEnd / xScale));
        }
    }

    public void mouseReleased(MouseEvent evt) {}
    public void mouseMoved(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}
    public void mouseClicked(MouseEvent evt) {}
}
