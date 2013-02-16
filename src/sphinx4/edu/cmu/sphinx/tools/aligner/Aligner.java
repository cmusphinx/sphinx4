/*
 * Copyright 1999-2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.tools.aligner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.api.GrammarAligner;
import edu.cmu.sphinx.result.WordResult;

/**
 * This is a simple tool to align audio to text and dump a database
 * for the training/evaluation.
 * 
 * You need to provide a model, dictionary, audio and the text to align. 
 */
public class Aligner {
    
    static int diff = 200;

    public static void main(String args[]) throws Exception {

        URL acousticModel = new URL(args[0]);
        URL dictionary = new URL(args[1]);
        GrammarAligner aligner = new GrammarAligner(acousticModel, dictionary,
                null);

        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(
                args[2]));
        String text = readFileText(args[3]);

        ArrayList<WordResult> results = aligner.align(stream, text);
//        for (WordResult result : results) {
//          System.out.println(result);
//        }
        dumpDatabase(args[2], results);
    } 
    
    public static void copyAudio(String sourceFileName,
            String destinationFileName, int startMs, int endMs) throws UnsupportedAudioFileException, IOException {
        AudioInputStream inputStream = null;
        AudioInputStream cutStream = null;
        File file = new File(sourceFileName);
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioFormat format = fileFormat.getFormat();
        inputStream = AudioSystem.getAudioInputStream(file);
        int bytesPerMsSecond = format.getFrameSize()
                * (int) format.getFrameRate() / 1000;
        inputStream.skip(startMs * bytesPerMsSecond);
        long framesOfAudioToCopy = endMs * (int) format.getFrameRate() / 1000;
        cutStream = new AudioInputStream(inputStream, format,
                framesOfAudioToCopy);
        File destinationFile = new File(destinationFileName);
        AudioSystem.write(cutStream, fileFormat.getType(),
                destinationFile);
        inputStream.close();
        cutStream.close();
    }

    private static void dumpDatabase(String input,
            ArrayList<WordResult> results) throws UnsupportedAudioFileException, IOException {        
        ArrayList<ArrayList<WordResult>> utts = new ArrayList<ArrayList<WordResult>>();
        ArrayList<WordResult> currentUtt = null;
        int fillerLength = 0;
        for (WordResult result : results) {
            if (!result.isFiller()) {
                fillerLength = 0;
                if (currentUtt == null) {
                    currentUtt = new ArrayList<WordResult>(1);
                }
                currentUtt.add(result);
            } else {
                fillerLength += result.getEndFrame() - result.getStartFrame();
                if (fillerLength > diff) {
                    if (currentUtt != null)
                        utts.add(currentUtt);
                    currentUtt = null;
                }
            }
        }
        
        int count = 0;
        for (ArrayList<WordResult> utt : utts) {
            String uttId = String.format("%03d", count) + "0";
            String outFile = input.substring(0, input.length() - 4) + "-" + uttId + ".wav";
            int startMs = utt.get(0).getStartFrame() - diff;
            int lengthMs = utt.get(utt.size() - 1).getEndFrame() - startMs + diff;            
            for (WordResult result : utt) {
                System.out.print(result.getPronunciation().getWord());
                System.out.print(' ');
            }
            copyAudio(input, outFile, startMs, lengthMs);
            System.out.println("(" + uttId + ")");
            count++;
        }
    }

    private static String readFileText(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line.trim());
            sb.append(" ");
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }
}
