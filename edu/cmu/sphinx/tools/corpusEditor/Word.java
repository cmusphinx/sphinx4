package edu.cmu.sphinx.tools.corpusEditor;

import edu.cmu.sphinx.tools.audio.AudioData;
import edu.cmu.sphinx.tools.batch.BatchNISTRecognizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Word =
 * Spelling
 * BeginTime
 * EndTime
 * IsExcluded
 * [ID]
 * [Pronunciation]
 * [SpectrogramData]
 * [PCMData]
 * [PitchData]
 * [EnergyData]
 */
public class Word implements Comparable {
    String spelling;
    Integer beginTime;
    Integer endTime;
    boolean isExcluded;
    Utterance utterance;

    public String getSpelling() {
        return spelling;
    }

    public Integer getBeginTime() {
        return beginTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public boolean isExcluded() {
        return isExcluded;
    }

    public Utterance getUtterance() {
        return utterance;
    }

    private String getUtteranceShortName() {
        String s = utterance.getPcmFile();
        String s1[] = s.split("_");
        //System.out.println("+++");
        //for( String s3 : s1 ) {
        //    System.out.println("["+s3+"]");
        //}
        String s4[] = s1[2].split("\\W");
        //for( String s5 : s4 ) {
        //    System.out.println("["+s5+"]");
        //}
        return s4[0];
    }

    public String toString() {
        return hex2Unicode(spelling) + " " + getUtteranceShortName() + " " + beginTime + " " + endTime;
    }

    static String hex2Unicode(String hex) {
        if (hex.startsWith("<")) return hex;
        byte[] bytes = BatchNISTRecognizer.hex2Binary(hex);
        try {
            return new String(bytes, "GB2312");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public Word() {
    }

    public Word(String spelling, int beginTime, int endTime, boolean excluded) {
        this.spelling = spelling;
        this.beginTime = beginTime;
        this.endTime = endTime;
        isExcluded = excluded;
    }

    public void init(Utterance utterance) {
        this.utterance = utterance;
    }

    public AudioData getAudio() {
        try {
            short[] pcm = readPcmData();

            return new AudioData(pcm, utterance.corpus.waveform.samplesPerSecond);

        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private short[] readPcmData() throws IOException {
        InputStream in = new FileInputStream(utterance.getPcmFile());

        int bo = utterance.corpus.time2PcmOffet(beginTime);
        int eo = utterance.corpus.time2PcmOffet(endTime);
        int l = eo - bo;

        byte[] buf = new byte[l];
        in.skip(bo);
        in.read(buf, 0, l);

        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));

        short [] sbuf = new short[l / 2];

        for (int i = 0; i < l / 2; i++) {
            sbuf[i] = din.readShort();
        }
        return sbuf;
    }

    public double [] getPitch() {
        try {
            return readAsciiDoubleData(utterance.getPitchFile());
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public double [] getEnergy() {
        try {
            return readAsciiDoubleData(utterance.getEnergyFile());
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private double[] readAsciiDoubleData(String file) throws IOException {
        LineNumberReader in = new LineNumberReader(new FileReader(file));

        int bo = utterance.corpus.time2AsciiDoubleLine(beginTime);
        int eo = utterance.corpus.time2AsciiDoubleLine(endTime);

        double[] buf = new double[eo - bo];

        for (int i = 0; i<bo; i++) {
            in.readLine();
        }

        for (int i = 0; i < (eo - bo); i++) {
            String line = in.readLine();
            buf[i] = Double.parseDouble(line);
        }
        return buf;
    }

    public List<String> getCharacters() {
        String s = spelling;
        List<String> r = new ArrayList();

        if (s.startsWith("<")) {
            r.add(s);
        } else {
            while (s.length() > 0) {
                String head = s.substring(0, 4);
                s = s.substring(4);
                r.add(head);
            }
        }
        return r;
    }

    public int compareTo(Object o) {
        return spelling.compareTo(((Word) o).spelling);
    }
}
