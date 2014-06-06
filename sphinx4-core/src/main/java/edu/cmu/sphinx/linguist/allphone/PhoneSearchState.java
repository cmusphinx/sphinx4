package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;

public class PhoneSearchState implements SearchState, SearchStateArc {
    
    private Unit unit;
    private AcousticModel acousticModel;
    
    public PhoneSearchState(Unit unit, AcousticModel model) {
        this.unit = unit;
        this.acousticModel = model;
    }
    
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] result = new SearchStateArc[1];
        result[0] = new PhoneHmmSearchState(unit, acousticModel.lookupNearestHMM(unit, HMMPosition.INTERNAL, true).getState(0), acousticModel);
        return result;
    }

    public boolean isEmitting() {
        return false;
    }

    public boolean isFinal() {
        return true;
    }

    public String toPrettyString() {
        return "Unit " + unit.toString();
    }

    public String getSignature() {
        return null;
    }

    public WordSequence getWordHistory() {
        return null;
    }

    public int getOrder() {
        return 0;
    }

    public SearchState getState() {
        return null;
    }

    public float getProbability() {
        return 0;
    }

    public float getLanguageProbability() {
        return 0;
    }

    public float getInsertionProbability() {
        return 0;
    }

    public Object getLexState() {
        return null;
    }

}
