#!/bin/bash

JDK=/usr/lib/jvm/java-6-sun/

cc -I$JDK"/include" -I$JDK"/include/linux" -L$JDK"/jre/lib/i386/client/" -L$JDK"/jre/lib/i386" -o cTranscriber -ljvm s4JNIExample.c

