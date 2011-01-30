(import
  '(edu.cmu.sphinx.decoder Decoder)
  '(edu.cmu.sphinx.decoder ResultListener)
  '(edu.cmu.sphinx.decoder.pruner SimplePruner)
  '(edu.cmu.sphinx.decoder.scorer ThreadedAcousticScorer)
  '(edu.cmu.sphinx.decoder.search PartitionActiveListFactory)
  '(edu.cmu.sphinx.decoder.search SimpleBreadthFirstSearchManager)
  '(edu.cmu.sphinx.frontend DataBlocker)
  '(edu.cmu.sphinx.frontend FrontEnd)
  '(edu.cmu.sphinx.frontend.endpoint NonSpeechDataFilter)
  '(edu.cmu.sphinx.frontend.endpoint SpeechClassifier)
  '(edu.cmu.sphinx.frontend.endpoint SpeechMarker)
  '(edu.cmu.sphinx.frontend.feature DeltasFeatureExtractor)
  '(edu.cmu.sphinx.frontend.feature LiveCMN)
  '(edu.cmu.sphinx.frontend.filter Preemphasizer)
  '(edu.cmu.sphinx.frontend.frequencywarp MelFrequencyFilterBank)
  '(edu.cmu.sphinx.frontend.transform DiscreteCosineTransform)
  '(edu.cmu.sphinx.frontend.transform DiscreteFourierTransform)
  '(edu.cmu.sphinx.frontend.util AudioFileDataSource)
  '(edu.cmu.sphinx.frontend.window RaisedCosineWindower)
  '(edu.cmu.sphinx.instrumentation BestPathAccuracyTracker)
  '(edu.cmu.sphinx.instrumentation MemoryTracker)
  '(edu.cmu.sphinx.instrumentation SpeedTracker)
  '(edu.cmu.sphinx.jsgf JSGFGrammar)
  '(edu.cmu.sphinx.linguist.acoustic UnitManager)
  '(edu.cmu.sphinx.linguist.acoustic.tiedstate Sphinx3Loader)
  '(edu.cmu.sphinx.linguist.acoustic.tiedstate TiedStateAcousticModel)
  '(edu.cmu.sphinx.linguist.dictionary FastDictionary)
  '(edu.cmu.sphinx.linguist.flat FlatLinguist)
  '(edu.cmu.sphinx.recognizer Recognizer)
  '(edu.cmu.sphinx.util LogMath)
  '(java.util.logging Logger)
  '(java.util.logging Level)
  '(java.net URL)
  )

(def root "../../..")

;; init common 
(.setLevel (Logger/getLogger "") Level/WARNING)
(def logMath (new LogMath 1.0001 true))

(def absoluteBeamWidth -1)
(def relativeBeamWidth 1E-80)
(def wordInsertionProbability 1E-36)
(def languageWeight 8.0)

;; init audio data
(def audioSource (new AudioFileDataSource 3200 nil))
(def audioURL (new URL (str "file:" root "/src/apps/edu/cmu/sphinx/demo/transcriber/10001-90210-01803.wav")))
(.setAudioFile audioSource audioURL nil)

;;init front end
(def dataBlocker (new DataBlocker
  10)) ;; blockSizeMs

(def speechClassifier (new SpeechClassifier
  10 ;; frameLengthMs
  0.003 ;; adjustment
  10 ;; threshold
  0)) ;; minSignal

(def speechMarker (new SpeechMarker
  200 ;; startSpeechTime
  500 ;; endSilenceTime
  100 ;; speechLeader
  50 ;; speechLeaderFrames
  100 ;; speechTrailer
  15.0)) ;; decay

(def nonSpeechDataFilter (new NonSpeechDataFilter))

(def premphasizer (new Preemphasizer
  0.97)) ;; preemphasisFactor

(def windower (new RaisedCosineWindower
  0.46 ;; double alpha
  25.625 ;; windowSizeInMs
  10.0)) ;; windowShiftInMs

(def fft (new DiscreteFourierTransform
  -1 ;; numberFftPoints
  false)) ;; invert

(def melFilterBank (new MelFrequencyFilterBank
  130.0 ;; minFreq
  6800.0 ;; maxFreq
  40)) ;; numberFilters

(def dct (new DiscreteCosineTransform
  40 ;; numberMelFilters
  13)) ;; cepstrumSize

(def cmn (new LiveCMN
  12.0 ;; initialMean
  100 ;; cmnWindow
  160)) ;; cmnShiftWindow

(def featureExtraction (new DeltasFeatureExtractor
  3)) ;; window


(def pipeline [
  audioSource
  dataBlocker
  speechClassifier
  speechMarker
  nonSpeechDataFilter
  premphasizer
  windower
  fft
  melFilterBank
  dct
  cmn
  featureExtraction])

(def frontend (new FrontEnd pipeline))

;; init models
(def unitManager (new UnitManager))

(def dictionary (new FastDictionary
  (new URL (str "file:" root "/models/acoustic/tidigits/dict/dictionary"))
  (new URL (str "file:" root "/models/acoustic/tidigits/noisedict"))
  []
  false
  "<sil>"
  false
  false
  unitManager))

(def modelLoader (new Sphinx3Loader
  (new URL (str "file:" root "/models/acoustic/tidigits"))
  "mdef"
  ""
  logMath
  unitManager
  (float 0.0)
  (float 1e-7)
  (float 0.0001)
  true
  ))

(def model (new TiedStateAcousticModel modelLoader unitManager true))


;; init linguist
(def grammar (new JSGFGrammar
  ;; URL baseURL
  (new URL (str "file:" root "/src/apps/edu/cmu/sphinx/demo/transcriber/"))
  logMath ;; LogMath logMath
  "digits" ;; String grammarName
  false ;; boolean showGrammar
  false ;; boolean optimizeGrammar
  false ;; boolean addSilenceWords
  false ;; boolean addFillerWords
  dictionary)) ;; Dictionary dictionary

(def linguist (new FlatLinguist
  model ;; AcousticModel acousticModel
  logMath ;; LogMath logMath
  grammar ;; Grammar grammar
  unitManager ;; UnitManager unitManager
  wordInsertionProbability ;; double wordInsertionProbability
  1.0 ;; double silenceInsertionProbability
  1.0 ;; double fillerInsertionProbability
  1.0 ;; double unitInsertionProbability
  languageWeight ;; float languageWeight
  false ;; boolean dumpGStates
  false ;; boolean showCompilationProgress
  false ;; boolean spreadWordProbabilitiesAcrossPronunciations
  false ;; boolean addOutOfGrammarBranch
  1.0 ;; double outOfGrammarBranchProbability
  1.0 ;; double phoneInsertionProbability
  nil)) ;; AcousticModel phoneLoopAcousticModel

;; init recognizer
(def scorer (new ThreadedAcousticScorer frontend nil 10 true 0 Thread/NORM_PRIORITY))

(def pruner (new SimplePruner))

(def activeListFactory (new PartitionActiveListFactory absoluteBeamWidth relativeBeamWidth logMath))

(def searchManager (new SimpleBreadthFirstSearchManager
  logMath linguist pruner
  scorer activeListFactory
  false 0.0 0 false))

(def decoder (new Decoder
  searchManager
  false false
  []
  100000))

(def recognizer (new Recognizer decoder nil))

;; allocate the resources necessary for the recognizer
(.allocate recognizer)

;; Loop until last utterance in the audio file has been decoded in which case the recognizer will return null.
;;(print (.getBestResultNoFiller (.recognize recognizer)))

(loop
  [result (.recognize recognizer)]
  (if (not (= result nil))
    (let []
      (println (.getBestResultNoFiller result))
      (recur (.recognize recognizer)))))







