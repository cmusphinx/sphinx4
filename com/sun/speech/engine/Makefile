# Copyright 2001 Sun Microsystems, Inc.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
#

# Relative path to the top of the source tree
TOP = ../../../..
PACKAGE_PATH = com/sun/speech/engine

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

EXTRA_JARS=$(EXTRA_LIB)/jsapi.jar

# List any sub directories that need to be built
SUBDIRS = recognition

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}


