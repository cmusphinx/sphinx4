/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package demo.sphinx.zipcity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Provides a zip to city, state lookup service
 */
public class ZipDatabase {
    private Map zipDB = new HashMap();

    /**
     * Creates the zip database
     *
     * @throws IOException if there is a problem loading the zip
     * database
     */
    public ZipDatabase() throws IOException {
        int line = 0;
        InputStream is = this.getClass().getResourceAsStream("zip.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String input;
        // parses entries of the form:
        // 03064,NH,NASHUA
        try {
            while  ((input = br.readLine()) != null) {
                line++;
                StringTokenizer st = new StringTokenizer(input, ",");
                if (st.countTokens() == 3) {
                    String zip = st.nextToken();
                    String state = st.nextToken();
                    String city = st.nextToken();
                    city = fixupCase(city);
                    zipDB.put(zip, new ZipInfo(zip, city, state));
                } else {
                    throw new IOException("Bad zip format, line " + line);
                }
            }
        } finally {
            br.close();
        }
    }

    /**
     * Looks up the city associated with the given zip code
     *
     * @param zipcode the zip code
     *
     * @return city info or null if the zip was not found
     */
    public ZipInfo lookup(String zipcode) {
        return (ZipInfo) zipDB.get(zipcode);
    }

    /**
     * Fixes up the case of a string.  In the zip data, some of the
     * cities are all upper case, while some are mixed case. fixupCase
     * converts the input string to a mixed case string
     * 
     * @param s the string to fixup
     * @return a mixed-case form of string
     */
    private String fixupCase(String s) {
        boolean nextIsUpper = true;
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (nextIsUpper) {
                c = Character.toUpperCase(c);
            }  else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
            nextIsUpper = Character.isWhitespace(c);
        }
        return sb.toString();
    }

    /**
     * A test program for zip info
     */
    public static void main(String[] args) throws Exception {
        ZipDatabase zipDB = new ZipDatabase();

        BufferedReader br = 
            new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("Enter zip: ");
            String zip = br.readLine();
            System.out.println("Input: " + zip);

            ZipInfo info = zipDB.lookup(zip);
            System.out.println("Output: " + info);
        }
    }

}


/**
 * Holds the zip information, including the zip, city and state
 */
class ZipInfo {
    private String zip;
    private String city;
    private String state;

    /**
     * Creates a ZipInfo 
     *
      * @param zip the zip code
      * @param city the city
      * @param state the state
      */
    ZipInfo(String zip, String city, String state) {
        this.zip = zip;
        this.city = city;
        this.state = state;
    }

    /**
     * Gets the zip code
     *
     * @return the zip code
     */
    public String getZip() {
        return zip;
    }

    /**
     * Gets the city
     *
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * Gets the state
     *
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * Returns a string representation of this object
     *
     * @return a string representation of this object
     */
    public String toString() {
        return "Zip: " + getZip() +
              " City: " + getCity() +
              " State: " + getState();
    }
}
