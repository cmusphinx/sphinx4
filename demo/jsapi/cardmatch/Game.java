
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A CardMatch Game.
 */
public class Game {

    private List cards;
    private Card card1;
    private Card card2;


    /**
     * Constructs a Card with the following image file.
     *
     * @param imageFile the image file for this Card
     */
    public Game(int numberOfCards, String[] imageFiles) {
        cards = createCards(numberOfCards, imageFiles);
        startOver();
    }


    /**
     * Return the list of Cards.
     *
     * @return the list of Cards
     */
    public List getCards() {
        return cards;
    }


    /**
     * Creates a list of cards.
     *
     * @param numberOfCards the number of cards in the created list
     * @param imageFiles the image files of people to use
     *
     * @return a list of cards
     */
    private List createCards(int numberOfCards, String[] imageFiles) {
        List list = new LinkedList();
        for (int i = 0; i < numberOfCards; i++) {
            int whichImage = i % imageFiles.length;
            Card card = new Card(imageFiles[whichImage]);
            list.add(card);
        }
        return list;
    }


    /**
     * Starts a new game.
     */
    public void startOver() {
        Collections.shuffle(cards);
        int id = 1;
        for (Iterator i = cards.iterator(); i.hasNext(); id++) {
            Card card = (Card) i.next();
            card.setID(String.valueOf(id));
            card.setDefaultImageFile(id + ".gif");
            card.setMatched(false);
            card.setSelected(false);
            card.resetJToggleButton();
        }
        card1 = null;
        card2 = null;
    }


    /**
     * Returns the Card of the given ID, which can be "1", "2", ... "6".
     *
     * @return the Card of the given ID
     */
    public Card getCard(String id) {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (card.getID().equals(id)) {
                // System.out.println("getCard(): " + card.getID());
                return card;
            }
        }
        return null;
    }


    /**
     * Turns over the card with the given ID, which can be "1", "2" ... "6".
     *
     * @param cardID the card to turn over
     */
    public void turnCard(String cardID) {
        Card card = getCard(cardID);
        if (card != null && !card.isMatched()) {
            card.setSelected(!card.isSelected());
            if (card1 == null) {
                card1 = card;
            } else {
                if (card2 == null && card1 != card) {
                    card2 = card;
                }
            }
        }
    }


    /**
     * Checks if there are any matches.
     *
     * @return true if the last two selected cards match, false otherwise
     */
    public boolean checkForMatches() {
        if (card1 != null && card2 != null) {
            // System.out.println("Card1 = " + card1.getID());
            // System.out.println("Card2 = " + card2.getID());
            boolean match =
                (card1.getImageFile().equals(card2.getImageFile()));
            if (match) {
                card1.setMatched(true);
                card2.setMatched(true);
                card1 = null;
                card2 = null;
            }
            return match;
        }
        return false;
    }


    /**
     * Turn back the last two cards that were guessed.
     */
    public void turnGuessedCards() {
        if (card1 != null && card2 != null) {
            if (!card1.isMatched()) {
                card1.setSelected(false);
            }
            if (!card2.isMatched()) {
                card2.setSelected(false);
            }
            card1 = null;
            card2 = null;
        }
    }


    /**
     * Returns true if there are two guesses already.
     *
     * @return true if there were two guesses already
     */
    public boolean hasTwoGuesses() {
        return (card1 != null && card2 != null);
    }


    /**
     * Returns true if this game has been won.
     *
     * @return true if this game has been won
     */
    public boolean hasWon() {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (!card.isMatched()) {
                return false;
            }
        }
        return true;
    }
}

