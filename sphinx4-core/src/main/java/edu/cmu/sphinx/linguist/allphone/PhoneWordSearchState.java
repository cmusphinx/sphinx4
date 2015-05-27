package edu.cmu.sphinx.linguist.allphone;

import java.util.ArrayList;

import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.LeftRightContext;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;

public class PhoneWordSearchState extends PhoneNonEmittingSearchState implements WordSearchState {
    
    public PhoneWordSearchState(Unit unit, AllphoneLinguist linguist, float insertionProb, float languageProb) {
        super(unit, linguist, insertionProb, languageProb);
    }
    
    public SearchStateArc[] getSuccessors() {
        ArrayList<SearchStateArc> result = new ArrayList<SearchStateArc>();
        Unit rc = UnitManager.SILENCE;
        Unit base = unit.getBaseUnit();
        if (unit.isContextDependent())
            rc = ((LeftRightContext)unit.getContext()).getRightContext()[0];
        ArrayList<HMM> successors = linguist.useContextDependentPhones() ? linguist.getCDSuccessors(base, rc) : linguist.getCISuccessors();
        for (HMM successor : successors)
            result.add(new PhoneHmmSearchState(successor.getInitialState(), linguist, linguist.getPhoneInsertionProb(), LogMath.LOG_ONE));
        return result.toArray(new SearchStateArc[result.size()]);
    }

    public boolean isFinal() {
        return true;
    }

    public Pronunciation getPronunciation() {
        Unit[] pronUnits = new Unit[1];
        pronUnits[0] = unit;
        Pronunciation p = new Pronunciation(pronUnits, "", 1.0f);
        p.setWord(new Word(unit.getName(), null, false));
        return p;
    }
    
    public boolean isWordStart() {
        return false;
    }

    public int getOrder() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhoneWordSearchState))
            return false;
        boolean haveSameBaseId = ((PhoneWordSearchState)obj).unit.getBaseID() == unit.getBaseID();
        boolean haveSameContex = ((PhoneWordSearchState)obj).unit.getContext().equals(unit.getContext());
        return haveSameBaseId && haveSameContex;
    }
    
    @Override
    public int hashCode() {
    	return unit.getContext().hashCode() * 91 + unit.getBaseID();
    }
}
