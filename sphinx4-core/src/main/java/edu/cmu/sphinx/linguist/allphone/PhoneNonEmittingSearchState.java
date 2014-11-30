package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.util.LogMath;

public class PhoneNonEmittingSearchState implements SearchState, SearchStateArc {
    
    protected Unit unit;
    protected AcousticModel acousticModel;
    
    public PhoneNonEmittingSearchState(Unit unit, AcousticModel model) {
        this.unit = unit;
        this.acousticModel = model;
    }
    
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] result = new SearchStateArc[1];
        result[0] = new PhoneWordSearchState(unit, acousticModel);
        return result;
    }

    public boolean isEmitting() {
        return false;
    }

    public boolean isFinal() {
        return false;
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
        return this;
    }

    public float getProbability() {
        return getLanguageProbability() + getInsertionProbability();
    }

    public float getLanguageProbability() {
        return LogMath.LOG_ONE;
    }

    public float getInsertionProbability() {
        return LogMath.LOG_ONE;
    }

    public Object getLexState() {
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhoneNonEmittingSearchState))
            return false;
        boolean haveSameBaseId = ((PhoneNonEmittingSearchState)obj).unit.getBaseID() == unit.getBaseID();
        return haveSameBaseId;
    }
    
    @Override
    public int hashCode() {
        return unit.getBaseID() * 37;
    }
}
