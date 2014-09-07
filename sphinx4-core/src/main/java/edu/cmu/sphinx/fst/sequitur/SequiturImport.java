/**
 * 
 * Copyright 2014 Universität Hamburg.
 * Portions Copyright 1999-2012 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.fst.sequitur;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;

import edu.cmu.sphinx.fst.Fst;
import edu.cmu.sphinx.fst.semiring.Semiring;
import edu.cmu.sphinx.fst.semiring.TropicalSemiring;

/**
 * Converter for an Fst in Sequitur G2P's XML to Sphinx binary OpenFst format.
 * 
 * Sequitur G2P (http://www-i6.informatik.rwth-aachen.de/web/Software/g2p.html)
 * provides easy-to-build G2P training facilities. Its binary models can be
 * converted to an XML FSA-format using fsa.py which is provided with Sequitur.
 * 
 * This program reads the XML and constructs a @link{edu.cmu.sphinx.fst.Fst},
 * which is then serialized into the Sphinx binary OpenFst format (but could
 * also be used directly).
 * 
 * NOTICE: Sequitur's fsa.py does not in all cases construct valid XML,
 * specifically it fails to encode XML character entities &amp;, &lt;, and &gt;
 * if these were part of the training material. If in doubt, please check for
 * and replace them in the alphabet portion of the XML prior to using this
 * converter.
 * 
 * Implementation details: - we add a state for &lt;s&gt; to the end of both
 * symbol alphabets - we increment all state IDs in the states and in the arcs -
 * we add a new zero'th state which transitions via &lt;s&gt;:&lt;s&gt; to the
 * (new) first state
 * 
 * @author Johannes Twiefel, Timo Baumann
 */
public class SequiturImport {

    @XmlRootElement(name = "fsa")
    public static class FSA {
        @XmlAttribute
        String semiring;
        @XmlAttribute
        int initial; // first real state
        @XmlElement(name = "input-alphabet")
        Alphabet inputAlphabet;
        @XmlElement(name = "output-alphabet")
        Alphabet outputAlphabet;
        @XmlElement(name = "state")
        List<State> states;
        transient List<edu.cmu.sphinx.fst.State> openFstStates;
        transient Semiring ring = new TropicalSemiring();

        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            // might also work with other formats, but we have never seen any
            // other format
            assert "tropical".equals(semiring);
            // add a new initial state that transduces <s>:<s> and transitions
            // to the first real state
            State initialState = new State();
            initialState.id = 0;
            Arc initialArc = new Arc();
            initialArc.in = inputAlphabet.symbols.size() - 1;
            initialArc.out = outputAlphabet.symbols.size() - 1;
            initialArc.target = initial + 1;
            initialArc.weight = ring.one();
            initialState.arcs = Collections.<Arc> singletonList(initialArc);
            states.add(initialState);
            // sort the states (to ascertain that initialState is the first
            // element)
            Collections.<State> sort(states, new Comparator<State>() {
                public int compare(State s1, State s2) {
                    return s1.id - s2.id;
                }
            });
        }

        /**
         * convert our object to the Sphinx OpenFst data structure
         * 
         * @return an edu.cmu.sphinx.fst.Fst built from the XML
         */
        public Fst toFst() {
            Fst openFst = new Fst(ring);
            openFst.setIsyms(inputAlphabet.toSymbols());
            openFst.setOsyms(outputAlphabet.toSymbols());
            openFstStates = new ArrayList<edu.cmu.sphinx.fst.State>(
                    states.size());
            for (State state : states) {
                edu.cmu.sphinx.fst.State openFstState = state
                        .toUnconnectedOpenFstState();
                openFst.addState(openFstState);
                assert openFstState.getId() == state.id;
                openFstStates.add(openFstState);
            }
            openFst.setStart(openFstStates.get(0));
            // second pass (now that all openFst states are created) to add all
            // the openFst arcs
            for (State state : states) {
                state.connectStates(openFstStates);
            }
            return openFst;
        }
    }

    public static class Alphabet {
        @XmlElement(name = "symbol")
        List<Symbol> symbols;

        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            Iterator<Symbol> it = symbols.iterator();
            while (it.hasNext()) {
                if (it.next().content.matches("__\\d+__"))
                    it.remove();
            }
            for (int i = 0; i < symbols.size(); i++) {
                assert symbols.get(i).index != null;
                assert symbols.get(i).index == i;
                symbols.get(i).index = null;
            }
            Symbol s = new Symbol();
            s.content = "<s>";
            symbols.add(s);
        }

        String[] toSymbols() {
            String[] out = new String[symbols.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = symbols.get(i).content;
            }
            return out;
        }
    }

    public static class Symbol {
        @XmlAttribute
        Integer index;
        @XmlMixed
        List<String> contentList;
        transient String content;

        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            assert contentList != null : "Error with symbol " + index;
            assert contentList.size() == 1 : "Error with symbol " + index;
            this.content = contentList.get(0);
            if (content.equals("__term__")) {
                content = "</s>";
            } else if (content.matches("__.+__")) {
                content = "<eps>";
            }
        }
    }

    public static class State {
        @XmlAttribute
        int id;
        @XmlElement(name = "final")
        Object finalState;
        @XmlElement
        Float weight;
        @XmlElement(name = "arc")
        List<Arc> arcs;

        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            id++; // increment state ID (because we add a new initial state 0
                  // transitioning via <s>:<s>)
        }

        /**
         * return a first approximation State which does not yet incoroporate
         * arcs
         */
        public edu.cmu.sphinx.fst.State toUnconnectedOpenFstState() {
            return new edu.cmu.sphinx.fst.State(weight != null ? weight : 0.f);
        }

        /**
         * add arcs to the state now that all states are available as possible
         * targets
         */
        public void connectStates(List<edu.cmu.sphinx.fst.State> openFstStates) {
            if (arcs != null)
                for (Arc arc : arcs) {
                    edu.cmu.sphinx.fst.Arc openFstArc = arc
                            .toOpenFstArc(openFstStates);
                    openFstStates.get(id).addArc(openFstArc);
                }
        }
    }

    public static class Arc {
        @XmlAttribute
        int target;
        @XmlElement
        int in; // automatically set to 0 (which corresponds to epsilon) if not
                // set in XML
        @XmlElement
        int out; // automatically set to 0 (which corresponds to epsilon) if not
                 // set in XML
        @XmlElement
        float weight;

        public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            target++; // increment state ID (because we add a new initial state
                      // 0 transitioning via <s>:<s>)
        }

        public edu.cmu.sphinx.fst.Arc toOpenFstArc(
                List<edu.cmu.sphinx.fst.State> openFstStates) {
            return new edu.cmu.sphinx.fst.Arc(in, out, weight,
                    openFstStates.get(target));
        }
    }

    /**
     * Load a Sequitur FSA in XML format and store it in Sphinx' OpenFst
     * binary/serialized format.
     * 
     * @param args
     *            filename of input file, filename of output file
     * @throws JAXBException
     *             indicating that XML could not be read
     * @throws IOException
     *             indicating that file-handling does not work
     */
    public static void main(String... args) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(FSA.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        FSA fsa = (FSA) unmarshaller.unmarshal(new File(args[0]));
        edu.cmu.sphinx.fst.Fst fst = fsa.toFst();
        fst.saveModel(args[1]);
        System.out
                .println("The Sequitur G2P XML-formatted FST "
                        + args[0]
                        + " has been converted to Sphinx' OpenFst binary format in the file "
                        + args[1]);
        // uncomment this to test your model:
        // edu.cmu.sphinx.linguist.g2p.G2PConverter d = new
        // edu.cmu.sphinx.linguist.g2p.G2PConverter(args[1]);
        // List<edu.cmu.sphinx.linguist.g2p.Path> path =
        // d.phoneticize("wahnsinn", 5);
        // System.err.println(path);
    }
}
