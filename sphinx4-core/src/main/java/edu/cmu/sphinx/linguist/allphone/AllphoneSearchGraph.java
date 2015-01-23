package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.HMMState;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.util.LogMath;

public class AllphoneSearchGraph implements SearchGraph {

    private AllphoneLinguist linguist;
    
    public AllphoneSearchGraph(AllphoneLinguist linguist) {
        this.linguist = linguist;
    }
    
    public SearchState getInitialState() {
        HMMState silHmmState = linguist.getAcousticModel().lookupNearestHMM(UnitManager.SILENCE, HMMPosition.UNDEFINED, true).getInitialState();
        return new PhoneHmmSearchState(silHmmState, linguist, LogMath.LOG_ONE, LogMath.LOG_ONE);
    }

    public int getNumStateOrder() {
        return 2;
    }
    
    public boolean getWordTokenFirst() {
        return false;
    }
}
