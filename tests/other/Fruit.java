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

package tests.other;

/**
 * Bogus class that implements a fruit, used in the EqualsInstanceOfTest.
 */
public class Fruit {

    /**
     * An orange.
     */
    public static final Fruit ORANGE = new Fruit("orange");

    /**
     * An apple.
     */
    public static final Fruit APPLE = new Fruit("apple");

    private String name;

    /**
     * Constructs a fruit with the given name.
     *
     * @param name the name of the fruit
     */
    public Fruit(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this Fruit.
     *
     * @return the name of this Fruit
     */
    public String toString() {
        return name;
    }

    /**
     * Returns true if the given object is equals this Fruit.
     * That is, if they are the same object, or if the name
     * of the given fruit is the same as the name of this fruit.
     *
     * @param object the Fruit object to test
     *
     * @return true if the given object is equal to this Fruit
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null) {
            return name.equals(object.toString());
        } else {
            return false;
        }
    }
}
