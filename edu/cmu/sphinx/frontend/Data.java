/**
 * [[[copyright]]]
 */

package edu.cmu.sphinx.frontend;

import java.io.Serializable;

/**
 * Data can be event signals or just data to be processed, and they go
 * through the processors in the front-end pipeline. Signals can carry
 * data like audio, preemphasized audio data, cepstra, etc.. Signals
 * can also be used to indicate events like beginning/end of audio
 * segment, data dropped, quality changed, etc..
 */
public interface Data extends Serializable {}
