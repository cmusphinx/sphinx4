#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-6-sun/

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JAVA_HOME"/jre/lib/i386/client/":$JAVA_HOME"/jre/lib/i386" 
echo "test "$LD_LIBRARY_PATH

./cTranscriber
