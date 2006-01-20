package edu.cmu.sphinx.tools.batch;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.props.*;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * User: Peter Wolf
 * Date: Nov 10, 2005
 * Time: 2:42:06 PM
 * Copyright 2005, Peter Wolf
 *
 * Runs a NIST corpus as used by the GALE project.  The inputs are
 * a CTL file, and a REF file.  The output is a CTM file.
 *
 * A CTL file contains a list of utterances to decode. The format is
 *
 * <utterance file> <start offset> <end offset> <utterance name>
 *
 * The <utterance file> is a base to which the property "dataDirectory" is
 * prepended, and ".raw" is appended.  The utterance file should be raw
 * PCM that agrees with the "bitsPerSample", "channelCount", "samplesPerSecond",
 * and "framesPerSecond" properties.
 *
 * The <start offset> and <end offset> are specified in frames, where
 *
 * bytesPerFrame = (bitsPerSample/8)*channelCount*samplesPerSecond/framesPerSecond
 *
 * The <utterance name> should be a unique string.  For example
 * "<utterance file>_<start offset>_<end offset>".
 *
 * A REF file contains the correct transcripts of the utterances specified in
 * the CTL file.  Each line should be of the form
 *
 * <ASCII transcript> (<utterance name>)
 *
 * The output is a "processed" CTM file.  It is used by the NIST tools
 * to compute the performance on the copus.  The format is not documented
 * because it is currently a hack to get the Dry Run going.  We need
 * to think more about it.  If you want to use this tool talk to Peter Wolf,
 * or Arthur Chan.
 *
 */
public class BatchNISTRecognizer extends BatchModeRecognizer {

    protected String ctlFile;
    protected String dataDir;
    protected String refFile;
    protected String ctmFile;
    protected int bitsPerSample;
    protected int samplesPerSecond;
    protected int framesPerSecond;
    protected int channelCount;
    protected int bytesPerFrame;

    /**
     * The sphinx property that specifies the file containing the corpus utterance audio
     */
    public final static String PROP_DATA_DIR = "dataDirectory";

    /**
     * The sphinx property that specifies the file containing the corpus utterance audio
     */
    public final static String PROP_CTL_FILE = "ctlFile";

    /**
     * The sphinx property that specifies the file containing the transcripts of the corpus
     */
    public final static String PROP_REF_FILE = "refFile";

    /**
     * The sphinx property that specifies the the directory where the output XXX files should be placed
     */
    public final static String PROP_CTM_FILE = "ctmFile";

    /**
     * The sphinx properties that specify the format of the PCM audio in the data file
     */
    public final static String PROP_BITS_PER_SAMPLE = "bitsPerSample";
    public final static String PROP_CHANNEL_COUNT = "channelCount";
    public final static String PROP_SAMPLES_PER_SECOND = "samplesPerSecond";
    public final static String PROP_FRAMES_PER_SECOND = "framesPerSecond";

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        super.register(name,registry);
        this.name = name;
        registry.register(PROP_DATA_DIR, PropertyType.STRING);
        registry.register(PROP_CTL_FILE, PropertyType.STRING);
        registry.register(PROP_REF_FILE, PropertyType.STRING);
        registry.register(PROP_CTM_FILE, PropertyType.STRING);
        registry.register(PROP_BITS_PER_SAMPLE, PropertyType.INT);
        registry.register(PROP_CHANNEL_COUNT, PropertyType.INT);
        registry.register(PROP_SAMPLES_PER_SECOND, PropertyType.INT);
        registry.register(PROP_FRAMES_PER_SECOND, PropertyType.INT);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        //cm = ps.getPropertyManager();
        recognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                Recognizer.class);
        inputDataProcessors = (List) ps.getComponentList
                (PROP_INPUT_DATA_PROCESSORS, DataProcessor.class);
        dataDir = ps.getString(PROP_DATA_DIR, "<raw data directory not set>");
        ctlFile = ps.getString(PROP_CTL_FILE, "<ctl file not set>");
        refFile = ps.getString(PROP_REF_FILE, "<ref file not set>");
        ctmFile = ps.getString(PROP_CTM_FILE, "<ctm file not set>");

        bitsPerSample = ps.getInt(PROP_BITS_PER_SAMPLE, 16);
        channelCount = ps.getInt(PROP_CHANNEL_COUNT, 1);
        samplesPerSecond = ps.getInt(PROP_SAMPLES_PER_SECOND, 16000);
        framesPerSecond = ps.getInt(PROP_FRAMES_PER_SECOND, 100);

        bytesPerFrame = ((bitsPerSample / 8) * channelCount * samplesPerSecond) / framesPerSecond;

        logger.info(
                "BatchNISTRecognizer:\n" +
                        "  dataDirectory=" + dataDir + "\n" +
                        "  ctlFile=" + ctlFile + "\n" +
                        "  bitsPerSample=" + bitsPerSample + "\n" +
                        "  channelCount=" + channelCount + "\n" +
                        "  samplesPerSecond=" + samplesPerSecond + "\n" +
                        "  framesPerSecond=" + framesPerSecond + "\n");
    }

    protected class CTLException extends Exception {
        CTLException(String msg) {
            super(msg);
        }
    }

    protected class CTLUtterance {
        int startOffset;
        int endOffset;
        String name;
        byte[] data;
        String ref;

        CTLUtterance(String ctl, String ref) throws CTLException {
            /*
                example line:
                20040422_150000_NTDTV.80Hz-6400Hz 64155 65103 20040422_150000_NTDTV_64155-65103_spk8
            */
            this.ref = ref;
            String[] fields = ctl.split(" ");
            if (fields.length != 4) throw new CTLException("CTL Syntax Error: " + ctl);
            startOffset = Integer.parseInt(fields[1]);
            endOffset = Integer.parseInt(fields[2]);
            name = fields[3];
            data = new byte[(endOffset - startOffset) * bytesPerFrame];
            int i = fields[0].indexOf('.');
            String file = fields[0];
            if( i >= 0 ) {
                file = file.substring(0,i);
            }
            file = dataDir + "/" + file + ".raw";
            try {
                InputStream dataStream = new FileInputStream(file);
                dataStream.skip(startOffset * bytesPerFrame);
                if (dataStream.read(data) != data.length) {
                    new CTLException("Unable to read " + data.length + " bytes of utterance " + name);
                }
            }
            catch (IOException e) {
                throw new CTLException("Unable to read utterance " + name + ": " + e.getMessage());
            }
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return name;
        }

        public String getRef() {
            return ref;
        }
    }

    protected class CTLIterator implements Iterator {

        CTLUtterance utterance;
        LineNumberReader ctlReader;
        LineNumberReader refReader;

        CTLIterator() throws IOException {
            ctlReader = new LineNumberReader(new FileReader(ctlFile));
            refReader = new LineNumberReader(new FileReader(refFile));
            utterance = nextUtterance();
        }

        private CTLUtterance nextUtterance() {
            try {
                String ctl = ctlReader.readLine();
                String ref = refReader.readLine();
                if (ctl == null || ref == null)
                    return null;
                else
                    return new CTLUtterance(ctl, ref);
            } catch (Exception e) {
                throw new Error(e.getMessage());
            }
        }

        public boolean hasNext() {
            return utterance != null;
        }

        public Object next() {
            CTLUtterance u = utterance;
            utterance = nextUtterance();
            return u;
        }

        public void remove() {
            throw new Error("Not implemented");
        }
    }

    protected void setInputStream(CTLUtterance utt) throws IOException {
        for (Iterator i = inputDataProcessors.iterator(); i.hasNext();) {
            DataProcessor dataSource = (DataProcessor) i.next();

            if (dataSource instanceof StreamDataSource) {
                ((StreamDataSource) dataSource).setInputStream(utt.getInputStream(), utt.getName());
            } else if (dataSource instanceof StreamCepstrumSource) {
                boolean isBigEndian = Utilities
                        .isCepstraFileBigEndian(utt.getName());
                StreamCepstrumSource cepstrumSource =
                        (StreamCepstrumSource) dataSource;
                cepstrumSource.setInputStream(utt.getInputStream(), isBigEndian);
            }
        }
    }

    public void decode() {

        try {
            totalCount = 0;
            DataOutputStream ctm = new DataOutputStream(new FileOutputStream(ctmFile));
            recognizer.allocate();

            for (Iterator i = new CTLIterator(); i.hasNext();) {
                CTLUtterance utt = (CTLUtterance) i.next();
                setInputStream(utt);
                Result result = recognizer.recognize();
                System.out.println("Utterance " + totalCount + ": " + utt.getName());
                System.out.println("Reference: " + utt.getRef());
                System.out.println("Result   : " + result);
                logger.info("Utterance " + totalCount + ": " + utt.getName());
                logger.info("Result   : " + result);
                handleResult(ctm, utt, result);
                totalCount++;
            }

            recognizer.deallocate();
        } catch (IOException io) {
            logger.severe("I/O error during decoding: " + io.getMessage());
        }
        logger.info("BatchCTLDecoder: " + totalCount + " utterances decoded");
    }

    protected void handleResult(DataOutputStream out, CTLUtterance utt, Result result) throws IOException {
        dumpBestPath(out, utt, result.getBestFinalToken());
    }

    private int dumpBestPath(DataOutputStream out, CTLUtterance utt, Token token) throws IOException {

        if (token == null) return 0;

        Token pred = token.getPredecessor();
        int startFrame = dumpBestPath(out, utt, pred);
        if (token.isWord()) {

            int endFrame = token.getFrameNumber();

            WordSearchState wordState = (WordSearchState) token.getSearchState();
            Word word = wordState.getPronunciation().getWord();
            String spelling = word.getSpelling();
            if (!spelling.startsWith("<")) {
                String [] names = utt.name.split("_");
                String id = names[0] + "_" + names[1] + "_" + names[2];
                out.write((id + " 1 " + (utt.startOffset + startFrame) / 100.0 + " " + (endFrame - startFrame) / 100.0 + " ").getBytes());
                out.write(toHex2Binary(spelling));
                out.write(" 0.700000\n".getBytes());
            }
            return endFrame;
        }
        return startFrame;
    }

    private byte[] toHex2Binary(String spelling) {
        byte [] bin = new byte[ spelling.length() / 2];
        for (int i = 0; i < spelling.length(); i += 2) {
            int i0 = hexToByte(spelling.charAt(i));
            int i1 = hexToByte(spelling.charAt(i + 1));
            bin[i / 2] = (byte) (i1 + (16 * i0));
        }
        return bin;
    }

    private int hexToByte(char c) {
        switch (c) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'a':
                return 10;
            case 'b':
                return 11;
            case 'c':
                return 12;
            case 'd':
                return 13;
            case 'e':
                return 14;
            case 'f':
                return 15;
            default:
                throw new Error("Bad hex char " + c);
        }

    }

    public static void main(String[] argv) {

        if (argv.length != 1) {
            System.out.println(
                    "Usage: BatchNISTRecognizer propertiesFile");
            System.exit(1);
        }

        String propertiesFile = argv[0];

        ConfigurationManager cm;

        BatchNISTRecognizer bmr;

        try {
            URL url = new File(propertiesFile).toURI().toURL();
            cm = new ConfigurationManager(url);
            bmr = (BatchNISTRecognizer) cm.lookup("batchNIST");
        } catch (IOException ioe) {
            System.err.println("I/O error during initialization: \n   " + ioe);
            return;
        } catch (InstantiationException e) {
            System.err.println("Error during initialization: \n  " + e);
            return;
        } catch (PropertyException e) {
            System.err.println("Error during initialization: \n  " + e);
            return;
        }

        if (bmr == null) {
            System.err.println("Can't find batchNIST in " + propertiesFile);
            return;
        }

        bmr.decode();
    }
}
