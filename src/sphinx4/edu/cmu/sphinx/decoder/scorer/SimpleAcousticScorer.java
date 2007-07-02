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

package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * A Simple acoustic scorer. a certain number of frames have been processed
 * <p/>
 * Note that all scores are maintained in LogMath log base.
 */
public class SimpleAcousticScorer implements AcousticScorer {


    /** Property the defines the frontend to retrieve features from for scoring */
    @S4Component(type = BaseDataProcessor.class)
    public final static String PROP_FRONTEND = "frontend";


    /** Propertry that defines whether scores are normalized or not */
    @S4Boolean(defaultValue = false)
    public final static String PROP_NORMALIZE_SCORES = "normalizeScores";

    /** Default value for PROP_NORMALIZE_SCORES */

    public final static boolean PROP_NORMALIZE_SCORES_DEFAULT = false;

    // ------------------------------
    // configuration data
    // -----------------------------
    private String name;
    private FrontEnd frontEnd;
    private boolean normalizeScores;


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        frontEnd = (FrontEnd) ps.getComponent(PROP_FRONTEND);
        normalizeScores = ps.getBoolean(PROP_NORMALIZE_SCORES);
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.util.props.Configurable#getName()
    */
    public String getName() {
        return name;
    }


    /** Starts the scorer */
    public void startRecognition() {
    }


    /**
     * Scores the given set of states
     *
     * @param scoreableList a list containing scoreable objects to be scored
     * @return true if there was a Data available to score false if there was no more Data available to score
     */
    public Scoreable calculateScores(List<Token> scoreableList) {

        if (scoreableList.size() <= 0) {
            return null;
        }

        Scoreable best = null;

        try {
            Data data = frontEnd.getData();

            while (data instanceof Signal) {
                if(data instanceof SpeechEndSignal)
                    return null;
                
                data = frontEnd.getData();
            }

            if (data == null)
                return null;

            if (data instanceof DoubleData)
                data = DataUtil.DoubleData2FloatData((DoubleData) data);

            best = scoreableList.get(0);

            for (Iterator<Token> i = scoreableList.iterator(); i.hasNext();) {
                Scoreable scoreable = i.next();
                /*
		if (scoreable.getFrameNumber() != data.getID()) {
		    throw new Error
			("Frame number mismatch: Token: " + 
			 scoreable.getFrameNumber() +
			 "  Data: " + data.getID());
		}
                */
                //TODO: programmable gain
                if (scoreable.calculateScore(data, false, 1.0f) >
                        best.getScore()) {
                    best = scoreable;
                }
            }

            if (normalizeScores) {
                for (Iterator<Token> i = scoreableList.iterator(); i.hasNext();) {
                    Scoreable scoreable = i.next();
                    scoreable.normalizeScore(best.getScore());
                }
            }
        } catch (DataProcessingException dpe) {
            dpe.printStackTrace();
            return best;
        }

        return best;
    }


    /** Performs post-recognition cleanup. */
    public void stopRecognition() {
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.decoder.scorer.AcousticScorer#allocate()
    */
    public void allocate() throws IOException {
    }


    /* (non-Javadoc)
    * @see edu.cmu.sphinx.decoder.scorer.AcousticScorer#deallocate()
    */
    public void deallocate() {
    }


}
