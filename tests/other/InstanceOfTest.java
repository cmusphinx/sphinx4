
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

import edu.cmu.sphinx.util.Timer;

/**
 * 
 * Performas some math timings. Used to compare different modes of
 * math (floating, double, mixed, integer)
 */
public class InstanceOfTest {
    private int maxIterations;



    Timer instanceOfTimer;
    Timer booleanTimer;
    Timer castTimer;
    Timer noCastTimer;

    public InstanceOfTest(int iterations) {
	maxIterations = iterations;
	instanceOfTimer = Timer.getTimer("InstanceOfTest", "instanceOfTimer");
	booleanTimer = Timer.getTimer("InstanceOfTest", "booleanTimer");
	castTimer = Timer.getTimer("InstanceOfTest", "castTimer");
	noCastTimer = Timer.getTimer("InstanceOfTest", "noCastTimer");
    }

    /**
     * instance of check
     */
    public int doInstanceOfCheck(BaseClass[] bc) {
	int fooCount = 0;
	instanceOfTimer.start();
        for (int i = 0; i < maxIterations; i++) {
	    for (int j = 0; j < bc.length; j++) {
		if (bc[j] instanceof FooClass) {
		    fooCount++;
		}
	    }
	}
	instanceOfTimer.stop();
	return fooCount;
    }

    /**
     * boolean check
     */
    public int doBooleanCheck(BaseClass[] bc) {
	int fooCount = 0;
	booleanTimer.start();
        for (int i = 0; i < maxIterations; i++) {
	    for (int j = 0; j < bc.length; j++) {
		if (bc[j].isFoo()) {
		    fooCount++;
		}
	    }
	}
	booleanTimer.stop();
	return fooCount;
    }

    /**
     * instance of check
     */
    public int doCastCheck(BaseClass[] bc) {
	int fooCount = 0;
	castTimer.start();
        for (int i = 0; i < maxIterations; i++) {
	    for (int j = 0; j < bc.length; j++) {
		if (bc[j] instanceof FooClass) {
		    FooClass foo = (FooClass) bc[j];
		    fooCount += foo.getBigId();
		}
	    }
	}
	castTimer.stop();
	return fooCount;
    }

    public int doNoCastCheck(BaseClass[] bc) {
	int fooCount = 0;
	noCastTimer.start();
        for (int i = 0; i < maxIterations; i++) {
	    for (int j = 0; j < bc.length; j++) {
		if (bc[j] instanceof FooClass) {
		    FooClass foo = ((FooClass) bc[j]).getFooClass();
		    fooCount += foo.getBigId();
		}
	    }
	}
	noCastTimer.stop();
	return fooCount;
    }


    /**
     * Time all of the routines for multiple interations
     */
    public void doScores(BaseClass[] bc) {
        doInstanceOfCheck(bc);
	doBooleanCheck(bc);
	doCastCheck(bc);
	doNoCastCheck(bc);
    }



    /**
     * Shows the results
     */
    public void dump() {
        Timer.dumpAll();
    }

    /**
     * Runs the timing test
     */
    public final static int COUNT = 10000;

    public static void main(String[] args) {
	InstanceOfTest iot = new InstanceOfTest(10000);
	BaseClass[] array = new BaseClass[COUNT];

	for (int i = 0; i < array.length; i++) {
	    array[i] = (i % 2 == 1) ? new BaseClass(i) : new FooClass(i);
	}

	// warm-up
	iot.doScores(array);

	Timer.resetAll();

	// go live
	iot.doScores(array);

	iot.dump();
    }
}




class BaseClass {

    int id;

    BaseClass(int id) {
	this.id = id;
    }

    boolean isFoo() {
	return false;
    }

    int getId() {
	return id;
    }
}


class FooClass extends BaseClass {
    FooClass(int id) {
	super(id);
    }

    final boolean isFoo() {
	return true;
    }

    FooClass getFooClass() {
	return this;
    }

    int getBigId() {
	return getId() * 2;
    }
}
