/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.tools.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.util.StreamCepstrumSource;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.BatchItem;
import edu.cmu.sphinx.util.BatchManager;
import edu.cmu.sphinx.util.PooledBatchManager;
import edu.cmu.sphinx.util.SimpleBatchManager;
import edu.cmu.sphinx.util.Utilities;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
import edu.cmu.sphinx.util.CommandInterpreter;
import edu.cmu.sphinx.util.CommandInterface;

/**
 * Decodes a batch file containing a list of files to decode. The files can be
 * either audio files or cepstral files, but defaults to audio files.
 * To run this BatchModeRecognizer:
 * <pre>
 * java BatchModeRecognizer &lt;xmlConfigFile&gt; &lt;batchFile&gt;
 * </pre>
 * where <code>xmlConfigFile</code> is an XML-based configuration file and
 * <code>batchFile</code> is a file listing all the files to decode and
 * transcript of those files. For information about the configuration
 * file, refer to the document 
 * <a href="../../util/props/doc-files/ConfigurationManagement.html">
 * Sphinx-4 Configuration Management</a>. For information about the
 * batch file, refer to the <a href="../../../../../../index.html#batch_files">
 * batch file description</a>.
 * 
 * This class will send recognition results to the logger if the log level is
 * set to INFO.
 *
 */
public class BatchModeRecognizer implements Configurable {

    /**
     * The SphinxProperty name for how many files to skip for every decode.
     */
    public final static String PROP_SKIP = "skip";

    /**
     * The default value for the property PROP_SKIP.
     */
    public final static int PROP_SKIP_DEFAULT = 0;
    
    /**
     * The SphinxProperty name for how many utterances to process
     */
    public final static String PROP_COUNT = "count";
    /**
     * The default value for the property PROP_COUNT.
     */
    public final static int PROP_COUNT_DEFAULT = 1000000;

    /**
     * The SphinxProperty that specified which batch job is to be run.
     *  
     */
    public final static String PROP_WHICH_BATCH = "whichBatch";

    /**
     * The default value for the property PROP_WHICH_BATCH.
     */
    public final static int PROP_WHICH_BATCH_DEFAULT = 0;

    /**
     * The SphinxProperty for the total number of batch jobs the decoding run
     * is being divided into.
     * 
     * The BatchDecoder supports running a subset of a batch. This allows a
     * test to be distributed among several machines.
     *  
     */
    public final static String PROP_TOTAL_BATCHES = "totalBatches";

    /**
     * The default value for the property PROP_TOTAL_BATCHES.
     */
    public final static int PROP_TOTAL_BATCHES_DEFAULT = 1;

    /**
     * The SphinxProperty that defines whether or not the decoder should use
     * the pooled batch manager
     */
    public final static String PROP_USE_POOLED_BATCH_MANAGER = "usePooledBatchManager";

    /**
     * The default value for the property PROP_USE_POOLED_BATCH_MANAGER.
     */
    public final static boolean PROP_USE_POOLED_BATCH_MANAGER_DEFAULT = false;

    /**
     * The Sphinx property that specifies the recognizer to use
     */
    public final static String PROP_RECOGNIZER = "recognizer";

    /**
     * The sphinx property that specifies the input source
     */
    public final static String PROP_INPUT_DATA_PROCESSORS = "inputDataProcessors";


    // -------------------------------
    // Configuration data
    // --------------------------------
    private String name;
    private List inputDataProcessors;
    private int skip;
    private int totalCount;
    private int whichBatch;
    private int totalBatches;
    private boolean usePooledBatchManager;
    private BatchManager batchManager;
    private Recognizer recognizer;
    private Logger logger;

    private BatchItem curBatchItem;
    private ConfigurationManager cm;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#register(java.lang.String,
     *      edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_SKIP, PropertyType.INT);
        registry.register(PROP_COUNT, PropertyType.INT);
        registry.register(PROP_WHICH_BATCH, PropertyType.INT);
        registry.register(PROP_TOTAL_BATCHES, PropertyType.INT);
        registry.register(PROP_USE_POOLED_BATCH_MANAGER, PropertyType.BOOLEAN);
        registry.register(PROP_RECOGNIZER, PropertyType.COMPONENT);
        registry.register(PROP_INPUT_DATA_PROCESSORS, 
                          PropertyType.COMPONENT_LIST);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {
        logger = ps.getLogger();
        cm = ps.getPropertyManager();
        skip = ps.getInt(PROP_SKIP, PROP_SKIP_DEFAULT);
        totalCount = ps.getInt(PROP_COUNT, PROP_COUNT_DEFAULT);
        if (totalCount <= 0) {
            totalCount = Integer.MAX_VALUE;
        }
        whichBatch = ps.getInt(PROP_WHICH_BATCH, PROP_WHICH_BATCH_DEFAULT);
        totalBatches = ps
                .getInt(PROP_TOTAL_BATCHES, PROP_TOTAL_BATCHES_DEFAULT);
        usePooledBatchManager = ps.getBoolean(PROP_USE_POOLED_BATCH_MANAGER,
                PROP_USE_POOLED_BATCH_MANAGER_DEFAULT);
        recognizer = (Recognizer) ps.getComponent(PROP_RECOGNIZER,
                Recognizer.class);
        inputDataProcessors = (List) ps.getComponentList
            (PROP_INPUT_DATA_PROCESSORS, DataProcessor.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the batch file to use for this recogition
     * 
     * @param batchFile
     *                the name of the batch file
     * @throws IOException
     *                 if the file could not be opened or read.
     */
    public void setBatchFile(String batchFile) throws IOException {
        if (usePooledBatchManager) {
            batchManager = new PooledBatchManager(batchFile, skip);
        } else {
            batchManager = new SimpleBatchManager(batchFile, skip, whichBatch,
                    totalBatches);
        }
    }

    /**
     * Decodes the batch of audio files
     * 
     * @throws IOException
     *                 if there is an I/O error processing the batch file
     */
    public void decode(String batchFile) {
        BatchItem batchItem;
        int count = 0;
        try {
            recognizer.allocate();
            setBatchFile(batchFile);

            batchManager.start();
            logger.info("BatchDecoder: decoding files in "
                    + batchManager.getFilename());
        
            while (count < totalCount && 
                        (batchItem = batchManager.getNextItem()) != null) {
                setInputStream(batchItem.getFilename());
                Result result = recognizer.recognize(batchItem.getTranscript());
                logger.info("File  : " + batchItem.getFilename());
                logger.info("Result: " + result);
                count++;
            }
            batchManager.stop();
        } catch (IOException io) {
            logger.severe("I/O error during decoding: " + io.getMessage());
        }
        recognizer.deallocate();
        logger.info("BatchDecoder: " + count + " files decoded");
    }
    
    
    /**
     * Sets the input stream to the given filename
     * @param filename the filename to set the input stream to
     * @return the InputStream representing the filename
     * @throws IOException if an error occurs
     */
    private void setInputStream(String filename) throws IOException {
        for (Iterator i = inputDataProcessors.iterator(); i.hasNext(); ) {
            DataProcessor dataSource = (DataProcessor) i.next();
            InputStream is = new FileInputStream(filename);
            if (dataSource instanceof StreamDataSource) {
                ((StreamDataSource) dataSource).setInputStream(is, filename);
            } else if (dataSource instanceof StreamCepstrumSource) {
                boolean isBigEndian = Utilities
                    .isCepstraFileBigEndian(filename);
                StreamCepstrumSource cepstrumSource =
                    (StreamCepstrumSource) dataSource;
                cepstrumSource.setInputStream(is, isBigEndian);
            }
        }
    }


    /**
     * Add commands to the given interpreter to support shell mode
     *
     *
     * @param ci the interpreter
     */
    private void addCommands(CommandInterpreter ci) {
	ci.add("ls", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1) {
                    ci.putResponse("Usage: ls");
                 } else {
                     String[] names = cm.getInstanceNames(Configurable.class);
                     for (int i = 0; i < names.length; i++) {
                         ci.putResponse(names[i]);
                     }
                }
                return "";
           }
            public String getHelp() {
                return "list active components";
            }
        });
	ci.add("show", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length < 2) {
                    cm.showConfig();
                 } else {
                     for (int i = 1; i < args.length; i++) {
                        String name = args[i];
                        cm.showConfig(name);
                     }
                }
                return "";
           }
            public String getHelp() {
                return "show component configuration";
            }
        });
	ci.add("edit", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 2) {
                    ci.putResponse("Usage: edit component");
                 } else {
                    cm.editConfig(args[1]);
                }
                return "";
           }
            public String getHelp() {
                return "edit a  component's configuration";
            }
        });
	ci.add("save", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 2) {
                    ci.putResponse("Usage: save filename.xml");
                 } else {
                    try {
                         cm.save(new File(args[1]));
                    } catch (IOException ioe) {
                        ci.putResponse("Can't save, " + ioe);
                    }
                }
                return "";
           }
            public String getHelp() {
                return "save configuration to a file";
            }
        });
	ci.add("set", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 4) {
                    ci.putResponse("Usage: set component property value");
                 } else {
                    try {
                        cm.setProperty(args[1], args[2], args[3]);
                    } catch (PropertyException pe) {
                        ci.putResponse(pe.toString());
                    }
                }
                return "";
           }
            public String getHelp() {
                return "set component property to a given value";
            }
        });
	ci.add("recognize", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length < 2) {
                    ci.putResponse("Usage: recognize audio [transcript]");
                 } else {
                    String audioFile = args[1];
                    String transcript = null;
                    if (args.length > 2)  {
                        transcript = args[2];
                    }
                    try {
                        setInputStream(audioFile);
                        Result result = recognizer.recognize(transcript);
                    } catch (IOException io) {
                        ci.putResponse("I/O error during decoding: " +
                            io.getMessage());
                    }
                }
                return "";
           }
            public String getHelp() {
                return "perform recognition on the given audio";
            }
        });
        ci.addAlias("recognize", "rec");

	ci.add("statsReset", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1) {
                    ci.putResponse("Usage: statsReset");
                 } else {
                    recognizer.resetMonitors();
                }
                return "";
           }
            public String getHelp() {
                return "resets gathered statistics";
            }
        });

	ci.add("batchRecognize", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1) {
                    ci.putResponse("Usage: batchRecognize");
                 } else {
                    try {
                        if (curBatchItem == null) {
                            batchManager.start();
                            curBatchItem = batchManager.getNextItem();
                        }
                        String audioFile = curBatchItem.getFilename();
                        String transcript = curBatchItem.getTranscript();
                        setInputStream(audioFile);
                        Result result = recognizer.recognize(transcript);
                    } catch (IOException io) {
                        ci.putResponse("I/O error during decoding: " +
                            io.getMessage());
                    }
                }
                return "";
           }
            public String getHelp() {
                return "perform recognition on the current batch item";
            }
        });
        ci.addAlias("batchRecognize", "br");

	ci.add("batchNext", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1 && args.length != 2) {
                    ci.putResponse("Usage: batchNext [norec]");
                 } else {
                    try {

                        // if we don't have a batch item, start (or
                        // start over)

                        if (curBatchItem == null) {
                            batchManager.start();
                        }
                        curBatchItem = batchManager.getNextItem();

                        // if we reach the end, just loop back and
                        // start over.

                        if (curBatchItem == null) {
                            batchManager.start();
                            curBatchItem = batchManager.getNextItem();
                        }

                        String audioFile = curBatchItem.getFilename();
                        String transcript = curBatchItem.getTranscript();
                        if (args.length == 2) {
                            ci.putResponse("Skipping: " + transcript);
                        } else {
                            setInputStream(audioFile);
                            Result result = recognizer.recognize(transcript);
                        }
                    } catch (IOException io) {
                        ci.putResponse("I/O error during decoding: " +
                            io.getMessage());
                    }
                }
                return "";
           }
            public String getHelp() {
                return "advance the batch and perform recognition";
            }
        });
        ci.addAlias("batchNext", "bn");

	ci.add("batchAll", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1) {
                    ci.putResponse("Usage: batchAll");
                 } else {
                    try {
                        if (curBatchItem == null) {
                            batchManager.start();
                        }

                        while (true) {
                            curBatchItem = batchManager.getNextItem();
                            // if we reach the end  bail out

                            if (curBatchItem == null) {
                                return "";
                            }
                            String audioFile = curBatchItem.getFilename();
                            String transcript = curBatchItem.getTranscript();
                            setInputStream(audioFile);
                            Result result = recognizer.recognize(transcript);
                        }
                    } catch (IOException io) {
                        ci.putResponse("I/O error during decoding: " +
                            io.getMessage());
                    }
                }
                return "";
           }
            public String getHelp() {
                return "recognize all of the remaining batch items";
            }
        });

	ci.add("batchReset", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 1) {
                    ci.putResponse("Usage: batchReset");
                 } else {
                    try {
                        batchManager.start();
                    } catch (IOException ioe) {
                        ci.putResponse("trouble reseting batch");
                    }
                }
                return "";
           }
           public String getHelp() {
               return "reset the batch to the beginning";
           }
        });
	ci.add("batchLoad", new CommandInterface() {
	     public String execute(CommandInterpreter ci, String[] args) {
                 if (args.length != 2) {
                    ci.putResponse("Usage: batchReset batchfile");
                 } else {
                    try {
                        setBatchFile(args[1]);
                    } catch (IOException ioe) {
                        ci.putResponse("Can't load " + args[1] + " " + ioe);
                    }
                }
                return "";
           }
           public String getHelp() {
               return "reset the batch to the beginning";
           }
        });
    }

    public void shell(String batchfile) {
        try {
            CommandInterpreter ci = new CommandInterpreter();
            ci.setPrompt("s4> "); 
            addCommands(ci);
            setBatchFile(batchfile);
            recognizer.allocate();
            ci.run();
            batchManager.stop();
            if (recognizer.getState() == RecognizerState.READY) {
                recognizer.deallocate();
            }
        } catch (IOException io) {
            logger.severe("I/O error during decoding: " + io.getMessage());
        }
    }

    /**
     * Main method of this BatchDecoder.
     * 
     * @param argv
     *                argv[0] : config.xml argv[1] : a file listing
     *                all the audio files to decode
     */
    public static void main(String[] argv) {
        if (argv.length < 2) {
            System.out.println(
                    "Usage: BatchDecoder propertiesFile batchFile [-shell]");
            System.exit(1);
        }
        String cmFile = argv[0];
        String batchFile = argv[1];
        ConfigurationManager cm;
        BatchModeRecognizer bmr = null;

        BatchModeRecognizer recognizer;
        try {
            URL url = new File(cmFile).toURI().toURL();
            cm = new ConfigurationManager(url);
            bmr = (BatchModeRecognizer) cm.lookup("batch");
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
            System.err.println("Can't find batchModeRecognizer in " + cmFile);
            return;
        }

        if (argv.length >= 3 && argv[2].equals("-shell")) {
            bmr.shell(batchFile);

        } else {
            bmr.decode(batchFile);
        }
    }
}
