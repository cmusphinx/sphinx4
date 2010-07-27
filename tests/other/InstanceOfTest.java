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
package other;

import edu.cmu.sphinx.util.Timer;
import edu.cmu.sphinx.util.TimerPool;

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
	instanceOfTimer = TimerPool.getTimer(this, "instanceOfTimer");
	booleanTimer = TimerPool.getTimer(this, "booleanTimer");
	castTimer = TimerPool.getTimer(this, "castTimer");
	noCastTimer = TimerPool.getTimer(this, "noCastTimer");
    }

    /**
     * instance of check
     */
    public int doInstanceOfCheck(BaseClass[] bc) {
	int fooCount = 0;
	instanceOfTimer.start();
    for (int i = 0; i < maxIterations; i++) {
        for (BaseClass baseClass : bc) {
            if (baseClass instanceof FooClass) {
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
        for (BaseClass baseClass : bc) {
            if (baseClass.isFoo()) {
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
        for (BaseClass baseClass : bc) {
            if (baseClass instanceof FooClass) {
                FooClass foo = (FooClass)baseClass;
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
        for (BaseClass baseClass : bc) {
            if (baseClass instanceof FooClass) {
                FooClass foo = ((FooClass)baseClass).getFooClass();
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
        TimerPool.dumpAll();
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

	TimerPool.resetAll();

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
