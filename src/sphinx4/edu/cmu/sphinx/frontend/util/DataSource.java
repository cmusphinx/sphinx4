package edu.cmu.sphinx.frontend.util;

/**
 * Descibes which functionality is necessary to be a DataSource, which include starting, pausing, continueing and
 * stopping data-acquisition.
 *
 * @author Holger Brandl
 */
public interface DataSource {

    public void startDataAcquisition();


    public void pauseDataAcquisition();


    public void continueDataAcquisition();


    public void stopDataAcquisition();
}
