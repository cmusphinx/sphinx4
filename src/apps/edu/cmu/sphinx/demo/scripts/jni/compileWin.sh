#!/bin/bash


gcc -mno-cygwin -I"C:\Program Files\Java\jdk1.6.0_11\include" -I"C:\Program Files\Java\jdk1.6.0_11\include\win32" -L"C:\Program Files\Java\jdk1.6.0_11\lib" -L"C:\Program Files\Java\jdk1.6.0_11\jre\lib\i386\client" -o cTranscriber -ljvm s4JNIExample.c "C:\Program Files\Java\jdk1.6.0_11\lib\jvm.lib"

