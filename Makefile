# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.

# Relative path to the "base" of the source tree
TOP = .

# List any sub directories that need to be built.  Start with generic
# packages going toward specialized.  That is, if one package depends
# on another, put the dependent package later in the list.
SUBDIRS = edu tests 

JSAPI_DIRS = com demo/jsapi
GTAR=/bin/tar

##########################################################################

####################    frontend.jar deploy macros    ####################


FRONT_END_JAR_STAGING_AREA = ./FrontEndJar
FRONT_END_JAR_FRONT_END = $(FRONT_END_JAR_STAGING_AREA)/edu/cmu/sphinx/frontend
FRONT_END_JAR_UTIL = $(FRONT_END_JAR_STAGING_AREA)/edu/cmu/sphinx/util


FRONT_END_CLASSES = \
	classes/edu/cmu/sphinx/frontend/*.class \
	classes/edu/cmu/sphinx/frontend/util/*.class \


FRONT_END_JAR_EXCLUDES = \
	$(FRONT_END_JAR_FRONT_END)/parallel \
	$(FRONT_END_JAR_FRONT_END)/SimpleFrontEnd.class


UTIL_CLASSES = \
	classes/edu/cmu/sphinx/util/*.class


##########################################################################

PUSH_DEST = /home/groups/c/cm/cmusphinx/htdocs/sphinx4
include ${TOP}/build/Makefile.config

#
# Any extra actions to perform when cleaning
#

clean::
	rm -rf $(CLASS_DEST_DIR)


#
# a quick and dirty implementation
#

javadocs:
	$(JAVADOC) -d $(DOC_DEST) -quiet -subpackages edu -exclude edu.cmu.sphinx.jsapi -source 1.4


#
# another quick and dirty implementation, with private methods/fields
#

javadocs_private:
	$(JAVADOC) -d $(DOC_DEST) -private -quiet -subpackages edu -exclude edu.cmu.sphinx.jsapi -source 1.4


push_javadocs:
	$(MAKE) javadocs
	$(GTAR) cf /tmp/sphinx4docs.tar -C $(DOC_DEST) .
	sscp /tmp/sphinx4docs.tar
	sshh tar xf sphinx4docs.tar -C $(PUSH_DEST)
	rm /tmp/sphinx4docs.tar

stest:
	$(GTAR) cfC /tmp/sphinx4docs.tar $(DOC_DEST) .
	$(GTAR) xfC /tmp/sphinx4docs.tar /home/plamere/stest


#
# the jsapi stuff is kept separate since it requires the jsapi.jar
# which is not available by default (due to licensing issues)
#

jsapi:
	@for subdir in $(JSAPI_DIRS)  ; do \
	    (echo "Building in $$subdir" ; cd $$subdir && $(MAKE) $(MAKEDEFS) ) || (echo "Skipping $$subdir");\
	done



#
# Creates a frontend.jar file that can be used in a standalone fashion.
#

frontend.jar:
	rm -f frontend.jar
	rm -rf $(FRONT_END_JAR_STAGING_AREA)
	($(MAKE) all; \
	mkdir -p $(FRONT_END_JAR_FRONT_END); \
	cp -r $(FRONT_END_CLASSES) $(FRONT_END_JAR_FRONT_END); \
	rm -rf $(FRONT_END_JAR_EXCLUDES); \
	mkdir -p $(FRONT_END_JAR_UTIL); \
	cp -r $(UTIL_CLASSES) $(FRONT_END_JAR_UTIL); \
	(cd $(FRONT_END_JAR_STAGING_AREA); $(JAR) cvf frontend.jar *); \
	chmod a+x $(FRONT_END_JAR_STAGING_AREA)/frontend.jar; \
	mv $(FRONT_END_JAR_STAGING_AREA)/frontend.jar .; \
	rm -rf $(FRONT_END_JAR_STAGING_AREA); )
