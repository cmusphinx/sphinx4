# [[[copyright]]]

# Relative path to the top of the source tree
TOP = ../..
PACKAGE_PATH = tests/live

# By default, build all of the .java files. 
FILES = $(shell echo *.java)
# List any sub directories that need to be built
SUBDIRS = 

EXTRA_JAVA_FLAGS= -Djava.util.logging.config.file=$(LOGGER_PROPERTIES)


PROFILE_FLAGS= -Xprof -server
PROFILE2_FLAGS= -Xrunhprof:cpu=samples,file=log.txt
PROFILE2_FLAGS= -Xrunhprof:file=log.txt

##########################################################################

include ${TOP}/build/Makefile.config

CLASSES = ${FILES:%.java=${CLASS_DEST_DIR}/${PACKAGE_PATH}/%.class}

all:: ${CLASSES} ${STUBS}



# Runs the default LiveDemo

live: all
	$(JAVA_CMD) -DshowPartialResults=true tests.live.Live decoders.list



# Runs the LiveDemo in hands-free mode, i.e., the endpointer will
# determine the speech endpoints automatically. No buttons pressed.

live-free: all
	$(JAVA_CMD) \
	-DhandsFree=true \
	-Dedu.cmu.sphinx.frontend.NonSpeechFilter.mergeSpeechSegments=false \
	tests.live.Live decoders.list



# Runs the LiveDemo with the "Alpha Spelling" test loaded first.

live-alpha: all
	$(JAVA_CMD) \
	-DfirstDecoder="Alpha Spelling" \
	tests.live.Live decoders.list



