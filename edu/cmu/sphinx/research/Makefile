# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
# 
# Relative path to the "base" of the source tree
TOP = ../../../..
PACKAGE_PATH = edu/cmu/sphinx/research

# By default, build all of the .java files. 
FILES = $(shell echo *.java)

# List any sub directories that need to be built.  Start with generic
# packages going toward specialized.  That is, if one package depends
# on another, put the dependent package later in the list.
SUBDIRS = bushderby distributed parallel

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

# all:: ${CLASSES} ${STUBS}
