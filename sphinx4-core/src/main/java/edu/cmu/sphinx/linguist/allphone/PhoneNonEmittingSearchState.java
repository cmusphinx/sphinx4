package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.Unit;

public class PhoneNonEmittingSearchState implements SearchState, SearchStateArc {
    
    protected Unit unit;
    protected AllphoneLinguist linguist;
    private float insertionProb;
    private float languageProb;
    
    public PhoneNonEmittingSearchState(Unit unit, AllphoneLinguist linguist, float insertionProb, float languageProb) {
        this.unit = unit;
        this.linguist = linguist;
        this.insertionProb = insertionProb;
        this.languageProb = languageProb;
    }
    
    public SearchStateArc[] getSuccessors() {
        SearchStateArc[] result = new SearchStateArc[1];
        result[0] = new PhoneWordSearchState(unit, linguist, insertionProb, languageProb);
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
        return languageProb;
    }

    public float getInsertionProbability() {
        return insertionProb;
    }

    public Object getLexState() {
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhoneNonEmittingSearchState))
            return false;
        boolean haveSameBaseId = ((PhoneNonEmittingSearchState)obj).unit.getBaseID() == unit.getBaseID();
        boolean haveSameContex = ((PhoneNonEmittingSearchState)obj).unit.getContext().equals(unit.getContext());
        return haveSameBaseId && haveSameContex;
    }
    
    @Override
    public int hashCode() {
        return unit.getContext().hashCode() * 91 + unit.getBaseID();
    }
}
