/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute,
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment;



/**
 * Represents an ordered set of {@link Item}s and their associated children. A
 * relation has a name and a list of items, and is added to an
 * {@link Utterance} via an {@link UsEnglishWordExpander}.
 */
public class Relation {
    private String name;
    private Utterance owner;
    private Item head;
    private Item tail;

    /**
     * Name of the relation that contains tokens from the original input text.
     * This is the first thing to be added to the utterance.
     */
    public static final String TOKEN = "Token";

    /**
     * Name of the relation that contains the normalized version of the
     * original input text.
     */
    public static final String WORD = "Word";

    /**
     * Creates a relation.
     *
     * @param name the name of the Relation
     * @param owner the utterance that contains this relation
     */
    Relation(String name, Utterance owner) {
        this.name = name;
        this.owner = owner;
        head = null;
        tail = null;
    }

    /**
     * Retrieves the name of this Relation.
     *
     * @return the name of this Relation
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the head of the item list.
     *
     * @return the head item
     */
    public Item getHead() {
        return head;
    }

    /**
     * Sets the head of the item list.
     *
     * @param item the new head item
     */
    void setHead(Item item) {
        head = item;
    }

    /**
     * Gets the tail of the item list.
     *
     * @return the tail item
     */
    public Item getTail() {
        return tail;
    }

    /**
     * Sets the tail of the item list.
     *
     * @param item the new tail item
     */
    void setTail(Item item) {
        tail = item;
    }

    /**
     * Adds a new item to this relation. The item added does not share its
     * contents with any other item.
     *
     * @return the newly added item
     */
    public Item appendItem() {
        return appendItem(null);
    }

    /**
     * Adds a new item to this relation. The item added shares its contents
     * with the original item.
     *
     * @param originalItem the ItemContents that will be shared by the new item
     *
     * @return the newly added item
     */
    public Item appendItem(Item originalItem) {
        ItemContents contents;
        Item newItem;

        if (originalItem == null) {
            contents = null;
        } else {
            contents = originalItem.getSharedContents();
        }
        newItem = new Item(this, contents);
        if (head == null) {
            head = newItem;
        }

        if (tail != null) {
            tail.attach(newItem);
        }
        tail = newItem;
        return newItem;
    }

    /**
     * Returns the utterance that contains this relation.
     *
     * @return the utterance that contains this relation
     */
    public Utterance getUtterance() {
        return owner;
    }
}
