#!/bin/bash

JDK=/your/path/here

cc -I$JDK"/include/linux" -L$JDK"/jre/lib/i386/client/" -L$JDK"/jre/lib/i386" -o cTranscriber -ljvm s4JNIExample.c

