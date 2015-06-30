package edu.cmu.sphinx.demo.speakerid;

import java.net.URL;
import java.util.ArrayList;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import edu.cmu.sphinx.decoder.adaptation.Stats;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.speakerid.Segment;
import edu.cmu.sphinx.speakerid.SpeakerCluster;
import edu.cmu.sphinx.speakerid.SpeakerIdentification;
import edu.cmu.sphinx.util.TimeFrame;

public class SpeakerIdentificationDemo {

    /**
     * Returns string version of the given time in milliseconds
     * 
     * @param milliseconds time in milliseconds
     * @return time in format mm:ss
     */
    public static String time(int milliseconds) {
        return (milliseconds / 60000) + ":"
                + (Math.round((double) (milliseconds % 60000) / 1000));
    }

    /**
     * 
     * @param speakers
     *            An array of clusters for which it is needed to be printed the
     *            speakers intervals
     * @param fileName
     *            THe name of file we are processing
     */
    public static void printSpeakerIntervals(
            ArrayList<SpeakerCluster> speakers, String fileName) {
        int idx = 0;
        for (SpeakerCluster spk : speakers) {
            idx++;
            ArrayList<Segment> segments = spk.getSpeakerIntervals();
            for (Segment seg : segments)
                System.out.println(fileName + " " + " "
                        + time(seg.getStartTime()) + " "
                        + time(seg.getLength()) + " Speaker" + idx);
        }
    }

    /**
     * @param speakers
     *            An array of clusters for which it is needed to get the
     *            speakers intervals for decoding with per-speaker adaptation
     *            with diarization.
     * @param url
     *            Url for the audio
     * @throws Exception if something went wrong
     */
    public static void speakerAdaptiveDecoding(ArrayList<SpeakerCluster> speakers,
            URL url) throws Exception {

        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration
                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(
                configuration);

        TimeFrame t;
        SpeechResult result;

        for (SpeakerCluster spk : speakers) {
            Stats stats = recognizer.createStats(1);
            ArrayList<Segment> segments = spk.getSpeakerIntervals();

            for (Segment s : segments) {
                long startTime = s.getStartTime();
                long endTime = s.getStartTime() + s.getLength();
                t = new TimeFrame(startTime, endTime);

                recognizer.startRecognition(url.openStream(), t);
                while ((result = recognizer.getResult()) != null) {
                    stats.collect(result);
                }
                recognizer.stopRecognition();
            }

            Transform profile;
            // Create the Transformation
            profile = stats.createTransform();
            recognizer.setTransform(profile);

            for (Segment seg : segments) {
                long startTime = seg.getStartTime();
                long endTime = seg.getStartTime() + seg.getLength();
                t = new TimeFrame(startTime, endTime);

                // Decode again with updated SpeakerProfile
                recognizer.startRecognition(url.openStream(), t);
                while ((result = recognizer.getResult()) != null) {
                    System.out.format("Hypothesis: %s\n",
                            result.getHypothesis());
                }
                recognizer.stopRecognition();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SpeakerIdentification sd = new SpeakerIdentification();
        URL url = SpeakerIdentificationDemo.class.getResource("test.wav");
        ArrayList<SpeakerCluster> clusters = sd.cluster(url.openStream());

        printSpeakerIntervals(clusters, url.getPath());
        speakerAdaptiveDecoding(clusters, url);
    }
}
