# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Relative path to the "base" of the source tree
TOP = ../../../../..
PACKAGE_PATH = edu/cmu/sphinx/search/parallel

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built.  Start with generic
# packages going toward specialized.  That is, if one package depends
# on another, put the dependent package later in the list.
SUBDIRS = 
LOGGER_PROPERTIES=./simple_search.props
EXTRA_JAVA_FLAGS= -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}

run_simple_linguist:
	$(JAVA_CMD) edu.cmu.sphinx.search.SimpleLinguist

run_dt: ${CLASSES}
	$(JAVA_CMD) edu.cmu.sphinx.search.DicTest /lab/speech/sphinx4/data/wsj/dict/cmudict.0.6d 

