# Copyright 2001 Sun Microsystems, Inc.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
#

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = demo/jsapi/cardmatch

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

EXTRA_JARS=$(EXTRA_LIB)/jsapi.jar$(CSEP)$(EXTRA_LIB)/freetts/freetts.jar$(CSEP)$(EXTRA_LIB)/freetts/cmukal8.jar$(CSEP)$(EXTRA_LIB)/freetts/cmukal16.jar$(CSEP)$(EXTRA_LIB)/freetts/cmulex.jar

# List any sub directories that need to be built
SUBDIRS = 

PROFILE_FLAGS= -server
LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx800m

LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

DEBUG_FLAGS=
#EXTRA_JAVA_FLAGS=$(LOG_GC_FLAGS) $(LOG)
EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}


all:: ${CLASSES} ${STUBS}

run:
	$(JAVA_CMD) demo.jsapi.cardmatch.CardMatch cardmatch.props

run-ep:
	$(JAVA_CMD) \
	-Ddemo.jsapi.cardmatch.doEndpointing=true \
	-Dedu.cmu.sphinx.frontend.endpointer=edu.cmu.sphinx.frontend.endpoint.EnergyEndpointer \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startHigh=12 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.endLow=9 \
	-Dedu.cmu.sphinx.frontend.EnergyEndpointer.startWindow=8 \
	-Dedu.cmu.sphinx.frontend.filterNonSpeech=true \
	-Dedu.cmu.sphinx.frontend.NonSpeechFilter.mergeSpeechSegments=false \
	demo.jsapi.cardmatch.CardMatch \
	cardmatch.props

grammarTest:
	$(JAVA_CMD) demo.jsapi.cardmatch.JSGFGrammarTest cardmatch.props
