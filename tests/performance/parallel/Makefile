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
PACKAGE_PATH = tests/performance/parallel

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

# A quicker version of the TIDIGITS test. Take about a half hour to run.

tidigits_quick_parallel:
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.BatchDecoder.skip=5\
	-Dedu.cmu.sphinx.search.BreadthFirstSearchManager.showTokenCount=false\
	-Dedu.cmu.sphinx.search.WordListGrammar.path=./digits.wordlist\
	-Dedu.cmu.sphinx.util.GDLDumper.skipHMMs=false\
	edu.cmu.sphinx.decoder.BatchDecoder ./wsj_mfc_plp.props ../tidigits/tidigits.batch
