# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.


# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/live

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 

EXTRA_JAVA_FLAGS= -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

# LOGGER_PROPERTIES=tiDigits.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

PROFILE_FLAGS= -Xprof 
LOG_GC_FLAGS= -Xloggc:gc.txt
GC_FLAGS= -XX:+PrintGCDetails
GC_FLAGS = -ms128m -mx512m
# GC_FLAGS=  -XX:+UseConcMarkSweepGC
#GC_FLAGS=  -XX:+UseParallelGC
#GC_FLAGS= -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails
HEAP_DUMP = -Xrunhprof:heap=dump,cpu=samples
#GC_FLAGS= -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+PrintGCDetails -XX:+TraceGen1Time


#GC_FLAGS= -ms1g -XX:-UseConcMarkSweepGC -XX:+UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails
#GC_FLAGS= -ms1g -XX:+UseConcMarkSweepGC -XX:-UseParallelGC -XX:ParallelGCThreads=8 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

#GC_FLAGS= -ms512m -mx512m -XX:-UseConcMarkSweepGC -XX:+UseParNewGC -XX:ParallelGCThreads=4 -XX:NewSize=28m -XX:MaxNewSize=28m -XX:+TraceGen0Time -XX:+TraceGen1Time -XX:+PrintGCDetails

#PROFILE2_FLAGS= -Xrunhprof:file=log.txt
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt

DEBUG_FLAGS= 
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)


# PROFILE_FLAGS= -Xprof -server
# PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt
# PROFILE2_FLAGS= -Xrunhprof:file=log.txt

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}



# Runs the default LiveDemo

live: all
	$(JAVA_CMD) \
	-DshowPartialResults=true \
	-Dedu.cmu.sphinx.frontend.cmn.live=true \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startHigh=12 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startWindow=8 \
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true \
	tests.live.Live decoders.list



# Runs the LiveDemo in hands-free mode, i.e., the endpointer will
# determine the speech endpoints automatically. No buttons pressed.

live-free: all
	$(JAVA_CMD) \
	-DhandsFree=true \
	-Dedu.cmu.sphinx.frontend.NonSpeechFilter.mergeSpeechSegments=false \
	-Dedu.cmu.sphinx.frontend.cmn.live=true \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startHigh=12 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startWindow=8 \
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true \
	tests.live.Live decoders.list



# Runs the LiveDemo with the "Alpha Spelling" test loaded first.

live-alpha: all
	$(JAVA_CMD) \
	-DfirstDecoder="Alpha Spelling" \
	-DshowPartialResults=true \
	-Dedu.cmu.sphinx.frontend.cmn.live=true \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.EnergyEndpointer \
	-D
	tests.live.Live decoders.list
