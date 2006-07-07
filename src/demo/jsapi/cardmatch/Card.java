
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

package demo.jsapi.cardmatch;



/**
 * A Card.
 */
public class Card {

    private String id;
    private String defaultImageFile;
    private String imageFile;
    private boolean isMatched;
    private boolean isSelected;


    /**
     * Constructs a Card with the following image file.
     *
     * @param imageFile the image file for this Card
     */
    public Card(String imageFile) {
        this.imageFile = imageFile;
        this.isMatched = false;
    }


    /**
     * Returns true if this Card is selected.
     *
     * @return <code>true</code> if the card was selected
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Returns the string representation of this card
     *
     * @return the string representation
     */
    public String toString() {
        return imageFile + " Matched:" + isMatched + " Selected: " +
            isSelected();
    }


    /**
     * Selects/deselects this Card.
     *
     * @param select whether to select this Card or not
     */
    public void setSelected(final boolean selected) {
        isSelected = selected;
    }


    /**
     * Returns true if this Card has already been matched correctly.
     *
     * @return <code>true</code> if the card has been matched
     */
    public boolean isMatched() {
        return isMatched;
    }


    /**
     * Sets if this Card has already been matched.
     *
     * @param matched true if it has already been match, false otherwise
     */
    public void setMatched(boolean matched) {
        this.isMatched = matched;
    }


    /**
     * Determines if the given card matches this card
     *
     * @param card the card to check
     *
     * @return <code>true</code> if the cards match
     */
    public boolean isMatch(Card card) {
        return getImageFile().equals(card.getImageFile());
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
}

