#!/bin/sh

java -cp ../../classes tests.frontend.CmnFeatureExtractorTest cepstra.s3 > features.out

diff features.s3 features.out | wc -l