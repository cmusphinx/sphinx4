/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.corpus;

import java.io.*;

/**
 * This a sample program that demonstrates the Corpus API and AudioRecorder API.  It implements
 * a trivial corpus collection.  It reads transcripts from a text file, with each transcript separated by a LF.
 * It then creates a Corpus with an Utterance for each transcript.   It then prompts the user with the
 * transcript and records 5 seconds of audio.  Finally, it saves the Corpus as an XML file.
 *
 * User: Peter Wolf
 * Date: Mar 9, 2006
 * Time: 10:05:04 PM
 */
public class CorpusRecorderSample {


    public static void main(String[] args) {
        try {

            if (args.length != 2) {
                throw new Error("USAGE: CorpusRecorderSample inputTranscriptFile outputCorpuXMLFile");
            }

            Corpus corpus = makeCorpusWithOnlyTrascripts(args[0]);

            for (Utterance utterance : corpus.getUtterances()) {
                recordAudioData(utterance);
            }

            FileOutputStream out = new FileOutputStream(args[1]);
            corpus.writeToXML(out);

        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Given a text file of transcripts, create a Corpus with Utterances that only contain transcripts.
     *
     * @param transcriptFile
     * @return the corpus
     * @throws IOException
     */
    private static Corpus makeCorpusWithOnlyTrascripts(String transcriptFile) throws IOException {

        // transcripts are stored as LF separated text
        LineNumberReader in = new LineNumberReader(new FileReader(transcriptFile));

        // create a new corpus
        Corpus corpus = new Corpus();

        // for each transcript in the file
        //   create a new utterance associated with that transcript
        //   add it to the corpus
        //   record audio associated with that utterance
        for (String transcript = in.readLine(); transcript != null; transcript = in.readLine()) {
            Utterance utterance = new Utterance();
            utterance.setTranscript(transcript);
            corpus.addUtterance(utterance);
            recordAudioData(utterance);
        }

        return corpus;

    }

    /**
     * Prompt the user with the transcript, and record 5 seconds of audio.
     * Also demonstrate the VUMeter API
     *
     * @param utterance
     */
    private static void recordAudioData(Utterance utterance) {

        // create a new AudioRecorder
        AudioRecorder recorder = new AudioRecorder();

        // the recorder will store the data in the specified file
        //
        // also open() allocates the audio device and starts the VUMeter running,
        // but does not record audio.  This allows the application to monitor the
        // sound level before recording
        recorder.open("utt" + utterance.hashCode() + ".pcm");

        // demonstrate the VU meter and sleep for 5 seconds
        sleepAndMonitorVUMeter(recorder, 5000);

        // prompt the user
        System.out.println("Please say: " + utterance.getTranscript());

        // start recording.  This starts streaming the audio to the specified file
        recorder.start();

        // demonstrate the VU meter and sleep for 5 seconds
        sleepAndMonitorVUMeter(recorder, 5000);

        // stop recording
        RegionOfAudioData data = recorder.stop();

        // associate the recorded audio with the utterance
        utterance.setRegionOfAudioData(data);

        // stop the VUMeter and release the audio device
        recorder.close();
    }

    /**
     * Demonstrate the VUMeter.  Rms is the average level, and peak is the peak over
     * the last frame of audio.  Peak goes up instantly and then decays.
     * @param recorder
     * @param ms
     */
    private static void sleepAndMonitorVUMeter(AudioRecorder recorder, int ms) {
        for (int i = 0; i < 10; i++) {
            System.out.println("VU RMS " + recorder.getRmsDB());
            System.out.println("VU Peak " + recorder.getPeakDB());
            sleep(ms / 10);
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
