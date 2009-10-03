#!/bin/bash

# Needs to have SRILM in PATH

uniform-classes classdefs > uniform-classdefs
replace-words-with-classes classes=uniform-classdefs sample.transcripts > sample-with-classes.transcripts
ngram-count -text sample.transcripts -lm sample.trigram.lm
ngram-count -text sample-with-classes.transcripts -lm sample-with-classes.trigram.lm
