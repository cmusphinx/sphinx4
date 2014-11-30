package edu.cmu.sphinx.linguist.allphone;

import java.util.ArrayList;
import java.util.Iterator;

import edu.cmu.sphinx.linguist.SearchStateArc;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;

public class PhoneWordSearchState extends PhoneNonEmittingSearchState implements WordSearchState {
    
    public PhoneWordSearchState(Unit unit, AcousticModel model) {
        super(unit, model);
    }
    
    public SearchStateArc[] getSuccessors() {
        ArrayList<SearchStateArc> result = new ArrayList<SearchStateArc>();
        Iterator<Unit> iter = acousticModel.getContextIndependentUnitIterator();
        while( iter.hasNext()) {
            Unit ciUnit = iter.next();
            HMMState hmmState = acousticModel.lookupNearestHMM(ciUnit, HMMPosition.UNDEFINED, true).getInitialState();
            result.add(new PhoneHmmSearchState(ciUnit, hmmState, acousticModel));
        }
        return result.toArray(new SearchStateArc[result.size()]);
    }

    public boolean isFinal() {
        return true;
    }

    @Override
    public Pronunciation getPronunciation() {
        Unit[] pronUnits = new Unit[1];
        pronUnits[0] = unit;
        Pronunciation p = new Pronunciation(pronUnits, "", null, 1.0f);
        p.setWord(new Word(unit.getName(), null, false));
        return p;
    }
    
    @Override
    public boolean isWordStart() {
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PhoneWordSearchState))
            return false;
        boolean haveSameBaseId = ((PhoneWordSearchState)obj).unit.getBaseID() == unit.getBaseID();
        return haveSameBaseId;
    }
    
    @Override
    public int hashCode() {
        return unit.getBaseID() * 37;
    }
}
