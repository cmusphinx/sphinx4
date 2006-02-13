package edu.cmu.sphinx.tools.corpusEditor;

/**
 * Waveform =
 * WaveformFile
 * BitsPerSample
 * SamplesPerSecond
 * ChannelCount
 */
public class Waveform {

    Integer bitsPerSample;
    Integer samplesPerSecond;
    Integer channelCount;

    public Waveform() {
    }

    public Waveform(Integer bitsPerSample, Integer samplesPerSecond, Integer channelCount) {
        this.bitsPerSample = bitsPerSample;
        this.samplesPerSecond = samplesPerSecond;
        this.channelCount = channelCount;
    }


}
