/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2002-2004 Sun Microsystems, Inc.  
 * Portions Copyright 2002-2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.tools.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;

/**
 * Plays an AudioData in a separate thread.
 */
public class AudioPlayer extends Thread {
    private AudioData audio;
    private SourceDataLine line = null;
    
    /**
     * Creates a new AudioPlayer for the given AudioData.
     */
    public AudioPlayer(AudioData audio) {
        this.audio = audio;
    }

    /**
     * Notifies the AudioPlayer thread to play the audio.
     */
    public void play() {
        synchronized(audio) {
            audio.notify();
        }
    }

    /**
     * Plays the AudioData in a separate thread.
     */
    public void run() {
        while (true) {
            try {
                synchronized(audio) {
                    audio.wait();
                    AudioFormat format = audio.getAudioFormat();
                    short[] data = audio.getAudioData();
                    int selectionStart = Math.max(0,audio.getSelectionStart());
                    int selectionEnd = audio.getSelectionEnd();
                    if (selectionEnd == -1) {
                        selectionEnd = data.length;
                    }
                    
                    DataLine.Info info =
                        new DataLine.Info(SourceDataLine.class, 
                                          format);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();

                    byte[] frame = new byte[2];
                    for (int i = selectionStart; i < selectionEnd; i++) {
                        Utils.toBytes(data[i], frame, true);
                        line.write(frame, 0, frame.length);
                    }
                    
                    line.drain();
                    line.close();
                    line = null;
                }
            } catch(Exception e) {
                e.printStackTrace();
                break;
            }   
        }
    }
}
