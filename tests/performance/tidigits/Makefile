# [[[copyright]]] 
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



PROFILE_FLAGS= -Xprof -server
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

GC_FLAGS = -ms1g -mx1g

#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt

DEBUG_FLAGS=
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}


##########################################################################
#
# The TIDIGITS tests that uses FlatLinguist.
#
##########################################################################



# The full TIDGITS test.  Takes a long long time to run

tidigits:
	$(JAVA_CMD) tests.decoder.BatchDecoder tidigits.props tidigits.batch



# A quicker version of the TIDIGITS test. Take about a half hour to run.

tidigits_quick:
	$(JAVA_CMD)  \
	-Dtests.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false\
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



# tidigits_quick but with endpointing

tidigits_quick_ep:
	$(JAVA_CMD)  \
	-Dtests.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startLow=2 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startHigh=6 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endLow=2 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endHigh=6 \
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



# A very quick version of the TIDIGITS test. Takes < 5 minutes to run.

tidigits_very_quick:
	$(JAVA_CMD) \
	-Dtests.decoder.BatchDecoder.skip=50\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



# A super quick version of the TIDIGITS test. Takes < 5 minutes to run.

tidigits_super_quick:
	$(JAVA_CMD) \
	-Dtests.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=true \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



tidigits_super_quick_align:
	$(JAVA_CMD) \
	-Dtests.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.search.Dictionary.addSilEndingPronunciation=false \
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dtests.search.Decoder.showPartialResults=false \
	-Dtests.search.Decoder.showBestToken=false \
	tests.decoder.BatchAligner tidigits_aligner.props tidigits.batch



##########################################################################
#
# The TIDIGITS tests that uses TreeLinguist.
#
##########################################################################


# The TIDIGITS test, but uses the TreeLinguist instead of the FlatLinguist

tidigits_tree:
	$(JAVA_CMD) \
	-Dtests.decoder.BatchDecoder.skip=0\
	-Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.TreeLinguist\
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



# A quick version of the TIDIGITS test, but uses the TreeLinguist
# instead of the FlatLinguist

tidigits_quick_tree:
	$(JAVA_CMD)   \
	-Dtests.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true\
	-Dedu.cmu.sphinx.search.Linguist.joinPronunciations=false \
	-Dtests.search.Decoder.showPartialResults=false \
	-Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-36 \
	-Dtests.search.Decoder.showBestToken=false \
	-Dtests.search.Decoder.showErrorToken=true \
	-Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.TreeLinguist\
	tests.decoder.BatchDecoder tidigits.props tidigits.batch



# A very quick version of the TIDIGITS test, but uses the TreeLinguist
# instead of the FlatLinguist

tidigits_super_quick_tree:
	$(JAVA_CMD)   \
	-Dtests.decoder.BatchDecoder.skip=200\
	-Dedu.cmu.sphinx.model.acoustic.useComposites=false\
	-Dedu.cmu.sphinx.search.Linguist.expandInterNodeContexts=true\
	-Dedu.cmu.sphinx.search.Linguist.showSentenceHMM=false\
	-Dedu.cmu.sphinx.search.Recognizer.linguist=edu.cmu.sphinx.search.TreeLinguist\
	tests.decoder.BatchDecoder tidigits.props ./tidigits.batch



