# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# "tidigits" refers to the:
#
# "NIST CD-ROM Version of the Texas Instruments-developed Studio Quality
# Speaker-Independent Connected-Digit Corpus".
#
# Makefile for all the TIDIGITS tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/tidigits

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

NO_CONTEXT_FLAGS= -Dedu.cmu.sphinx.model.acoustic.useComposites=false -Dedu.cmu.sphinx.model.acoustic.useCDUnits=false


#PROFILE_FLAGS= -Xprof -server
PROFILE_FLAGS= 
LOG_GC_FLAGS=  -Xloggc:gc.txt
 GC_FLAGS=  -XX:+PrintGCDetails
# GC_FLAGS=  -XX:+UseConcMarkSweepGC
#GC_FLAGS=  -XX:+UseParallelGC
#GC_FLAGS= -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails
HEAP_DUMP =   -Xrunhprof:heap=dump,cpu=samples
#GC_FLAGS= -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails -XX:+TraceGen1Time

# GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

#GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails
#GC_FLAGS= -ms1g -XX:+UseConcMarkSweepGC -XX:-UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

GC_FLAGS = -ms128m -mx128m

#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt

DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS) 

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: 


##########################################################################
#
# The TIDIGITS tests that uses TreeLinguist.
#
##########################################################################



# The full TIDGITS test.  Takes a long long time to run

tidigits:
	$(JAVA_CMD) edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# A quicker version of the TIDIGITS test. Take about a half hour to run.

tidigits_quick:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# tidigits_quick but with endpointing

tidigits_quick_ep:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startLow=2 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startHigh=6 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endLow=2 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endHigh=6 \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# A very quick version of the TIDIGITS test. Takes < 5 minutes to run.

tidigits_very_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=50\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# A super quick version of the TIDIGITS test. Takes < 5 minutes to run.

tidigits_super_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch


tidigits_super_quick_align:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	edu.cmu.sphinx.decoder.BatchAligner tidigits_aligner.props tidigits.batch



##########################################################################
#
# The TIDIGITS tests that uses FlatLinguist.
#
##########################################################################



# The TIDIGITS test, but uses the TreeLinguist instead of the FlatLinguist

tidigits_flat:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0 \
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=true \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# A quick version of the TIDIGITS test, but uses the TreeLinguist
# instead of the FlatLinguist

tidigits_quick_flat:
	$(JAVA_CMD)   \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=false \
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	-Dtests.search.Decoder.showErrorToken=true \
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=true \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



# A very quick version of the TIDIGITS test, but uses the TreeLinguist
# instead of the FlatLinguist

tidigits_super_quick_flat:
	$(JAVA_CMD)   \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true\
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=true \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props ./tidigits.batch



# A quick version of the TIDIGITS test, but uses the TreeLinguist
# instead of the FlatLinguist

tidigits_quick_flat_no_context:
	$(JAVA_CMD)   \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=false \
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	-Dtests.search.Decoder.showErrorToken=true \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=10E-1 \
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=true \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props tidigits.batch



##########################################################################
#
# TIDIGITS tests using the Wall Street Journal acoustic models
#
##########################################################################



# A quick version of the TIDIGITS test using the WSJ acoustic
# models

wsj_tidigits_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1E-26 \
	edu.cmu.sphinx.decoder.BatchDecoder wsjDigits.props \
	./tidigits.batch



# A very quick version of the TIDIGITS test using the WSJ acoustic
# models

wsj_tidigits_super_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false\
	edu.cmu.sphinx.decoder.BatchDecoder wsjDigits.props \
	./tidigits.batch



# A very quick version of the TIDIGITS test using the WSJ acoustic
# models and the tree linguist

wsj_tidigits_super_quick_tree:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.enableBushterbi=false\
	edu.cmu.sphinx.decoder.BatchDecoder wsjDigits.props \
	./tidigits.batch



# A very quick version of the TIDIGITS test using the WSJ acoustic
# models and the tree linguist
#
# Experimental bushderbi algorithm

wsj_tidigits_super_quick_bush:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=true \
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.enableBushterbi=true\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-100 \
	edu.cmu.sphinx.decoder.BatchDecoder wsjDigits.props \
	./tidigits.batch



# A very quick version of TIDIGITS test using the TreeLinguist and an FST.

wsj_tidigits_super_quick_tree_fst:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=true \
	-Dedu.cmu.sphinx.search.Recognizer.grammar=edu.cmu.sphinx.search.ArpaGrammar\
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=800\
	-Dedu.cmu.sphinx.search.Dictionary.wordReplacement=lollapalooza\
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-100 \
	-Dedu.cmu.sphinx.search.ArpaGrammar.path=/lab/speech/sphinx4/data/fst/digits.fst \
        -Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	edu.cmu.sphinx.decoder.BatchDecoder wsjDigits.props \
	./tidigits.batch




##########################################################################
#
# Miscellaneous tests
#
##########################################################################



prune_test:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=1000000\
	-Dtests.search.Decoder.showBestToken=false \
	-Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1e-100\
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props \
	./tidigits.batch

prune_test2:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dedu.cmu.sphinx.search.ActiveList.absoluteBeamWidth=1000000\
	-Dtests.search.Decoder.showBestToken=false \
	-Dedu.cmu.sphinx.search.ActiveList.relativeBeamWidth=1e-100\
	edu.cmu.sphinx.decoder.BatchDecoder tidigits.props \
	./tidigits.batch

