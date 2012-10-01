import sys

libDir = "../../../lib/"
classPaths = [
    "sphinx4.jar",
    "jsapi.jar",
    "WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz.jar"
]
for classPath in classPaths:
    sys.path.append(libDir + classPath)

true = 1
false = 0

from edu.cmu.sphinx.decoder import Decoder
from edu.cmu.sphinx.decoder import ResultListener
from edu.cmu.sphinx.decoder.pruner import SimplePruner
from edu.cmu.sphinx.decoder.scorer import ThreadedAcousticScorer
from edu.cmu.sphinx.decoder.search import PartitionActiveListFactory
from edu.cmu.sphinx.decoder.search import SimpleBreadthFirstSearchManager
from edu.cmu.sphinx.frontend import DataBlocker
from edu.cmu.sphinx.frontend import FrontEnd
from edu.cmu.sphinx.frontend.endpoint import NonSpeechDataFilter
from edu.cmu.sphinx.frontend.endpoint import SpeechClassifier
from edu.cmu.sphinx.frontend.endpoint import SpeechMarker
from edu.cmu.sphinx.frontend.feature import DeltasFeatureExtractor
from edu.cmu.sphinx.frontend.feature import LiveCMN
from edu.cmu.sphinx.frontend.filter import Preemphasizer
from edu.cmu.sphinx.frontend.frequencywarp import MelFrequencyFilterBank
from edu.cmu.sphinx.frontend.transform import DiscreteCosineTransform
from edu.cmu.sphinx.frontend.transform import DiscreteFourierTransform
from edu.cmu.sphinx.frontend.util import AudioFileDataSource
from edu.cmu.sphinx.frontend.window import RaisedCosineWindower
from edu.cmu.sphinx.instrumentation import BestPathAccuracyTracker
from edu.cmu.sphinx.instrumentation import MemoryTracker
from edu.cmu.sphinx.instrumentation import SpeedTracker
from edu.cmu.sphinx.jsgf import JSGFGrammar
from edu.cmu.sphinx.linguist.acoustic import UnitManager
from edu.cmu.sphinx.linguist.acoustic.tiedstate import Sphinx3Loader
from edu.cmu.sphinx.linguist.acoustic.tiedstate import TiedStateAcousticModel
from edu.cmu.sphinx.linguist.dictionary import FastDictionary
from edu.cmu.sphinx.linguist.flat import FlatLinguist
from edu.cmu.sphinx.recognizer import Recognizer
from edu.cmu.sphinx.util import LogMath
from java.util.logging import Logger
from java.util.logging import Level
from java.net import URL
from java.util import ArrayList

# if (args.length < 1) {
#  throw new Error("USAGE: GroovyTranscriber <sphinx4 root> [<WAV file>]")
# }

root = "../../.."

# init common 
Logger.getLogger("").setLevel(Level.WARNING)
logMath = LogMath(1.0001, true)
absoluteBeamWidth = -1
relativeBeamWidth = 1E-80
wordInsertionProbability = 1E-36
languageWeight = 8.0

# init audio data
audioSource = AudioFileDataSource(3200, None)
audioURL =  URL("file:" + root + "/src/apps/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav")
# (args.length > 1) ?
#  File(args[0]).toURI().toURL() :
 
audioSource.setAudioFile(audioURL, None)

# init front end
dataBlocker = DataBlocker(
        10 # blockSizeMs
)
speechClassifier = SpeechClassifier(
        10,     # frameLengthMs,
        0.003, # adjustment,
        10,     # threshold,
        0       # minSignal
)

speechMarker = SpeechMarker(
        200, # startSpeechTime,
        500, # endSilenceTime,
        100, # speechLeader,
        50,  # speechLeaderFrames
        100, # speechTrailer
        15   # endSilenceDecay
)

nonSpeechDataFilter = NonSpeechDataFilter()

premphasizer = Preemphasizer(
        0.97 # preemphasisFactor
)
windower = RaisedCosineWindower(
        0.46, # double alpha
        25.625, # windowSizeInMs
        10.0 # windowShiftInMs
)
fft = DiscreteFourierTransform(
        -1, # numberFftPoints
        false # invert
)
melFilterBank = MelFrequencyFilterBank(
        130.0, # minFreq,
        6800.0, # maxFreq,
        40 # numberFilters
)
dct = DiscreteCosineTransform(
        40, # numberMelFilters,
        13  # cepstrumSize
)
cmn = LiveCMN(
        12.0, # initialMean,
        100,  # cmnWindow,
        160   # cmnShiftWindow
)
featureExtraction = DeltasFeatureExtractor(
        3 # window
)

pipeline = [
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

frontend = FrontEnd(pipeline)

# init models
unitManager = UnitManager()

modelLoader = Sphinx3Loader(
        "file:" + root + "/bld/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz",
        "mdef",
        "",
        logMath,
        unitManager,
        0.0,
        1e-7,
        0.0001,
        true)

model = TiedStateAcousticModel(modelLoader, unitManager, true)

dictionary = FastDictionary(
        URL("file:" + root + "/bld/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/dict/cmudict.0.6d"),
        URL("file:" + root + "/bld/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/noisedict"),
        ArrayList(),
        false,
        "<sil>",
        false,
        false,
        unitManager)

# init linguist
grammar = JSGFGrammar(
        # URL baseURL,
        URL("file:" + root + "/src/apps/edu/cmu/sphinx/demo/transcriber/"),
        logMath, # LogMath logMath,
        "digits", # String grammarName,
        false, # boolean showGrammar,
        false, # boolean optimizeGrammar,
        false, # boolean addSilenceWords,
        false, # boolean addFillerWords,
        dictionary # Dictionary dictionary
)

linguist = FlatLinguist(
        model, # AcousticModel acousticModel,
        logMath, # LogMath logMath,
        grammar, # Grammar grammar,
        unitManager, # UnitManager unitManager,
        wordInsertionProbability, # double wordInsertionProbability,
        1.0, # double silenceInsertionProbability,
        1.0, # double fillerInsertionProbability,
        1.0, # double unitInsertionProbability,
        languageWeight, # float languageWeight,
        false, # boolean dumpGStates,
        false, # boolean showCompilationProgress,
        false, # boolean spreadWordProbabilitiesAcrossPronunciations,
        false, # boolean addOutOfGrammarBranch,
        1.0, # double outOfGrammarBranchProbability,
        1.0, # double phoneInsertionProbability,
        None # AcousticModel phoneLoopAcousticModel
)

# init recognizer
scorer = ThreadedAcousticScorer(frontend, None, 10, true, 0, 5)

pruner = SimplePruner()

activeListFactory = PartitionActiveListFactory(absoluteBeamWidth, relativeBeamWidth, logMath)

searchManager = SimpleBreadthFirstSearchManager(
        logMath, linguist, pruner,
        scorer, activeListFactory,
        false, 0.0, 0, false)

decoder = Decoder(searchManager,
        false, false,
        ArrayList(),
        100000)

recognizer = Recognizer(decoder, None)

# allocate the resourcs necessary for the recognizer
recognizer.allocate()

# Loop unitl last utterance in the audio file has been decoded, in which case the recognizer will return None.
result = recognizer.recognize()
while (result != None):
    resultText = result.getBestResultNoFiller()
    print resultText
    result = recognizer.recognize()
