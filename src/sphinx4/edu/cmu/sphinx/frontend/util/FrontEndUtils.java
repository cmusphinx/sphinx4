package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

/**
 * Some little helper methods to ease the handling of frontend-processor chains.
 *
 * @author Holger Brandl
 */
public class FrontEndUtils {


    /** Returns a the next <code>DataProcessor</code> of type <code>predecClass</code> which preceeds <code>dp</code> */
    public static DataProcessor getDataSource(DataProcessor dp, Class<? extends DataProcessor> predecClass) {
        while (!predecClass.isInstance(dp.getPredecessor())) {
            dp = dp.getPredecessor();

            if (dp == null)
                return null;

            if (dp instanceof FrontEnd)
                dp = ((FrontEnd) dp).getLastDataProcessor();
        }


        return dp;
    }


    /**
     * Determines the window shift-size used by the <code>RaisedCosineWindower</code> which precedes the given
     * <code>DataProcessor</code>.
     *
     * @param dp The <code>DataProcessor</code> which predecessors are searched backwards to find an instance of a
     *           <code>RaisedCosineWindower</code>
     * @return The window shift size of the found <code>RaisedCosineWindower</code>
     * @throws AssertionError if there was no <code>RaisedCosineWindower</code> within the predecessor list of
     *                        <code>dp</code>
     */
    public static float getWindowShiftMs(DataProcessor dp) {
        while (!(dp instanceof RaisedCosineWindower)) {
            dp = dp.getPredecessor();

            assert dp != null : "used FrontEnd does not contain a RaisedCosineWindower";

            if (dp instanceof FrontEnd)
                dp = ((FrontEnd) dp).getLastDataProcessor();
        }

        return ((RaisedCosineWindower) dp).getWindowShiftInMs();
    }
}
