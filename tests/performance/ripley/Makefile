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


GCSPY_FLAGS= -client -Xport3000 -Xverbose -Xwait
PROFILE_FLAGS= $(GCSPY_FLAGS)
PROFILE_FLAGS= -Xprof -server

LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx512m

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

DEBUG_FLAGS= -XX:+UsePerfData
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
  SKIP = 5
else
  SKIP = 0
endif

ifeq ($(MODE),super_quick)
  SKIP = 200
endif

ifeq ($(HEAP),large)
GC_FLAGS = -ms512m -mx1024m
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
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=/lab/speech/sphinx4/data/medialab/language_models/mit.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch


ripley_trigram_ep:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160 \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startLow=5\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startHigh=6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.endLow=5\
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=/lab/speech/sphinx4/data/medialab/language_models/mit.trigram.lm \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch


ripley_trigram_ep_5:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.sdecoder.linguist.Linguist.languageWeight=6\
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startLow=5\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startHigh=6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.endLow=5\
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=/lab/speech/sphinx4/data/medialab/language_models/mit.trigram.lm.5 \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch.5



ripley_trigram_ep_2_lw16:
	$(JAVA_CMD) \
        -Dedu.cmu.sphinx.decoder.linguist.Linguist.languageWeight=16 \
	-Dedu.cmu.sphinx.decoder.linguist.Linguist.wordInsertionProbability=.1\
	-Dedu.cmu.sphinx.decoder.search.ActiveList.relativeBeamWidth=1E-160 \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startLow=5\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.startHigh=6\
	-Dedu.cmu.sphinx.frontend.endpoint.EnergyEndpointer.endLow=5\
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true\
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=/lab/speech/sphinx4/data/medialab/language_models/mit.trigram.lm.2 \
	edu.cmu.sphinx.decoder.BatchDecoder ./ripley.props /lab/speech/sphinx4/data/medialab/mit.batch.2

