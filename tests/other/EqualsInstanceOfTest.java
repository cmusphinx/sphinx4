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

package tests.other;

/**
 * A comparison of speed between using the instanceof operation and
 * a direct object reference comparison.
 */
public class EqualsInstanceOfTest {

    private class Apple extends Fruit {
        public Apple() {
            super("apple");
        }
    }

    /**
     * Time the instanceof operation.
     *
     * @param times the number of times instanceof is called
     *
     * @return the total amount of time the instanceof operation took
     */
    public long doInstanceOfTest(int times) {
        Apple apple = new Apple();
        long time = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            if (apple instanceof Fruit) {
            }
        }
        time = System.currentTimeMillis() - time;
        return time;
    }

    /**
     * Time the execution of the equals() method. Note that this test
     * is the best case scenario, since the equals() method should
     * return immediately (because they are the same object).
     *
     * @param times the number of times equals() is called.
     *
     * @return the total amount of time the equals() method takes to execute
     */
    public long doEqualsTest(int times) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            if (Fruit.ORANGE.equals(Fruit.ORANGE)) {
            }
        }
        time = System.currentTimeMillis() - time;
        return time;
    }

    /**
     * Time the execution of the == operator.
     *
     * @param times the number of times the == operator is used
     *
     * @return the total amount of time the == operator takes to used
     */
    public long doEqualsEqualsTest(int times) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            if (Fruit.ORANGE == Fruit.ORANGE) {
            }
        }
        time = System.currentTimeMillis() - time;
        return time;
    }

    /**
     * Main program to compare the execution times of the instanceof
     * operation and the equals() method. To execute:
     * <code>
     * java tests.other.EqualsInstanceOfTest
     * </code>
     */
    public static void main(String[] argv) {
        EqualsInstanceOfTest test = new EqualsInstanceOfTest();
        int times = 100000;
        long instanceOfTime = test.doInstanceOfTest(times);
        long equalsTime = test.doEqualsTest(times);
        long equalsEqualsTime = test.doEqualsEqualsTest(times);

        System.out.println
            ("Doing instanceof " + times + " times took " + instanceOfTime +
             " secs.");
        System.out.println
            ("Doing equals " + times + " times took " + equalsTime + " secs.");
        System.out.println
            ("Doing == " + times + " times took " + equalsEqualsTime + " secs.");
    }
}
