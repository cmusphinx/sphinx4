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


package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Cepstrum;
import edu.cmu.sphinx.frontend.CepstrumSource;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.Signal;

import edu.cmu.sphinx.frontend.util.EnergyPlotter;

import edu.cmu.sphinx.util.SphinxProperties;

import java.io.IOException;

import java.util.*;


/**
 * Implements an energy-based endpointer that is based on a simple
 * two event windowing algorithm.
 *
 * This endpointer looks at the energy levels of the signal,
 * given by input Cepstrum, to determine the speech start and
 * speech end. The Signals, Signal.SPEECH_START and Signal.SPEECH_END,
 * are inserted at the speech endpoints.
 *
 * This endpointer maintains several configurable parameters:
 * <pre>
 * edu.cmu.sphinx.frontend.EnergyEndpointer.startLow
 * edu.cmu.sphinx.frontend.EnergyEndpointer.startHigh
 * edu.cmu.sphinx.frontend.EnergyEndpointer.endLow
 * </pre>
 *
 * The above parameters are usually set by the user.
 * <pre>
 * edu.cmu.sphinx.frontend.EnergyEndpointer.startWindow
 * edu.cmu.sphinx.frontend.EnergyEndpointer.startOffset
 * edu.cmu.sphinx.frontend.EnergyEndpointer.endWindow
 * edu.cmu.sphinx.frontend.EnergyEndpointer.endOffset
 *
 * edu.cmu.sphinx.frontend.EnergyEndpointer.maxDropout
 * </pre>
 * When the energy level is above <code>startHigh</code> for
 * <code>startWindow</code> number of frames, then speech has started.
 * Speech start will be <code>startOffset</code> number of frames
 * before speech went to or above <code>startLow</code>.
 *
 * <p>When the energy level is below <code>endLow</code> for
 * <code>endWindow</code> number of frames, then speech has ended.
 * Speech end will be <code>endOffset</code> number of frames after
 * speech went below <code>endLow</code>.
 *
 * <p>The <code>maxDropout</code> parameter deals with spikes before
 * speech starts. If the energy previously when above startLow, but
 * returns to below <code>startLow</code> without entering speech,
 * then it is a spike. If the spike is less than <code>maxDropout</code>
 * number of frames from speech, the spike is part of speech.
 * Otherwise, it is not part of speech.
 *
 * <p>At the end of speech side, spikes are always consider part of
 * speech unless it is contiguously more than <code>endWindow</code>
 * frames from speech.
 */
public class EnergyEndpointer extends DataProcessor implements Endpointer {


    private static final String PROP_PREFIX = 
    "edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.";


    /**
     * The SphinxProperty for the energy level which is a lower bound
     * threshold at the energy is considered as speech.
     */
    public static final String PROP_START_LOW = PROP_PREFIX + "startLow";


    /**
     * The default value for PROP_START_LOW.
     */
    public static final float PROP_START_LOW_DEFAULT = 0.0f;
        

    /**
     * The SphinxProperty for the starting energy level if stayed above
     * for more that PROP_START_WINDOW frames, the signal is considered speech.
     */
    public static final String PROP_START_HIGH = PROP_PREFIX + "startHigh";
    

    /**
     * The default value for PROP_START_HIGH.
     */
    public static final float PROP_START_HIGH_DEFAULT = 0.0f;
       

    /**
     * The SphinxProperty for the number of frames staying about
     * PROP_START_HIGH energy levels for the signal to be considered speech.
     */
    public static final String PROP_START_WINDOW = PROP_PREFIX + "startWindow";

    
    /**
     * The default value for PROP_START_WINDOW.
     */
    public static final int PROP_START_WINDOW_DEFAULT = 3;


    /**
     * The SphinxProperty for the number frames before the energy 
     * level goes above PROP_START_LOW that is consider start of speech.
     */
    public static final String PROP_START_OFFSET = PROP_PREFIX + "startOffset";

    
    /**
     * The default value for PROP_START_OFFSET.
     */
    public static final int PROP_START_OFFSET_DEFAULT = 5;


    /**
     * The SphinxProperty for the ending energy level, below which
     * the signal is considered non-speech.
     */
    public static final String PROP_END_LOW = PROP_PREFIX + "endLow";


    /**
     * The default value for PROP_END_LOW.
     */
    public static final float PROP_END_LOW_DEFAULT = 0.0f;


    /**
     * The SphinxProperty for the number of frames the energy level
     * stayed below PROP_END_LOW level, for the signal to be considered
     * end of speech.
     */
    public static final String PROP_END_WINDOW = PROP_PREFIX + "endWindow";


    /**
     * The default value for PROP_END_WINDOW.
     */
    public static final int PROP_END_WINDOW_DEFAULT = 30;


    /**
     * The SphinxProperty for the number of frames after energy goes
     * below PROP_END_LOW level that is considered the end of speech.
     */
    public static final String PROP_END_OFFSET = PROP_PREFIX + "endOffset";


    /**
     * The default value for PROP_END_OFFSET.
     */
    public static final int PROP_END_OFFSET_DEFAULT = 10;


    /**
     * The SphinxProperty that specifies how many frames is a spike
     * away from speech for it to be considered part of speech.
     */
    public static final String PROP_MAX_DROPOUT = PROP_PREFIX + "maxDropout";


    /**
     * The default value for PROP_MAX_DROPOUT.
     */
    public static final int PROP_MAX_DROPOUT_DEFAULT = 10;


    /**
     * The SphinxProperty that specifies whether to print out the
     * energy values, and the speech and utterance endpoints.
     */
    public static final String PROP_PLOT_ENERGY = PROP_PREFIX + "plotEnergy";
   

    /**
     * The default value for PROP_PLOT_ENERGY.
     */
    public static final boolean PROP_PLOT_ENERGY_DEFAULT = false;


    /**
     * If energy is greater than startHigh for startWindow frames,
     * speech has started.
     */
    private int startWindow;

    /**
     * The number of frames before energy goes above startLow
     * that a SPEECH_START is inserted.
     */
    private int startOffset;

    /**
     * If energy is below endLow for endWindow frames, speech has ended.
     */
    private int endWindow;

    /**
     * The number of frames after energy goes below endLow that
     * a SPEECH_END is inserted.
     */
    private int endOffset;

    /**
     * maxDropout is the maximum number of frames with energy below
     * endLow before speech is considered ending.
     */
    private int maxDropout;
    

    private float startLow;   // lower bound for the start window
    private float startHigh;  // upper bound for the start window    
    private float endLow;     // lower bound for the end window

    // location values: below startLow, between low and high, above high
    private static final int BELOW_START_LOW = 1;
    private static final int BETWEEN_START_LOW_HIGH = 2;
    private static final int ABOVE_START_HIGH = 3;

    private CepstrumSource predecessor;  // where to pull Cepstra from
    private LinkedList outputQueue;      // where to cache the read Cepstra

    /**
     * The last frame before speech went above startLow.
     */
    private Cepstrum lastStartLowFrame;

    /**
     * The frame when speech stayed below endLow for endOffset frames.
     */
    private Cepstrum endOffsetFrame;

    /**
     * The last SPEECH_END Cepstrum inserted into the stream.
     */
    private Cepstrum lastSpeechEndFrame;

    private boolean plotEnergy;    // print the energy values to stdout
    private boolean inSpeech;       // are we in a speech region?
    private float lastEnergy;       // previous energy value
    private int location;           // which part of hill are we at?
    private int startLowFrames;     // # of contiguous frames energy < startLow
    private int startHighFrames;    // # of frames with energy > startHigh
    private int endLowFrames;       // # of frames with energy < endLow
    private int numSpeechSegments;  // # of speech segments in this utterance
    private int numCepstra;         // # of Cepstra in this Utterance
    private EnergyPlotter plotter;  // for plotting energy to System.out


    /**
     * Initializes this EnergyEndpointer with the given name, context,
     * and CepstrumSource predecessor.
     *
     * @param name the name of this EnergyEndpointer
     * @param context the context of the SphinxProperties this
     *    EnergyEndpointer uses
     * @param props the SphinxProperties to read properties from
     * @param predecessor the CepstrumSource where this EnergyEndpointer
     *    gets Cepstrum from
     *
     * @throws java.io.IOException
     */
    public void initialize(String name, String context, 
                           SphinxProperties props,
                           CepstrumSource predecessor) throws IOException {
        super.initialize(name, context, props);
        setProperties();
        this.predecessor = predecessor;
        this.outputQueue = new LinkedList();
        plotter = new EnergyPlotter(props);
        reset();
    }


    /**
     * Reads the parameters needed from the static SphinxProperties object.
     */
    private void setProperties() {
        SphinxProperties properties = getSphinxProperties();
        startLow
            = properties.getFloat(PROP_START_LOW, PROP_START_LOW_DEFAULT);
        startHigh
            = properties.getFloat(PROP_START_HIGH, PROP_START_HIGH_DEFAULT);
        startWindow
            = properties.getInt(PROP_START_WINDOW, PROP_START_WINDOW_DEFAULT);
        startOffset
            = properties.getInt(PROP_START_OFFSET, PROP_START_OFFSET_DEFAULT);
        endLow
            = properties.getFloat(PROP_END_LOW, PROP_END_LOW_DEFAULT);
        endWindow
            = properties.getInt(PROP_END_WINDOW, PROP_END_WINDOW_DEFAULT);
        endOffset
            = properties.getInt(PROP_END_OFFSET, PROP_END_OFFSET_DEFAULT);
        maxDropout
            = properties.getInt(PROP_MAX_DROPOUT, PROP_MAX_DROPOUT_DEFAULT);
        plotEnergy
            = properties.getBoolean(PROP_PLOT_ENERGY, 
                                    PROP_PLOT_ENERGY_DEFAULT);
    }


    /**
     * Reset all the variables to their initial values.
     *
     * TODO: Currently, not all the variables are reset.
     */
    private void reset() {
        inSpeech = false;
        lastEnergy = 0;
        location = BELOW_START_LOW;
        startLowFrames = 0;
        startHighFrames = 0;
        endLowFrames = 0;
        numSpeechSegments = 0;
        numCepstra = 0;
    }


    /**
     * Returns the next Cepstrum, which can be either Cepstrum with
     * data, or Cepstrum with a SPEECH_START or SPEECH_END signal.
     *
     * @return the next Cepstrum, or null if no Cepstrum is available
     *
     * @throws java.io.IOException if there is error reading the
     *    Cepstrum object
     *
     * @see Cepstrum
     */
    public Cepstrum getCepstrum() throws IOException {
       
        Cepstrum cepstrum = readCepstrum();
        
        getTimer().start();
        
        if (cepstrum != null) {
            
            Signal signal = cepstrum.getSignal();
            
            if (signal != null && signal.equals(Signal.UTTERANCE_START)) {
                utteranceStart();
                outputQueue.addFirst(cepstrum);
                // read ahead until we have reach the start of speech
                while (cepstrum.getEnergy() < startHigh ||
                       startHighFrames <= startWindow) {
                    cepstrum = readCepstrum();
                    if (cepstrum != null) {
                        outputQueue.addFirst(cepstrum);
                    } else {
                        break;
                    }
                }
            } else if (signal != null && signal.equals(Signal.UTTERANCE_END)) {
                if (inSpeech) {
                    speechEnd();
                }
                outputQueue.addFirst(cepstrum);
            } else {
                // add the new cepstrum to outputQueue
                outputQueue.addFirst(cepstrum);
            }
        }
        
        if (outputQueue.size() > 0) {
            cepstrum = (Cepstrum) outputQueue.removeLast();
        } else {
            cepstrum = null;
        }
        
        if (plotEnergy) {
            plotter.plot(cepstrum);
        }

        getTimer().stop();

        return cepstrum;
    }


    /**
     * Reads a Cepstrum from the predecessor, and use it to do endpointing.
     *
     * @return the Cepstrum read from the predecessor
     */
    private Cepstrum readCepstrum() throws IOException {
        Cepstrum cepstrum = predecessor.getCepstrum();

        if (cepstrum != null) {
            numCepstra++;
            if (cepstrum.hasContent()) {
                // Call a different method to handle the Cepstrum
                // depending on its energy level.
                if (cepstrum.getEnergy() < startLow || 
                    cepstrum.getEnergy() < endLow) {
                    processLowEnergyCepstrum(cepstrum);
                } else if (startLow <= cepstrum.getEnergy() &&
                           cepstrum.getEnergy() <= startHigh) {
                    processMediumEnergyCepstrum(cepstrum);
                } else if (cepstrum.getEnergy() > startHigh) {
                    processHighEnergyCepstrum(cepstrum);
                }
            }
        }
        
        return cepstrum;
    }


    /**
     * Process the given low energy Cepstrum.
     *
     * @param cepstrum the low energy Cepstrum to process
     */
    private void processLowEnergyCepstrum(Cepstrum cepstrum) {
        if (inSpeech) {
            // If we in a speech segment, but encounters a low energy
            // cepstrum, this can be the end of the speech segment.
            // This can be confirmed by (endLowFrames > endWindow).

            if (endLowFrames == endOffset) {
                endOffsetFrame = cepstrum;
            } else if (endLowFrames > endWindow) {
                speechEnd();
            }
            endLowFrames++;
        } else {
            if (startLowFrames > 1) {
                startHighFrames = 0;
            }
            startLowFrames++;
        }

        if (cepstrum.getEnergy() < startLow) {
            location = BELOW_START_LOW;
        }
    }


    /**
     * Process the given medium energy Cepstrum.
     *
     * @param cepstrum the medium energy Cepstrum to process
     */
    private void processMediumEnergyCepstrum(Cepstrum cepstrum) {
        if (location == BELOW_START_LOW && !inSpeech) {
            if (lastStartLowFrame != null) {
                // If we are below startLow, and lastStartLowFrame
                // was not null, we just had a dropout (or spike).
                // If the dropout was longer than maxDropout,
                // then the lastStartLowFrame should be reset.

                if (maxDropout < startLowFrames &&
                    outputQueue.size() > 0) {
                    // first cepstrum in outputQueue is the most recent
                    setLastStartLowFrame((Cepstrum) outputQueue.getFirst());
                }
            } else { // if lastStartLowFrame == null
                if (numCepstra == 1) {
                    // if this is the first frame
                    setLastStartLowFrame(cepstrum);
                } else if (numCepstra > 1) {
                    // first cepstrum in outputQueue is the most recent
                    setLastStartLowFrame((Cepstrum) outputQueue.getFirst());
                }
            }
        }
        startLowFrames = 0;
        endLowFrames = 0;
        location = BETWEEN_START_LOW_HIGH;
    }
   

    /**
     * Process the given high energy Cepstrum.
     *
     * @param cepstrum the high energy Cepstrum to process
     */
    private void processHighEnergyCepstrum(Cepstrum cepstrum) {
        if (location == BELOW_START_LOW) {
            if (outputQueue.size() > 0) {
                setLastStartLowFrame((Cepstrum) outputQueue.getFirst());
            }
        }
        // if energy is greater than startHigh for 
        // more than startWindow frames

        if (startHighFrames > startWindow) {
            if (!inSpeech) {
                speechStart();
            } else {
                endLowFrames = 0;
                endOffsetFrame = null;
            }
        } else {
            startHighFrames++;
        }
        location = ABOVE_START_HIGH;
        endLowFrames = 0;
    }


    /**
     * Sets the lastStartLowFrame.
     *
     * @param cepstrum the Cepstrum that will be the lastStartLowFrame
     */
    private void setLastStartLowFrame(Cepstrum cepstrum) {
        lastStartLowFrame = cepstrum;
    }


    /**
     * What happens when an UTTERANCE_START is encountered.
     */
    private void utteranceStart() {
        reset();
        setLastStartLowFrame(null);
        endOffsetFrame = null;
    }


    /**
     * What happens when speech starts.
     */
    private void speechStart() {
        insertSpeechStart();
        setLastStartLowFrame(null);
        inSpeech = true;
        endLowFrames = 0;
        endOffsetFrame = null;
        numSpeechSegments++;
    }


    /**
     * What should happen when speech ends.
     */
    private void speechEnd() {
        insertSpeechEnd();
        inSpeech = false;
        endLowFrames = 0;
        startHighFrames = 0;
    }

    
    /**
     * Inserts a SPEECH_START at the appropriate place.
     */
    private void insertSpeechStart() {
        // "index" is where we should insert the SPEECH_START
        int index = outputQueue.indexOf(lastStartLowFrame);

        // Go back startOffset frames, but check if we have hit
        // an UTTERANCE_START or SPEECH_END, in which case we should
        // stop going back.
        
        if (index > -1) {
            int i = 0;
            for (ListIterator iterator = outputQueue.listIterator(index);
                 i < startOffset && iterator.hasNext(); i++) {
                Cepstrum cepstrum = (Cepstrum) iterator.next();
                if (cepstrum.hasSignal(Signal.UTTERANCE_START) ||
                    cepstrum.hasSignal(Signal.SPEECH_END)) {
                    break;
                }
            }
            index += i;
        }

        if (index >= outputQueue.size()) {
            index = outputQueue.size();
        } else if (index < 0) {
            // This probably means that there weren't any frames
            // below startLow so far for this Utterance. We just
            // go back and find the UTTERANCE_START.
            index = 0;
            
            System.out.println
                ("No frames under startLow so far, " +
                 "will insert SPEECH_START right after UTTERANCE_START.");
            
            int c = 0;
            for (ListIterator i = outputQueue.listIterator(); 
                 i.hasNext(); c++) {
                Cepstrum cepstrum = (Cepstrum) i.next();
                if (cepstrum.hasSignal(Signal.UTTERANCE_START)) {
                    index = c;
                }
            }
        }
        outputQueue.add(index, (new Cepstrum(Signal.SPEECH_START)));
    }

    
    /**
     * Inserts a SPEECH_END at the appropriate place.
     */
    private void insertSpeechEnd() {
        int index = outputQueue.indexOf(endOffsetFrame);
        endOffsetFrame = null;
        if (index < 0) {
            index = 0;
        }
        lastSpeechEndFrame = new Cepstrum(Signal.SPEECH_END);
        outputQueue.add(index, lastSpeechEndFrame);
    }


    /**
     * Removes the previous SPEECH_END Cepstrum.
     */
    private void removeLastSpeechEnd() {
        if (lastSpeechEndFrame != null) {
            outputQueue.remove(lastSpeechEndFrame);
        }
    }
}
