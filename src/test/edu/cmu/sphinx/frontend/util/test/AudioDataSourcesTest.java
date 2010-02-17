package edu.cmu.sphinx.frontend.util.test;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.AudioFileProcessListener;
import edu.cmu.sphinx.frontend.util.ConcatAudioFileDataSource;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Some small unit tests to check whether the AudioFileDataSource and the ConcatAudioFileDataSource are working
 * properly.
 */
public class AudioDataSourcesTest {

    public static final String baseDir = "tests/other/";

    private int numFileStarts; // used to test the AudioFileProcessListener implementation
    private int numFileEnds;


    @Before
    public void setUp() {
        numFileStarts = 0;
        numFileEnds = 0;
    }


    @Test
    public void testSimpleFileSources() throws DataProcessingException {
        runAssert("test.wav");
        runAssert("test.aiff");
        runAssert("test.au");
//        runAssert("test.ogg"); // only works if the appropriate JavaSound extension is installed
    }


    @Test
    public void test8KhzSource() throws DataProcessingException {
        AudioFileDataSource dataSource = ConfigurationManager.getInstance(AudioFileDataSource.class);

        // test simple .wav
        dataSource.setAudioFile(new File(baseDir + "test8k.wav"), null);

        Assert.assertTrue(dataSource.getData() instanceof DataStartSignal);

        Data d = dataSource.getData();
        Assert.assertTrue(dataSource.getData() instanceof DoubleData);
        Assert.assertTrue(((DoubleData) d).getSampleRate() == 8000);


        while ((d = dataSource.getData()) instanceof DoubleData) ;

        Assert.assertTrue(d instanceof DataEndSignal);
    }


    @Test
    public void testConcatDataSource() throws DataProcessingException {
        ConcatAudioFileDataSource dataSource = ConfigurationManager.getInstance(ConcatAudioFileDataSource.class);

        dataSource.addNewFileListener(new AudioFileProcessListener() {


            public void audioFileProcStarted(File audioFile) {
                numFileStarts++;
            }


            public void audioFileProcFinished(File audioFile) {
                numFileEnds++;
            }

            @Override
            public void newProperties(PropertySheet ps) throws PropertyException {
                // to avoid compilation error
            }
        });

        File batchFile = new File(baseDir + "test.drv");
        assert batchFile.isFile() : "no file, not test.";
        dataSource.setBatchFile(batchFile);


        Assert.assertTrue(dataSource.getData() instanceof DataStartSignal);
        Assert.assertTrue(dataSource.getData() instanceof DoubleData);

        Data d;
        while ((d = dataSource.getData()) instanceof DoubleData) ;
        Assert.assertTrue(d instanceof DataEndSignal);

        Assert.assertTrue(numFileStarts == 3);
        Assert.assertTrue(numFileEnds == 3);
    }


    private void runAssert(String fileName) throws DataProcessingException {
        AudioFileDataSource dataSource = ConfigurationManager.getInstance(AudioFileDataSource.class);

        // test simple .wav
        dataSource.setAudioFile(new File(baseDir + fileName), null);

        Assert.assertTrue(dataSource.getData() instanceof DataStartSignal);
        Assert.assertTrue(dataSource.getData() instanceof DoubleData);

        Data d;
        while ((d = dataSource.getData()) instanceof DoubleData) ;
        Assert.assertTrue(d instanceof DataEndSignal);
    }
}
