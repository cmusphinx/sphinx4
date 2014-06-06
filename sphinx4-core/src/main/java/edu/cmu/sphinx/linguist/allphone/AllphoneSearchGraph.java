package edu.cmu.sphinx.linguist.allphone;

import edu.cmu.sphinx.linguist.SearchGraph;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.acoustic.AcousticModel;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;

public class AllphoneSearchGraph implements SearchGraph {

    private AcousticModel acousticModel;
    
    public AllphoneSearchGraph(AcousticModel model) {
        this.acousticModel = model;
    }
    
    public SearchState getInitialState() {
        return new PhoneSearchState(UnitManager.SILENCE, acousticModel);
    }

    public int getNumStateOrder() {
        return 2;
    }

}
