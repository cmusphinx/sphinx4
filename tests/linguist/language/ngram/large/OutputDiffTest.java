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


package tests.linguist.language.ngram.large;

import java.io.*;
import java.util.*;

public class OutputDiffTest {

    public static void main(String[] argv) {

        String diff = argv[0];
        String file1 = argv[1];
        String file2 = argv[2];

        int maxDiff = Integer.parseInt(diff);

        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(file1));
            BufferedReader reader2 = new BufferedReader(new FileReader(file2));
            
            String line1, line2;

            while ((line1 = reader1.readLine()) != null &&
                   (line2 = reader2.readLine()) != null) {
                
                StringTokenizer token1 = new StringTokenizer(line1);
                StringTokenizer token2 = new StringTokenizer(line2);

                int v1 = Integer.parseInt(token1.nextToken());
                int v2 = Integer.parseInt(token2.nextToken());

                if (Math.abs(v1 - v2) > maxDiff) {
                    System.out.println(line1);
                    System.out.println(line2);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
                
