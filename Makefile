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
SUBDIRS = com edu tests  

GTAR=/bin/tar
ifeq ($(EXTERNAL_JSAPI_LOCATION),)
EXTERNAL_JSAPI_LOCATION=/lab/speech/sphinx4/lib/jsapi.jar
endif
JSAPI_DEST=${TOP}/lib/jsapi.jar


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

#
# Augment the 'all' target with a fetch of the jsapi.jar
#

all:: $(JSAPI_DEST)

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

# #############################################
# Targets to get jsapi.jar
# ############################################
# the lib/jsapi.jar file requires the user to click through a license 
# agreement, thus it cannot be distributed in unpacked form. This set of
# targets will allow nightly builds to  work despite the fact that the 
# jsapi.jar  is not included in the distribution.  If the jsapi.jar
# is already in the proper place (the lib directory), nothing needs to
# be done. However, if it is not there, an attempt is made to copy it
# from EXTERNAL_JSAPI_LOCATION (currently /lab/speech/sphinx4/jsapi.jar). 
# If it is not
# there, then an error message is displayed indicating that the jsapi.jar
# needs to be unpacked in the lib directory.  For our test systems 
# we need to install jsapi.jar in /lab/speech/sphinx4/jsapi.jar


#
# copy in the jsapi.jar if it doesn't exist
# 
$(JSAPI_DEST): 
	@$(MAKE) fetch_jsapi


#
# if the jsapi.jar isn't in the EXTERNAL_JSAPI_LOCATION then
# issue some unpacking instructions
# 
fetch_jsapi: $(EXTERNAL_JSAPI_LOCATION)
	@cp $(EXTERNAL_JSAPI_LOCATION) $(JSAPI_DEST)

#
# Issue the unpacking instructions
# 
$(EXTERNAL_JSAPI_LOCATION):
	@echo ""
	@echo "Cannot find lib/jsapi.jar needed to build Sphinx-4.  Please"
	@echo "extract it by running jsapi.sh or jsapi.exe from the lib"
	@echo "directory."
	@echo ""

# ##########################################################

