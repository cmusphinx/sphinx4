package edu.cmu.sphinx.tools.corpusEditor;

import edu.cmu.sphinx.tools.audio.AudioData;
import edu.cmu.sphinx.tools.audio.Utils;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Copyright 1999-2006 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * <p/>
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * <p/>
 * User: Peter Wolf
 * Date: Feb 23, 2006
 * Time: 4:25:19 PM
 */
public class Player {

    protected AudioData audio;
    protected AudioFormat format;
    protected boolean stop;
    protected byte[] data;
    protected DataLine.Info info;
    protected SourceDataLine out;

    protected PlayerThread thread=null;

    public Player( AudioData audioData ) {
        audio = audioData;
        format = audioData.getAudioFormat();
        data = toBytes( audio.getAudioData() );
        stop = false;

        info = new DataLine.Info(SourceDataLine.class,format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new Error( "Audio format is not supported" );
        }

        try {
            out = (SourceDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            throw new Error(e);
        }
    }

    public void start() {
        stop = false;
        thread = new PlayerThread();
        thread.start();
    }

    public void stop() {
        stop = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean isPlaying() {
        if( thread==null ) return false;
        else return thread.isAlive();
    }

    protected byte [] toBytes( short[] data ) {
        byte [] b = new byte[2];
        byte [] r = new byte[ data.length * 2 ];
        for( int i=0; i<data.length; i++ ) {
            Utils.toBytes( data[i], b, false );
            r[i*2] = b[0];
            r[(i*2)+1] = b[1];
        }
        return r;
    }

    protected class PlayerThread extends Thread {
        public void run() {
            try {

                out.open(format);
                out.start();

                for( int i=0; i<data.length; i+=1024 ) {
                    if( stop ) break;
                    int cnt = 1024;
                    if( data.length-i < cnt ) cnt = data.length-i;
                    out.write( data, i, cnt );
                }

                out.drain();
                out.close();

            } catch (LineUnavailableException e) {
                throw new Error(e);
            }
        }
    }
}
