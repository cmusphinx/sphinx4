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
PACKAGE_PATH = tests/frontend

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 


##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}

cmnFeatExt: all
	@chmod +x testCmnFeatExt.sh
	sh ./testCmnFeatExt.sh

alltests: all
	@$(MAKE) cmnFeatExt

clean::
	rm -f *.out