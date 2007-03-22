package edu.cmu.sphinx.frontend.util;

import edu.cmu.sphinx.util.BatchFile;
import edu.cmu.sphinx.util.ReferenceSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * Concatenates a list of audio files as one continuous audio stream.
 * <p/>
 * A {@link edu.cmu.sphinx.frontend.DataStartSignal DataStartSignal} will be placed before the start of the first file,
 * and a {@link edu.cmu.sphinx.frontend.DataEndSignal DataEndSignal} after the last file. No DataStartSignal or
 * DataEndSignal will be placed between them.
 *
 * @author Holger Brandl
 */
public class ConcatAudioFileDataSource extends AudioFileDataSource implements ReferenceSource {

    private String nextFile = null;
    private List<String> referenceList;
    private String batchFile;
    private boolean isInitialized;


    public ConcatAudioFileDataSource() {
        this(PROP_BYTES_PER_READ_DEFAULT);
    }


    public ConcatAudioFileDataSource(int bytesPerRead) {
        assert bytesPerRead > 0;
        this.bytesPerRead = bytesPerRead;
    }


    /** Initializes a ConcatFileDataSource. */
    public void initialize() {
        super.initialize();

        if (batchFile == null)
            return;

        try {
            referenceList = new ArrayList<String>();
            dataStream = new SequenceInputStream(new InputStreamEnumeration(batchFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setBatchFile(File batchFile) {
        assert batchFile.isFile();
        this.batchFile = batchFile.getPath();

        initialize();
    }


    public void setAudioFile(URL audioFileURL, String streamName) {
        throw new RuntimeException("Not supported for ConcatAudioFileDataSource");
    }


    /**
     * Returns a list of all reference text. Implements the getReferences() method of ReferenceSource.
     *
     * @return a list of all reference text
     */
    public List<String> getReferences() {
        return referenceList;
    }


    /**
     * The work of the concatenating of the audio files are done here. The idea here is to turn the list of audio files
     * into an Enumeration, and then fed it to a SequenceInputStream, giving the illusion that the audio files are
     * concatenated, but only logically.
     */
    class InputStreamEnumeration implements Enumeration {

        private BufferedReader reader;
        private String lastFile;


        InputStreamEnumeration(String batchFile) throws IOException {
            reader = new BufferedReader(new FileReader(batchFile));
        }


        /**
         * Tests if this enumeration contains more elements.
         *
         * @return true if and only if this enumeration object contains at least one more element to provide; false
         *         otherwise.
         */
        public boolean hasMoreElements() {
            if (nextFile == null) {
                nextFile = readNext();
            }
            return (nextFile != null);
        }


        /**
         * Returns the next element of this enumeration if this enumeration object has at least one more element to
         * provide.
         *
         * @return the next element of this enumeration.
         */
        public Object nextElement() {
            Object stream = null;
            if (nextFile == null) {
                nextFile = readNext();
            }

            if (nextFile != null) {
                try {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(new File(nextFile).toURI().toURL());

                    // test wether all files in the stream have the same format
                    AudioFormat format = ais.getFormat();
                    if (!isInitialized) {
                        isInitialized = true;

                        bigEndian = format.isBigEndian();
                        sampleRate = (int) format.getSampleRate();
                        signedData = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
                        bytesPerValue = format.getSampleSizeInBits() / 8;
                    }

                    if (format.getSampleRate() != sampleRate || format.getChannels() != 1 || format.isBigEndian() != bigEndian) {
                        throw new RuntimeException("format mismatch for subsequent files");
                    }

                    stream = ais;
                    // System.out.println(nextFile);

                    for (int i = 0; i < fileListeners.size(); i++)
                        fileListeners.get(i).audioFileProcStarted(new File(nextFile));

                    lastFile = nextFile;
                    nextFile = null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    throw new Error("Cannot convert " + nextFile + " to a FileInputStream");
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
            }

            return stream;
        }


        /**
         * Returns the name of next audio file
         *
         * @return the name of the appropriate audio file
         */
        public String readNext() {
            if (lastFile != null) {
                for (int i = 0; i < fileListeners.size(); i++)
                    fileListeners.get(i).audioFileProcFinished(new File(lastFile));

                lastFile = null;
            }

            String result;
            try {
                String next = reader.readLine();
                if (next != null) {
                    String reference = BatchFile.getReference(next);
                    referenceList.add(reference);
                }

                if (next != null && next.trim().length() > 0)
                    result = next;
                else
                    result = null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new Error("Problem reading from batch file");
            }
            return result;
        }
    }
}
