
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

import java.awt.Insets;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;


/**
 * A Card.
 */
public class Card {

    private String id;
    private String defaultImageFile;
    private String imageFile;
    private JToggleButton toggleButton;
    private boolean isMatched;


    /**
     * Constructs a Card with the following image file.
     *
     * @param imageFile the image file for this Card
     */
    public Card(String imageFile) {
        this.imageFile = imageFile;
        this.toggleButton = null;
        this.isMatched = false;
    }


    /**
     * Returns true if this Card is selected.
     */
    public boolean isSelected() {
        if (toggleButton == null) {
            throw new IllegalStateException("This card has no JCheckBox.");
        }
        return toggleButton.isSelected();
    }


    /**
     * Selects/deselects this Card.
     *
     * @param select whether to select this Card or not
     */
    public void setSelected(final boolean selected) {
        if (!isMatched()) {
            if (toggleButton != null) {
		SwingUtilities.invokeLater( new Runnable() {
		    public void run() {
			toggleButton.setSelected(selected);
		    }
		});
            }
        }
    }


    /**
     * Returns true if this Card has already been matched correctly.
     */
    public boolean isMatched() {
        return isMatched;
    }


    /**
     * Sets if this Card has already been matched.
     *
     * @param matched true if it has already been match, false otherwise
     */
    public void setMatched(final boolean matched) {
        this.isMatched = matched;
        if (toggleButton != null) {
	    SwingUtilities.invokeLater( new Runnable() {
		public void run() {
		    toggleButton.setSelected(matched);
		}
	    });
        }
    }


    /**
     * Sets the ID of this Card.
     *
     * @param id the ID of this Card
     */
    public void setID(String id) {
        this.id = id;
    }


    /**
     * Returns the ID of this Card.
     *
     * @return the ID of this Card
     */
    public String getID() {
        return id;
    }


    /**
     * Returns the name of the image file.
     *
     * @param the name of the image file
     */
    public String getImageFile() {
        return imageFile;
    }


    /**
     * Sets the name of the default image file
     *
     * @param the name of the default image file
     */
    public void setDefaultImageFile(String defaultImageFile) {
        this.defaultImageFile = defaultImageFile;
    }


    /**
     * Get the name of the default image file
     *
     * @return the name of the default image file
     */
    public String getDefaultImageFile() {
        return defaultImageFile;
    }


    /**
     * Resets the JToggleButton.
     */
    public synchronized void resetJToggleButton() {
        toggleButton = null;
    }


    /**
     * Returns a JCheckBox for the given Card.
     *
     * @param card the card of the JCheckBox
     *
     * @return a JCheckBox
     */
    public synchronized JToggleButton getJToggleButton() {
        if (toggleButton == null) {
            toggleButton = new JToggleButton
                ("Card " + id, new ImageIcon(getDefaultImageFile()));
            toggleButton.setSelectedIcon(new ImageIcon(getImageFile()));
            toggleButton.setBorderPainted(true);
            toggleButton.setContentAreaFilled(false);
            centerJToggleButton();
        }
        return toggleButton;
    }


    /**
     * Sets the various alignments for this button.
     */
    private void centerJToggleButton() {
        toggleButton.setHorizontalAlignment(SwingConstants.CENTER);
        toggleButton.setVerticalAlignment(SwingConstants.CENTER);
        toggleButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        toggleButton.setHorizontalTextPosition(SwingConstants.CENTER);
    }
}

