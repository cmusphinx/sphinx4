/**
 * 
 * Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.fst.openfst;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;

import edu.cmu.sphinx.fst.Arc;
import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.State;
import edu.cmu.sphinx.fst.semiring.Semiring;
import edu.cmu.sphinx.fst.utils.Utils;

/**
 * Provides the required functionality in order to convert from/to openfst's
 * text format
 * 
 * @author John Salatas <jsalatas@users.sourceforge.net>
 */
public class Convert {

    /**
     * Default private Constructor.
     */
    private Convert() {
    }

    /**
     * Exports an fst to the openfst text format Several files are created as
     * follows: - basename.input.syms - basename.output.syms - basename.fst.txt
     * See <a
     * href="http://www.openfst.org/twiki/bin/view/FST/FstQuickTour">OpenFst
     * Quick Tour</a>
     * 
     * @param fst the fst to export
     * @param basename the files' base name
     */
    public static void export(Fst fst, String basename) {
        exportSymbols(fst.getIsyms(), basename + ".input.syms");
        exportSymbols(fst.getOsyms(), basename + ".output.syms");
        exportFst(fst, basename + ".fst.txt");
    }

    /**
     * Exports an fst to the openfst text format
     * 
     * @param fst the fst to export
     * @param filename the openfst's fst.txt filename
     */
    private static void exportFst(Fst fst, String filename) {
        FileWriter file;
        try {
            file = new FileWriter(filename);
            PrintWriter out = new PrintWriter(file);

            // print start first
            State start = fst.getStart();
            out.println(start.getId() + "\t" + start.getFinalWeight());

            // print all states
            int numStates = fst.getNumStates();
            for (int i = 0; i < numStates; i++) {
                State s = fst.getState(i);
                if (s.getId() != fst.getStart().getId()) {
                    out.println(s.getId() + "\t" + s.getFinalWeight());
                }
            }

            String[] isyms = fst.getIsyms();
            String[] osyms = fst.getOsyms();
            numStates = fst.getNumStates();
            for (int i = 0; i < numStates; i++) {
                State s = fst.getState(i);
                int numArcs = s.getNumArcs();
                for (int j = 0; j < numArcs; j++) {
                    Arc arc = s.getArc(j);
                    String isym = (isyms != null) ? isyms[arc.getIlabel()]
                            : Integer.toString(arc.getIlabel());
                    String osym = (osyms != null) ? osyms[arc.getOlabel()]
                            : Integer.toString(arc.getOlabel());

                    out.println(s.getId() + "\t" + arc.getNextState().getId()
                            + "\t" + isym + "\t" + osym + "\t"
                            + arc.getWeight());
                }
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Exports a symbols' map to the openfst text format
     * 
     * @param syms the symbols' map
     * @param filename the the openfst's symbols filename
     */
    private static void exportSymbols(String[] syms, String filename) {
        if (syms == null)
            return;

        try {
            FileWriter file = new FileWriter(filename);
            PrintWriter out = new PrintWriter(file);

            for (int i = 0; i < syms.length; i++) {
                String key = syms[i];
                out.println(key + "\t" + i);
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Imports an openfst's symbols file
     * 
     * @param filename the symbols' filename
     * @return HashMap containing the impprted string-to-id mapping
     */
    private static HashMap<String, Integer> importSymbols(String filename) {
        HashMap<String, Integer> syms = null;

        try {
            FileInputStream fis = new FileInputStream(filename);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            syms = new HashMap<String, Integer>();
            String strLine;

            while ((strLine = br.readLine()) != null) {
                String[] tokens = strLine.split("\\t");
                String sym = tokens[0];
                Integer index = Integer.parseInt(tokens[1]);
                syms.put(sym, index);

            }
            br.close();
        } catch (IOException e1) {
            return null;
        }

        return syms;
    }

    /**
     * Imports an openfst text format Several files are imported as follows: -
     * basename.input.syms - basename.output.syms - basename.fst.txt
     * 
     * @param basename the files' base name
     * @param semiring the fst's semiring
     */
    public static Fst importFst(String basename, Semiring semiring) {
        Fst fst = new Fst(semiring);

        HashMap<String, Integer> isyms = importSymbols(basename + ".input.syms");
        if (isyms == null) {
            isyms = new HashMap<String, Integer>();
            isyms.put("<eps>", 0);
        }

        HashMap<String, Integer> osyms = importSymbols(basename
                + ".output.syms");
        if (osyms == null) {
            osyms = new HashMap<String, Integer>();
            osyms.put("<eps>", 0);
        }

        HashMap<String, Integer> ssyms = importSymbols(basename
                + ".states.syms");

        // Parse input
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(basename + ".fst.txt");
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }

        DataInputStream dis = new DataInputStream(fis);
        BufferedReader br = new BufferedReader(new InputStreamReader(dis));
        boolean firstLine = true;
        String strLine;
        HashMap<Integer, State> stateMap = new HashMap<Integer, State>();

        try {
            while ((strLine = br.readLine()) != null) {
                String[] tokens = strLine.split("\\t");
                Integer inputStateId;
                if (ssyms == null) {
                    inputStateId = Integer.parseInt(tokens[0]);
                } else {
                    inputStateId = ssyms.get(tokens[0]);
                }
                State inputState = stateMap.get(inputStateId);
                if (inputState == null) {
                    inputState = new State(semiring.zero());
                    fst.addState(inputState);
                    stateMap.put(inputStateId, inputState);
                }

                if (firstLine) {
                    firstLine = false;
                    fst.setStart(inputState);
                }

                if (tokens.length > 2) {
                    Integer nextStateId;
                    if (ssyms == null) {
                        nextStateId = Integer.parseInt(tokens[1]);
                    } else {
                        nextStateId = ssyms.get(tokens[1]);
                    }

                    State nextState = stateMap.get(nextStateId);
                    if (nextState == null) {
                        nextState = new State(semiring.zero());
                        fst.addState(nextState);
                        stateMap.put(nextStateId, nextState);
                    }
                    // Adding arc
                    if (isyms.get(tokens[2]) == null) {
                        isyms.put(tokens[2], isyms.size());
                    }
                    int iLabel = isyms.get(tokens[2]);
                    if (osyms.get(tokens[3]) == null) {
                        osyms.put(tokens[3], osyms.size());
                    }
                    int oLabel = osyms.get(tokens[3]);
                    float arcWeight = Float.parseFloat(tokens[4]);
                    Arc arc = new Arc(iLabel, oLabel, arcWeight, nextState);
                    inputState.addArc(arc);
                } else {
                    // This is a final weight
                    float finalWeight = Float.parseFloat(tokens[1]);
                    inputState.setFinalWeight(finalWeight);
                }
            }
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        fst.setIsyms(Utils.toStringArray(isyms));
        fst.setOsyms(Utils.toStringArray(osyms));

        return fst;
    }
}
