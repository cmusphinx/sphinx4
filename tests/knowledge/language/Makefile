# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Relative path to the top of the source tree
TOP = ../../..
PACKAGE_PATH = tests/knowledge/language

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}
EXTRA_JAVA_FLAGS=-mx128m

all:: ${CLASSES} ${STUBS}

LMGramTest:
	$(JAVA_CMD) -Xprof tests.knowledge.language.LMGramTest LMGramTest.props

LMGramTest_an4:
	$(JAVA_CMD) -Xprof \
	-Dedu.cmu.sphinx.knowledge.language.LanguageModel.location=an4_full.trigram.lm\
	tests.knowledge.language.LMGramTest \
	LMGramTest.props
