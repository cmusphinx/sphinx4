# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
# In following "ti46" refers to:
#
# "NIST CD-ROM Version of the Texas Instruments-developed 46-Word
# Speaker-Dependent Isolated Word Speech Database"
#
# "tidigits" refers to the:
#
# "NIST CD-ROM Version of the Texas Instruments-developed Studio Quality
# Speaker-Independent Connected-Digit Corpus".
#
# Makefile for all the TI46 tests.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/performance/ti46

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=ti46.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

NO_CONTEXT_FLAGS= -Dedu.cmu.sphinx.model.acoustic.useComposites=false -Dedu.cmu.sphinx.model.acoustic.useCDUnits=false


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

GC_FLAGS = -ms16m -mx16m

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
# The TI46 tests that use unit context and composite senones.
#
##########################################################################


#
# The full TI46 (isolated digits) test.
#
ti46:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

#
# The full TI46 (isolated digits) test.
#
ti46_eight:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-20\
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.eight.batch


#
# The full TI46 (isolated digits) test using the TreeLinguist.
#
ti46_tree:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
        -Dedu.cmu.sphinx.search.Linguist.wordInsertionProbability=1.0E-20\
	-Dedu.cmu.sphinx.search.StaticLinguist.isFlatSentenceHMM=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test.
#
ti46_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# The full TI46 (isolated digits) test, using a looping grammar.
#
ti46_multi:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=true \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test that uses a
# looping grammar.
#
ti46_multi_quick:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=true \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch

##########################################################################
#
# The TI46 tests that DO NOT use unit context and composite senones.
#
##########################################################################


#
# The full TI46 (isolated digits) test.
#
ti46_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test.
#
ti46_quick_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=false \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# The full TI46 (isolated digits) test, using a looping grammar.
#
ti46_multi_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=true \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch


#
# A quicker version of the TI46 (isolated digits) test that uses a
# looping grammar.
#
ti46_multi_quick_no_context:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=40\
	-Dedu.cmu.sphinx.search.WordListGrammar.isLooping=true \
	$(NO_CONTEXT_FLAGS) \
	edu.cmu.sphinx.decoder.BatchDecoder ti46.props ./ti46.batch
