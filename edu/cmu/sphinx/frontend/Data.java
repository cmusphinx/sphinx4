/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.Serializable;

/**
 * Data can be event signals or just data to be processed, and they go
 * through the processors in the front-end pipeline. Data can be
 * audio, preemphasized audio data, cepstra, etc.. Signals
 * can be used to indicate events like beginning/end of audio
 * segment, data dropped, quality changed, etc..
 */
public interface Data extends Serializable {}
