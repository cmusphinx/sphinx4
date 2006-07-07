
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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A CardMatch Game.
 */
public class Game {
    private List cards;


    /**
     * Constructs a Card with the following image file.
     *
     * @param numberOfCards the number of cards to use in the game
     * @param imageFile the array of image file for the cards
     */
    public Game(int numberOfCards, List imageFiles) {
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
    private List createCards(int numberOfCards, List imageFiles) {
        List list = new LinkedList();
        for (int i = 0; i < numberOfCards; i++) {
            int whichImage = i % imageFiles.size();
            Card card = new Card((String) imageFiles.get(whichImage));
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
        }
    }


    /**
     * Returns the Card of the given ID, which can be "1", "2", ... "6".
     *
     * @param id the card id
     * @return the Card of the given ID
     */
    public Card getCard(String id) {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (card.getID().equals(id)) {
                return card;
            }
        }
        return null;
    }


    /**
     * Mark all cards matching the given ID as unmatched
     *
     * @param id the card id
     */
    public void unsetMatch(Card card) {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card otherCard = (Card) i.next();
            if (card.isMatch(otherCard)) {
                otherCard.setMatched(false);
            }
        }
    }


    /**
     * Turns over the card with the given ID, which can be "1", "2" ... "6".
     *
     * @param cardID the card to turn over
     */
    public void turnCard(String cardID) {
        Card card = getCard(cardID);
        if (card != null && !card.isMatched()) {
            card.setSelected(true);
        }
    }


    /**
     * Determines if the given card is already selected
     * 
     * @param cardID the card
     *
     * @return <code>true</code>  if the card is already selected
     */
    public boolean isSelected(String cardID) {
        return getCard(cardID).isSelected();
    }

    /**
     * Checks if there are any matches.
     *
     * @return <code>true</code>  if the last two selected 
     * cards match, false otherwise
     */
    public boolean processMatches() {
        Card card0 = findUnmatchedCard(0);
        Card card1 = findUnmatchedCard(1);

        if (card0 != null && card1 != null) {
            if (card0.isMatch(card1)) {
                card0.setMatched(true);
                card1.setMatched(true);
                return true;
            }
        }
        return false;
    }


    /**
     * finds the first unmatched card
     *
     * @param which which card to find
     *
     * @return the first unmatched card
     */
    private Card findUnmatchedCard(int which) {
        int count = 0;
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (card.isSelected() && !card.isMatched()) {
                if (count == which) {
                    return card;
                }
                count++;
            }
        }
        return null;
    }
        


    /**
     * Turn back the last two cards that were guessed.
     */
    public void turnGuessedCards() {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (!card.isMatched()) {
                card.setSelected(false);
            }
        }
    }


    /**
     * Returns the number of selected, but unmatched cares
     *
     * @return the number of unselected cards
     */
    public int getNumSelected() {
        int numSelected = 0;
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            if (card.isSelected() && !card.isMatched()) {
                numSelected ++;
            }
        }
        return numSelected;
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

