#!/bin/sh

#
# Copyright 1999-2002 Carnegie Mellon University.  
# Portions Copyright 2002 Sun Microsystems, Inc.  
# Portions Copyright 2002 Mitsubishi ElectricResearch Laboratories.
# All Rights Reserved.  Use is subject to license terms.
# 
# See the file "license.terms" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL 
# WARRANTIES.
#

#
# This scripts compare the output of an language model (LM_SCORES_FILE)
# file with the correct output, and the output of backoff probability
# calculations (BACKOFF_FILE) with the correct output.
#

if [ -f lm.scores.diff ]; then
  rm lm.scores.diff
fi

if [ -f backoff.diff ]; then
  rm backoff.diff
fi

LM_SCORES_FILE=$1
CORRECT_LM_SCORES_FILE=$2
BACKOFF_FILE=$3
CORRECT_BACK_FILE=$4

diff $LM_SCORES_FILE $CORRECT_LM_SCORES_FILE > lm.scores.diff
diff $BACKOFF_FILE $CORRECT_BACKOFF_FILE > backoff.diff

chmod a+x ./outputDiff.sh
./outputDiff.sh lm.scores.diff
./outputDiff.sh backoff.diff
