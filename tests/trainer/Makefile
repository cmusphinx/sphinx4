# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Nothing.

#
# Makefile for all the trainer tests.
#

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/trainer

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built
SUBDIRS =


#GCSPY_FLAGS= -client -Xport3000 -Xverbose -Xwait
PROFILE_FLAGS= $(GCSPY_FLAGS)
PROFILE_FLAGS= -Xprof -server

#LOG_GC_FLAGS=  -Xloggc:gc.txt
GC_FLAGS = -ms256m -mx1g

LOGGER_PROPERTIES=trainer.props
LOG = -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

DEBUG_FLAGS=

EXTRA_JAVA_FLAGS=$(PROFILE_FLAGS) $(LOG_GC_FLAGS) $(LOG) $(GC_FLAGS) $(DEBUG_FLAGS)

ifeq ($(HEAP),large)
GC_FLAGS = -ms1024m -mx1600m
endif

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

default: trainer

trainer:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.knowledge.acoustic.location=. \
	edu.cmu.sphinx.trainer.Trainer trainer.props

init:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.trainer.Trainer.initialStage=_00_INITIALIZATION \
	-Dedu.cmu.sphinx.trainer.Trainer.finalStage=_00_INITIALIZATION \
	-Dedu.cmu.sphinx.knowledge.acoustic.location=. \
	edu.cmu.sphinx.trainer.Trainer trainer.props

ci:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.trainer.Trainer.initialStage=_10_CI_TRAIN \
	-Dedu.cmu.sphinx.trainer.Trainer.finalStage=_10_CI_TRAIN \
	-Dedu.cmu.sphinx.knowledge.acoustic.location=${PATH_SEP}lab${PATH_SEP}speech${PATH_SEP}sphinx4${PATH_SEP}data${PATH_SEP}save \
	edu.cmu.sphinx.trainer.Trainer trainer.props

copy:
	$(JAVA_CMD) \
	-Dedu.cmu.sphinx.trainer.Trainer.initialStage=_90_CP_MODEL \
	-Dedu.cmu.sphinx.trainer.Trainer.finalStage=_90_CP_MODEL \
	edu.cmu.sphinx.trainer.Trainer trainer.props
