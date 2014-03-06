package edu.cmu.sphinx.demo.speakerid;

import java.io.IOException;
import java.util.ArrayList;
import edu.cmu.sphinx.speakerid.*;

public class SpeakerIdentificationDemo {

    /**
     * Returns string version of the given time in miliseconds 
     * @param seconds
     * @return time in format mm:ss
     */
    public static String time(int seconds) {
        return (seconds / 60000) + ":" + (Math.round((double) (seconds % 60000) / 1000));
    }

    /**
     * 
     * @param speakers
     *            An array of clusters for which it is needed to be printed the
     *            speakers intervals
     * @throws IOException
     */
    public static void printSpeakerIntervals(ArrayList<SpeakerCluster> speakers, String fileName)
            throws IOException {
        int idx = 0;
        for (SpeakerCluster spk : speakers) {
            idx++;
            ArrayList<Segment> segments = spk.getSpeakerIntervals();
            for (Segment seg : segments)
                System.out.println(fileName + " " + " " + time(seg.getStartTime()) + " "
                        + time(seg.getLength()) + " Speaker" + idx);
        }
    }

    public static void main(String[] args) throws IOException {
        String inputFile = "src/apps/edu/cmu/sphinx/demo/speakerid/test.wav";
        SpeakerIdentification sd = new SpeakerIdentification();
        ArrayList<SpeakerCluster> clusters = sd.cluster(inputFile);
        printSpeakerIntervals(clusters, inputFile);
    }
}
