# Copyright 2001 Sun Microsystems, Inc.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
#
# @(#)Makefile	1.2 01/07/12

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/other

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 

TEST_SUITE=tests.junit.AllTests
GUI_TEST_SUITE=junit.swingui.TestRunner $(TEST_SUITE)
TEXT_TEST_SUITE=junit.ui.TestRunner $(TEST_SUITE)
LOGGER_PROPERTIES=./am.props

EXTRA_JAVA_FLAGS= -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}
	@gcc -O3 MathTest.c -o MathTest

run: 
	$(JAVA_CMD) -server tests.perf.MathTest
	MathTest

client:
	$(JAVA_CMD) tests.perf.MathTest

server:
	$(JAVA_CMD) -server tests.perf.MathTest


MathTests: ${CLASSES}
	gcc -O3 MathTest.c


clean::
	@$(RM) *.out
	@$(RM)  $(CLASSES)
