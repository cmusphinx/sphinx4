import edu.cmu.sphinx.decoder.Decoder
import edu.cmu.sphinx.decoder.ResultListener
import edu.cmu.sphinx.decoder.pruner.SimplePruner
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager
import edu.cmu.sphinx.frontend.DataBlocker
import edu.cmu.sphinx.frontend.FrontEnd
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor
import edu.cmu.sphinx.frontend.feature.LiveCMN
import edu.cmu.sphinx.frontend.filter.Preemphasizer
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform
import edu.cmu.sphinx.frontend.util.AudioFileDataSource
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower
import edu.cmu.sphinx.instrumentation.BestPathAccuracyTracker
import edu.cmu.sphinx.instrumentation.MemoryTracker
import edu.cmu.sphinx.instrumentation.SpeedTracker
import edu.cmu.sphinx.jsgf.JSGFGrammar
import edu.cmu.sphinx.linguist.acoustic.UnitManager
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel
import edu.cmu.sphinx.linguist.dictionary.FastDictionary
import edu.cmu.sphinx.linguist.flat.FlatLinguist
import edu.cmu.sphinx.recognizer.Recognizer
import edu.cmu.sphinx.util.LogMath
import java.util.logging.Logger
import java.util.logging.Level

if (args.length < 1) {
  throw new Error("USAGE: GroovyTranscriber <sphinx4 root> [<WAV file>]")
}

def root = args[0]

// init common 
Logger.getLogger("").setLevel(Level.WARNING)
def logMath = new LogMath(1.0001f, true)
def absoluteBeamWidth = -1
def relativeBeamWidth = 1E-80
def wordInsertionProbability = 1E-36
def languageWeight = 8.0f

// init audio data
def audioSource = new AudioFileDataSource(3200, null)
def audioURL = (args.length > 1) ?
  new File(args[0]).toURI().toURL() :
  new URL("file:" + root + "/src/apps/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav")
audioSource.setAudioFile(audioURL, null)

// init front end
def dataBlocker = new DataBlocker(
        10 // blockSizeMs
)
def speechClassifier = new SpeechClassifier(
        10,     // frameLengthMs,
        0.003, // adjustment,
        10,     // threshold,
        0       // minSignal
)

def speechMarker = new SpeechMarker(
        200, // startSpeechTime,
        500, // endSilenceTime,
        100, // speechLeader,
        50,  // speechLeaderFrames
        100, // speechTrailer
        15.0 // endSilenceDecay
)

def nonSpeechDataFilter = new NonSpeechDataFilter()

def premphasizer = new Preemphasizer(
        0.97 // preemphasisFactor
)
def windower = new RaisedCosineWindower(
        0.46, // double alpha
        25.625f, // windowSizeInMs
        10.0f // windowShiftInMs
)
def fft = new DiscreteFourierTransform(
        -1, // numberFftPoints
        false // invert
)
def melFilterBank = new MelFrequencyFilterBank(
        130.0, // minFreq,
        6800.0, // maxFreq,
        40 // numberFilters
)
def dct = new DiscreteCosineTransform(
        40, // numberMelFilters,
        13  // cepstrumSize
)
def cmn = new LiveCMN(
        12.0, // initialMean,
        100,  // cmnWindow,
        160   // cmnShiftWindow
)
def featureExtraction = new DeltasFeatureExtractor(
        3 // window
)

def pipeline = [
        audioSource,
        dataBlocker,
        speechClassifier,
        speechMarker,
        nonSpeechDataFilter,
        premphasizer,
        windower,
        fft,
        melFilterBank,
        dct,
        cmn,
        featureExtraction
]

def frontend = new FrontEnd(pipeline)

// init models
def unitManager = new UnitManager()

def modelLoader = new Sphinx3Loader(
        "file:" + root + "/models/acoustic/tidigits",
        "mdef",
        "",
        logMath,
        unitManager,
        0.0f,
        1e-7f,
        0.0001f,
        true)

def model = new TiedStateAcousticModel(modelLoader, unitManager, true)

def dictionary = new FastDictionary(
        new URL("file:" + root + "/models/acoustic/tidigits/dict/dictionary"),
        new URL("file:" + root + "/models/acoustic/tidigits/noisedict"),
        new ArrayList<URL>(),
        false,
        "<sil>",
        false,
        false,
        unitManager)

// init linguist
def grammar = new JSGFGrammar(
        // URL baseURL,
        new URL("file:" + root + "/src/apps/edu/cmu/sphinx/demo/transcriber/"),
        logMath, // LogMath logMath,
        "digits", // String grammarName,
        false, // boolean showGrammar,
        false, // boolean optimizeGrammar,
        false, // boolean addSilenceWords,
        false, // boolean addFillerWords,
        dictionary // Dictionary dictionary
)

def linguist = new FlatLinguist(
        model, // AcousticModel acousticModel,
        logMath, // LogMath logMath,
        grammar, // Grammar grammar,
        unitManager, // UnitManager unitManager,
        wordInsertionProbability, // double wordInsertionProbability,
        1.0, // double silenceInsertionProbability,
        1.0, // double fillerInsertionProbability,
        1.0, // double unitInsertionProbability,
        languageWeight, // float languageWeight,
        false, // boolean dumpGStates,
        false, // boolean showCompilationProgress,
        false, // boolean spreadWordProbabilitiesAcrossPronunciations,
        false, // boolean addOutOfGrammarBranch,
        1.0, // double outOfGrammarBranchProbability,
        1.0, // double phoneInsertionProbability,
        null // AcousticModel phoneLoopAcousticModel
)

// init recognizer
def scorer = new ThreadedAcousticScorer(frontend, null, 10, true, 0, Thread.NORM_PRIORITY)

def pruner = new SimplePruner()

def activeListFactory = new PartitionActiveListFactory(absoluteBeamWidth, relativeBeamWidth, logMath)

def searchManager = new SimpleBreadthFirstSearchManager(
        logMath, linguist, pruner,
        scorer, activeListFactory,
        false, 0.0, 0, false)

def decoder = new Decoder(searchManager,
        false, false,
        new ArrayList<ResultListener>(),
        100000)

def recognizer = new Recognizer(decoder, null)

try{
    // allocate the resourcs necessary for the recognizer
    recognizer.allocate()
} catch (Exception e) {
    println(e)
    e.printStackTrace();
}

// Loop unitl last utterance in the audio file has been decoded, in which case the recognizer will return null.
def result
while ((result = recognizer.recognize()) != null) {
  def resultText = result.getBestResultNoFiller()
  println(resultText)
}
