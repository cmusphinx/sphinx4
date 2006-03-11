package edu.cmu.sphinx.tools.corpus;

import java.io.*;

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
 * Date: Mar 1, 2006
 * Time: 9:41:38 PM
 */

/**
 * An AudioDatabase manages the large binary data that contains the audio recordings referenced by a Corpus.  The data
 * includes PCM audio, pitch and energy.  It will also contain any other data (e.g. video and lip position etc.) that
 * may be used to train and test recognizer models.  The data is assumed to indexed by time, and this class provides
 * efficient random access to contiguous blocks of the data.  This is a base class, it does not deal with creating this data,
 * or storing it.  Sub-classes will implement the behavior when data is stored as files or in databases.
 */
public abstract class AudioDatabase {

    String pcmFileName;
    String pitchFileName;
    String energyFileName;

    int bitsPerSample;
    int samplesPerSecond;
    int channelCount;
    int bytesPerMillisecond;

    public AudioDatabase() {
    }

    void init() {
        bytesPerMillisecond = (bitsPerSample / 8) * channelCount * samplesPerSecond / 1000;
    }

    private int time2PcmOffet(int time) {
        return time * bytesPerMillisecond;
    }

    private int time2AsciiDoubleLine(int time) {
        return time;
    }

    public String getPcmFileName() {
        return pcmFileName;
    }

    public String getPitchFileName() {
        return pitchFileName;
    }

    public String getEnergyFileName() {
        return energyFileName;
    }

    public int getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getBytesPerMillisecond() {
        return bytesPerMillisecond;
    }

    public abstract byte[] readPcmAsBytes(int beginTime, int endTime);

    public abstract short[] readPcmAsShorts(int beginTime, int endTime);
   
    public abstract double [] readPitch(int beginTime, int endTime);

    public abstract double [] readEnergy(int beginTime, int endTime);

/*
    public byte[] readPcmAsBytes(int beginTime, int endTime) {
        try {
            InputStream in = new FileInputStream(getPcmFileName());

            int bo = time2PcmOffet(beginTime);
            int eo = time2PcmOffet(endTime);
            int l = eo - bo;

            byte[] buf = new byte[l];
            in.skip(bo);
            in.read(buf, 0, l);

            return buf;

        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }

    }

    public short[] readPcmAsShorts(int beginTime, int endTime) {
        try {
            byte[] buf = readPcmAsBytes(beginTime, endTime);
            int l = buf.length;

            DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));

            short [] sbuf = new short[l / 2];

            for (int i = 0; i < l / 2; i++) {
                sbuf[i] = din.readShort();
            }
            return sbuf;
        } catch (IOException e) {
            throw new Error(e);
        }
    }


    public double [] readPitch(int beginTime, int endTime) {
        return readAsciiDoubleData(getPitchFileName(), beginTime, endTime);
    }

    public double [] readEnergy(int beginTime, int endTime) {
        return readAsciiDoubleData(getEnergyFileName(), beginTime, endTime);
    }

    private double[] readAsciiDoubleData(String file, int beginTime, int endTime) {
        try {
            LineNumberReader in = new LineNumberReader(new FileReader(file));

            int bo = time2AsciiDoubleLine(beginTime);
            int eo = time2AsciiDoubleLine(endTime);

            double[] buf = new double[eo - bo];

            for (int i = 0; i < bo; i++) {
                in.readLine();
            }

            for (int i = 0; i < (eo - bo); i++) {
                String line = in.readLine();
                buf[i] = Double.parseDouble(line);
            }
            return buf;
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
    */

    public void setPcmFileName(String pcmFileName) {
        this.pcmFileName = pcmFileName;
    }

    public void setPitchFileName(String pitchFileName) {
        this.pitchFileName = pitchFileName;
    }

    public void setEnergyFileName(String energyFileName) {
        this.energyFileName = energyFileName;
    }

    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public void setSamplesPerSecond(int samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }
}
