# Copyrigh 1999-2003 Carnegie Mellon University.  
# Portions Copyright 2003 Sun Microsystems, Inc.  
# Portions Copyright 2003 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Nothing.

#
# Makefile for all the Ripley tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/ripley

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =


PROFILE_FLAGS= -Xprof
LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx512m -XX:NewSize=100m  -XX:MaxNewSize=100m

# GC_FLAGS = -ms256m -mx512m -XX:-UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewSize=100m -XX:MaxNewSize=100m 

LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

DEBUG_FLAGS= 

EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################
# 
# check the MODE setting. If invoked with MODE=quick, run a quick
# version of the test (5 times quicker)
# 
# example:      make MODE=quick ripley_flat_unigram
#
#

ifeq ($(MODE),quick)
  SKIP = 20
else
  SKIP = 0
endif

ifeq ($(MODE),super_quick)
  SKIP = 200
endif

ifeq ($(HEAP),large)
GC_FLAGS = -ms256m -mx512m
endif

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}


##########################################################################


ripley_flat_unigram:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch



ripley_trigram:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./ripley.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch



ripley_trigram_eep:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=15.5 \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=700 \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
        -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-200 \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startLow=5\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startHigh=6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.endLow=5\
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true\
	-Dedu.cmu.sphinx.frontend.FrameDropper.dropEveryNthFrame=2\
	-Dedu.cmu.sphinx.frontend.FrameDropper.replaceNthWithPrevious=true\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.addFillerWords=true\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.fillerInsertionProbability=.1\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./ripley.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch



ripley_trigram_lep:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=15.5 \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=700 \
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-80 \
        -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-200 \
	-Dedu.cmu.sphinx.frontend.bytesPerAudioFrame=320 \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.LevelTracker \
	-Dedu.cmu.sphinx.frontend.endpoint.LevelTracker.threshold=10 \
	-Dedu.cmu.sphinx.frontend.endpoint.AudioFilter.mergeSpeechSegments=true\
	-Dedu.cmu.sphinx.frontend.FrameDropper.dropEveryNthFrame=2\
	-Dedu.cmu.sphinx.frontend.FrameDropper.replaceNthWithPrevious=true\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.addFillerWords=true\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.fillerInsertionProbability=.1\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./ripley.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch



ripley_trigram_lep_beamfinder:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
	-Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.activeListType=edu.cmu.sphinx.decoder.search.PartitionActiveList\
	-Dedu.cmu.sphinx.decoder.Recognizer.searchManager=edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager\
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	-Dedu.cmu.sphinx.util.BeamFinder.enable=true\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.absoluteBeamWidth=8000\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-200 \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=15.5 \
	-Dedu.cmu.sphinx.frontend.bytesPerAudioFrame=320 \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.LevelTracker \
	-Dedu.cmu.sphinx.frontend.endpoint.AudioFilter.mergeSpeechSegments=true\
        -Dedu.cmu.sphinx.decoder.search.BreadthFirstSearchManager.relativeWordBeamWidth=1E-200 \
        -Dedu.cmu.sphinx.decoder.Recognizer.linguist=edu.cmu.sphinx.decoder.linguist.clextree.LexTreeLinguist \
	-Dedu.cmu.sphinx.frontend.endpoint.SingleThresholdEndpointer.energyThreshold=4.6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startLow=5\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startHigh=6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.endLow=5\
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=./ripley.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch
