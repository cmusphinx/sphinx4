#!/bin/bash

JDK=/HRI/External/java/1.6/linux-i686-gcc-glibc2.3.4

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$JDK"/jre/lib/i386/client/":$JDK"/jre/lib/i386" 

./cTranscriber
