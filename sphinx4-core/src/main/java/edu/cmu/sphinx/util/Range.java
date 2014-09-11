package edu.cmu.sphinx.util;

public class Range {

    private int start;
    private int end;
    
    public Range(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean contains(int shift) {
        return shift >= start && shift < end;
    }

    public int lowerEndpoint() {
        return start;
    }

    public int upperEndpoint() {
        return end;
    }

}
