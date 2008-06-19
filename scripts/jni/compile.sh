#!/bin/bash

JDK=/HRI/External/java/1.6/linux-i686-gcc-glibc2.3.4

cc -I$JDK"/include/linux" -L$JDK"/jre/lib/i386/client/" -L$JDK"/jre/lib/i386" -o cTranscriber -ljvm s4JNIExample.c

