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
PACKAGE_PATH = tests/search

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}
EXTRA_JAVA_FLAGS=-mx128m

all:: ${CLASSES} ${STUBS}

FSTGrammarTestTiny: all
	$(JAVA_CMD) tests.search.FSTGrammarTest FSTGrammarTest.props

FSTGrammarTestSmall: all
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=/lab/speech/sphinx4/data/fst/digits.fst\
    tests.search.FSTGrammarTest FSTGrammarTest.props


#################################################################
# loads a large model. 
FSTGrammarTestLarge: all
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=/lab/speech/sphinx4/data/fst/5721_word_vocab.FST \
	-Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.dictionaryPath=/lab/speech/sphinx4/data/wsj/dict/cmudict.0.6d \
	tests.search.FSTGrammarTest FSTGrammarTest.props


#################################################################
# loads a huge model. Don't expect this to finish anytime soon
FSTGrammarTestMega: all
	$(JAVA_CMD)  \
	-Dedu.cmu.sphinx.decoder.linguist.FSTGrammar.path=/lab/speech/sphinx4/data/fst/language_model.arpaformat.FST.ascii \
	-Dedu.cmu.sphinx.knowledge.dictionary.Dictionary.dictionaryPath=/lab/speech/sphinx4/data/wsj/dict/cmudict.0.6d \
	tests.search.FSTGrammarTest FSTGrammarTest.props


#################################################################
#
# Tests the token Partitioner
#
partitionertest: all
	$(JAVA_CMD) \
	tests.search.PartitionerTest

