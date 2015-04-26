/**
 * Portions Copyright 2001-2003 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute,
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment.tokenizer;

import java.util.StringTokenizer;

/**
 * Represents a node in a Relation. Items can have shared contents but each
 * item has its own set of Daughters. The shared contents of an item
 * (represented by ItemContents) includes the feature set for the item and the
 * set of all relations that this item is contained in. An item can be
 * contained in a number of relations and as daughters to other items. This
 * class is used to keep track of all of these relationships. There may be many
 * instances of item that reference the same shared ItemContents.
 */
public class Item {
    private Relation ownerRelation;
    private ItemContents contents;
    private Item parent;
    private Item daughter;
    private Item next;
    private Item prev;

    /**
     * Creates an item. The item is coupled to a particular Relation. If shared
     * contents is null a new sharedContents is created.
     *
     * @param relation the relation that owns this item
     * @param sharedContents the contents that is shared with others. If null,
     *        a new sharedContents is created.
     */
    public Item(Relation relation, ItemContents sharedContents) {
        ownerRelation = relation;
        if (sharedContents != null) {
            contents = sharedContents;
        } else {
            contents = new ItemContents();
        }
        parent = null;
        daughter = null;
        next = null;
        prev = null;

        getSharedContents().addItemRelation(relation.getName(), this);
    }

    /**
     * Finds the item in the given relation that has the same shared contents.
     *
     * @param relationName the relation of interest
     *
     * @return the item as found in the given relation or null if not found
     */
    public Item getItemAs(String relationName) {
        return getSharedContents().getItemRelation(relationName);
    }

    /**
     * Retrieves the owning Relation.
     *
     * @return the relation that owns this item
     */
    public Relation getOwnerRelation() {
        return ownerRelation;
    }

    /**
     * Retrieves the shared contents for this item.
     *
     * @return the shared item contents
     */
    public ItemContents getSharedContents() {
        return contents;
    }

    /**
     * Determines if this item has daughters.
     *
     * @return true if this item has daughters
     */
    public boolean hasDaughters() {
        return daughter != null;
    }

    /**
     * Retrieves the first daughter of this item.
     *
     * @return the first daughter or null if none
     */
    public Item getDaughter() {
        return daughter;
    }

    /**
     * Retrieves the Nth daughter of this item.
     *
     * @param which the index of the daughter to return
     *
     * @return the Nth daughter or null if none at the given index
     */
    public Item getNthDaughter(int which) {
        Item d = daughter;
        int count = 0;
        while (count++ != which && d != null) {
            d = d.next;
        }
        return d;
    }

    /**
     * Retrieves the last daughter of this item.
     *
     * @return the last daughter or null if none at the given index
     */
    public Item getLastDaughter() {
        Item d = daughter;
        if (d == null) {
            return null;
        }
        while (d.next != null) {
            d = d.next;
        }
        return d;
    }

    /**
     * Adds the given item as a daughter to this item.
     *
     * @param item for the new daughter
     * @return created item
     */
    public Item addDaughter(Item item) {
        Item newItem;
        ItemContents contents;

        Item p = getLastDaughter();

        if (p != null) {
            newItem = p.appendItem(item);
        } else {
            if (item == null) {
                contents = new ItemContents();
            } else {
                contents = item.getSharedContents();
            }
            newItem = new Item(getOwnerRelation(), contents);
            newItem.parent = this;
            daughter = newItem;
        }
        return newItem;
    }

    /**
     * Creates a new Item, adds it as a daughter to this item and returns the
     * new item.
     *
     * @return the newly created item that was added as a daughter
     */
    public Item createDaughter() {
        return addDaughter(null);
    }

    /**
     * Returns the parent of this item.
     *
     * @return the parent of this item
     */
    public Item getParent() {
        Item n;
        for (n = this; n.prev != null; n = n.prev) {
        }
        return n.parent;
    }

    /**
     * Sets the parent of this item.
     *
     * @param parent the parent of this item
     */
    /*
     * private void setParent(Item parent) { this.parent = parent; }
     */

    /**
     * Returns the utterance associated with this item.
     *
     * @return the utterance that contains this item
     */
    public Utterance getUtterance() {
        return getOwnerRelation().getUtterance();
    }

    /**
     * Returns the feature set of this item.
     *
     * @return the feature set of this item
     */
    public FeatureSet getFeatures() {
        return getSharedContents().getFeatures();
    }

    /**
     * Finds the feature by following the given path. Path is a string of ":"
     * or "." separated strings with the following interpretations:
     * <ul>
     * <li>n - next item
     * <li>p - previous item
     * <li>parent - the parent
     * <li>daughter - the daughter
     * <li>daughter1 - same as daughter
     * <li>daughtern - the last daughter
     * <li>R:relname - the item as found in the given relation 'relname'
     * </ul>
     * The last element of the path will be interpreted as a voice/language
     * specific feature function (if present) or an item feature name. If the
     * feature function exists it will be called with the item specified by the
     * path, otherwise, a feature will be retrieved with the given name. If
     * neither exist than a String "0" is returned.
     *
     * @param pathAndFeature the path to follow
     * @return created object
     */
    public Object findFeature(String pathAndFeature) {
        int lastDot;
        String feature;
        String path;
        Item item;
        Object results = null;

        lastDot = pathAndFeature.lastIndexOf(".");
        // string can be of the form "p.feature" or just "feature"

        if (lastDot == -1) {
            feature = pathAndFeature;
            path = null;
        } else {
            feature = pathAndFeature.substring(lastDot + 1);
            path = pathAndFeature.substring(0, lastDot);
        }

        item = findItem(path);
        if (item != null) {
            results = item.getFeatures().getObject(feature);
        }
        results = (results == null) ? "0" : results;

        // System.out.println("FI " + pathAndFeature + " are " + results);

        return results;
    }

    /**
     * Finds the item specified by the given path.
     *
     * Path is a string of ":" or "." separated strings with the following
     * interpretations:
     * <ul>
     * <li>n - next item
     * <li>p - previous item
     * <li>parent - the parent
     * <li>daughter - the daughter
     * <li>daughter1 - same as daughter
     * <li>daughtern - the last daughter
     * <li>R:relname - the item as found in the given relation 'relname'
     * </ul>
     * If the given path takes us outside of the bounds of the item graph, then
     * list access exceptions will be thrown.
     *
     * @param path the path to follow
     *
     * @return the item at the given path
     */
    public Item findItem(String path) {
        Item pitem = this;
        StringTokenizer tok;

        if (path == null) {
            return this;
        }

        tok = new StringTokenizer(path, ":.");

        while (pitem != null && tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (token.equals("n")) {
                pitem = pitem.getNext();
            } else if (token.equals("p")) {
                pitem = pitem.getPrevious();
            } else if (token.equals("nn")) {
                pitem = pitem.getNext();
                if (pitem != null) {
                    pitem = pitem.getNext();
                }
            } else if (token.equals("pp")) {
                pitem = pitem.getPrevious();
                if (pitem != null) {
                    pitem = pitem.getPrevious();
                }
            } else if (token.equals("parent")) {
                pitem = pitem.getParent();
            } else if (token.equals("daughter") || token.equals("daughter1")) {
                pitem = pitem.getDaughter();
            } else if (token.equals("daughtern")) {
                pitem = pitem.getLastDaughter();
            } else if (token.equals("R")) {
                String relationName = tok.nextToken();
                pitem =
                        pitem.getSharedContents()
                                .getItemRelation(relationName);
            } else {
                System.out.println("findItem: bad feature " + token + " in "
                        + path);
            }
        }
        return pitem;
    }

    /**
     * Gets the next item in this list.
     *
     * @return the next item or null
     */
    public Item getNext() {
        return next;
    }

    /**
     * Gets the previous item in this list.
     *
     * @return the previous item or null
     */
    public Item getPrevious() {
        return prev;
    }

    /**
     * Appends an item in this list after this item.
     *
     * @param originalItem new item has shared contents with this item (or *
     *        null)
     *
     * @return the newly appended item
     */
    public Item appendItem(Item originalItem) {
        ItemContents contents;
        Item newItem;

        if (originalItem == null) {
            contents = null;
        } else {
            contents = originalItem.getSharedContents();
        }

        newItem = new Item(getOwnerRelation(), contents);
        newItem.next = this.next;
        if (this.next != null) {
            this.next.prev = newItem;
        }

        attach(newItem);

        if (this.ownerRelation.getTail() == this) {
            this.ownerRelation.setTail(newItem);
        }
        return newItem;
    }

    /**
     * Attaches/appends an item to this one.
     *
     * @param item the item to append
     */
    void attach(Item item) {
        this.next = item;
        item.prev = this;
    }

    /**
     * Prepends an item in this list before this item.
     *
     * @param originalItem new item has shared contents with this item (or *
     *        null)
     *
     * @return the newly appended item
     */
    public Item prependItem(Item originalItem) {
        ItemContents contents;
        Item newItem;

        if (originalItem == null) {
            contents = null;
        } else {
            contents = originalItem.getSharedContents();
        }

        newItem = new Item(getOwnerRelation(), contents);
        newItem.prev = this.prev;
        if (this.prev != null) {
            this.prev.next = newItem;
        }
        newItem.next = this;
        this.prev = newItem;
        if (this.parent != null) {
            this.parent.daughter = newItem;
            newItem.parent = this.parent;
            this.parent = null;
        }
        if (this.ownerRelation.getHead() == this) {
            this.ownerRelation.setHead(newItem);
        }
        return newItem;
    }

    // Inherited from object
    public String toString() {
        // if we have a feature called 'name' use that
        // otherwise fall back on the default.
        String name = getFeatures().getString("name");
        if (name == null) {
            name = "";
        }
        return name;
    }

    /**
     * Determines if the shared contents of the two items are the same.
     *
     * @param otherItem the item to compare
     *
     * @return true if the shared contents are the same
     */
    public boolean equalsShared(Item otherItem) {
        if (otherItem == null) {
            return false;
        } else {
            return getSharedContents().equals(otherItem.getSharedContents());
        }
    }
}
