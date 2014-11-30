package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;

public class AllphoneSearchGraph implements SearchGraph {

    private AcousticModel acousticModel;
    
    public AllphoneSearchGraph(AcousticModel model) {
        this.acousticModel = model;
    }
    
    public SearchState getInitialState() {
        HMMState silHmmState = acousticModel.lookupNearestHMM(UnitManager.SILENCE, HMMPosition.UNDEFINED, true).getInitialState();
        return new PhoneHmmSearchState(UnitManager.SILENCE, silHmmState, acousticModel);
    }

    public int getNumStateOrder() {
        return 2;
    }
    
    public boolean getWordTokenFirst() {
        return false;
    }
}
