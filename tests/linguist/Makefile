# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

#
#
# Makefile for all the linguist tests.
#

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/linguist

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =

# LOGGER_PROPERTIES=tidigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

NO_CONTEXT_FLAGS= -Dedu.cmu.sphinx.model.acoustic.useComposites=false -Dedu.cmu.sphinx.model.acoustic.useCDUnits=false


PROFILE_FLAGS= -Xprof -server
#PROFILE_FLAGS=  -Xprof
#PROFILE_FLAGS=  -Xrunhprof:file=dump.hprof,format=b
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

GC_FLAGS = -ms128m -mx2g

#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt

DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS) 

PARSED_EXTRA_OPTIONS=$(subst :, , $(EXTRA_OPTIONS))

##########################################################################

include ${TOP}/build/Makefile.config


# JAVA_FLAGS =  -Xmx1024m -ea -Xint

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: 


##########################################################################
#
# The linguist test
#
##########################################################################

# The full TIDGITS test.  Takes a long long time to run

large_flat_unigram:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=16\
	edu.cmu.sphinx.decoder.BatchDecoder large_flat_unigram.props large.batch

large_unigram:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=16\
	edu.cmu.sphinx.decoder.BatchDecoder large_unigram.props large.batch

large_error:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	edu.cmu.sphinx.decoder.BatchDecoder large_error.props single.batch

single_error:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=0\
	edu.cmu.sphinx.decoder.BatchDecoder single_error.props single.batch

large_bigram:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=16\
	-Dedu.cmu.sphinx.model.language.location=large.bigram \
	edu.cmu.sphinx.decoder.BatchDecoder large.props large.batch


run_batch:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.whichBatch=$(WHICH_BATCH)\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.totalBatches=$(TOTAL_BATCHES)\
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=$(SKIP)\
        $(PARSED_EXTRA_OPTIONS) \
	edu.cmu.sphinx.decoder.BatchDecoder $(PROPS) $(BATCH_FILE) \


small:
	$(JAVA_CMD)  \
        -Dedu.cmu.sphinx.util.LinguistDumper.filename=small.gdl \
        -Dedu.cmu.sphinx.model.language.location=small.unigram \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
        edu.cmu.sphinx.decoder.BatchDecoder small.props small.batch


tune:
	$(JAVA_CMD)  \
        -Dedu.cmu.sphinx.util.LinguistDumper.filename=small.gdl \
        -Dedu.cmu.sphinx.model.language.location=small.unigram \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
        edu.cmu.sphinx.decoder.AutoTuner file:./tune.props 

tidigits_quick:
	$(JAVA_CMD)  \
        -Dedu.cmu.sphinx.util.LinguistDumper.filename=small.gdl \
        -Dedu.cmu.sphinx.model.language.location=small.unigram \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
        edu.cmu.sphinx.decoder.BatchDecoder small.props small.batch

tiny:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=2000\
        -Dedu.cmu.sphinx.util.LinguistDumper.filename=tiny.gdl \
        -Dedu.cmu.sphinx.model.language.location=tiny.unigram \
        edu.cmu.sphinx.decoder.BatchDecoder tiny.props small.batch


med:
	$(JAVA_CMD)  \
        -Dedu.cmu.sphinx.model.language.location=med.bigram \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=200\
        edu.cmu.sphinx.decoder.BatchDecoder med.props med.batch

tiny_static:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=2000\
        -Dedu.cmu.sphinx.util.LinguistDumper.filename=tiny_static.gdl \
        edu.cmu.sphinx.decoder.BatchDecoder tiny_static.props small.batch
