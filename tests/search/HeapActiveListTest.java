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

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.HeapActiveList;
import edu.cmu.sphinx.decoder.search.Token;


public class HeapActiveListTest {

    public static void main(String[] argv) {

        ActiveList activeList = new edu.cmu.sphinx.decoder.search.HeapActiveList(6);
        edu.cmu.sphinx.decoder.search.Token token1 = new edu.cmu.sphinx.decoder.search.Token(null, null, 9.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token2 = new edu.cmu.sphinx.decoder.search.Token(null, null, 4.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token3 = new edu.cmu.sphinx.decoder.search.Token(null, null, 7.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token4 = new edu.cmu.sphinx.decoder.search.Token(null, null, 6.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token5 = new edu.cmu.sphinx.decoder.search.Token(null, null, 10.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token6 = new edu.cmu.sphinx.decoder.search.Token(null, null, 8.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token7 = new edu.cmu.sphinx.decoder.search.Token(null, null, 5.0f, 0.0f, 0.0f, 0);
        edu.cmu.sphinx.decoder.search.Token token8 = new edu.cmu.sphinx.decoder.search.Token(null, null, 11.5f, 0.0f, 0.0f, 0);

        activeList.add(token1);
        activeList.add(token2);
        activeList.add(token3);
        activeList.add(token4);
        activeList.add(token5);
        activeList.add(token6);
        activeList.add(token7);
        activeList.add(token8);

        System.out.println(activeList.toString());

        edu.cmu.sphinx.decoder.search.Token token9 = new edu.cmu.sphinx.decoder.search.Token(null, null, 6.5f, 0.0f, 0.0f, 0);

        activeList.replace(token5, token9);

        System.out.println(activeList.toString());
    }
}
