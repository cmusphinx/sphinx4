package edu.cmu.sphinx.jsapi;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;

/**
 * Recognition thread to run the recognizer in parallel.
 *
 * @author Dirk Schnelle-Walka
 * @version $Revision: 1110 $
 */
final class RecognitionThread
        extends Thread {
    /** The wrapper for the sphinx4 recognizer. */
    private SphinxRecognizer recognizer;

    /**
     * Creates a new object.
     * @param rec The wrapper for the sphinx4 recognizer.
     */
    public RecognitionThread(final SphinxRecognizer rec) {
        recognizer = rec;
        setDaemon(true);
        setName("SphinxRecognitionThread");
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        final Recognizer rec = recognizer.getRecognizer();
        final Microphone microphone = getMicrophone();
        final boolean started;

        if (microphone != null) {
            microphone.clear();
            started = microphone.startRecording();
        } else {
            started = true;
        }

        if (started) {
            recognize(rec, microphone);
        }
        if (microphone != null) {
            // Stop recording from the microphone.
            while (microphone.isRecording()) {
                microphone.stopRecording();
            }
        }
    }

    /**
     * Recognition loop. Continue recognizing until this thread is
     * requested to stop.
     * @param rec The recognizer to use.
     * @param mic The microphone to use.
     */
    private void recognize(final Recognizer rec, final Microphone mic) {
        while (hasMoreData(mic) && !isInterrupted()) {
            try {
                rec.recognize();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks, if the microphone has more data to deliver.
     * @param mic The microphone or <code>null</code> if the data processor
     * is not a microphone.
     * @return <code>true</code> if there is more data.
     */
    private boolean hasMoreData(final Microphone mic) {
        if (mic == null) {
            return true;
        }

        return mic.hasMoreData();
    }

    /**
     * Stop this recognition thread.
     */
    public void stopRecognition() {
        final Microphone microphone = getMicrophone();
        if (microphone != null) {
            microphone.stopRecording();
        }

        interrupt();
    }

    /**
     * Retrieves the microphone.
     * @return The microphone, <code>null</code> if the data processor is
     * not a microphone.
     * @since 0.5.5
     */
    private Microphone getMicrophone() {
        final DataProcessor dataProcessor = recognizer.getDataProcessor();
        if (dataProcessor instanceof Microphone) {
            return (Microphone) dataProcessor;
        }

        return null;
    }
}
