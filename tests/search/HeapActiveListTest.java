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

package tests.search;

import edu.cmu.sphinx.search.ActiveList;
import edu.cmu.sphinx.search.HeapActiveList;
import edu.cmu.sphinx.search.Token;


public class HeapActiveListTest {

    public static void main(String[] argv) {

        ActiveList activeList = new HeapActiveList(6);
        Token token1 = new Token(null, null, 9.0f, 0.0f, 0.0f, 0);
        Token token2 = new Token(null, null, 4.0f, 0.0f, 0.0f, 0);
        Token token3 = new Token(null, null, 7.0f, 0.0f, 0.0f, 0);
        Token token4 = new Token(null, null, 6.0f, 0.0f, 0.0f, 0);
        Token token5 = new Token(null, null, 10.0f, 0.0f, 0.0f, 0);
        Token token6 = new Token(null, null, 8.0f, 0.0f, 0.0f, 0);
        Token token7 = new Token(null, null, 5.0f, 0.0f, 0.0f, 0);
        Token token8 = new Token(null, null, 11.5f, 0.0f, 0.0f, 0);

        activeList.add(token1);
        activeList.add(token2);
        activeList.add(token3);
        activeList.add(token4);
        activeList.add(token5);
        activeList.add(token6);
        activeList.add(token7);
        activeList.add(token8);

        System.out.println(activeList.toString());

        Token token9 = new Token(null, null, 6.5f, 0.0f, 0.0f, 0);

        activeList.replace(token5, token9);

        System.out.println(activeList.toString());
    }
}
