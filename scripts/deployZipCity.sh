#!/bin/sh
# Define the destination
PUSH_DEST=/home/groups/c/cm/cmusphinx/htdocs/sphinx4/src/apps/edu/cmu/sphinx/demo/
export PUSH_DEST
# Get the zipcity auxiliary files
pushd src/apps/edu/cmu/sphinx/demo/zipcity
bash -x srsync $PUSH_DEST/zipcity *
popd
pushd bin
# Get the zipcity code
bash -x srsync $PUSH_DEST/zipcity ZipCity.jar
popd
pushd lib
# Get some sphinx4, models
bash -x srsync $PUSH_DEST/zipcity sphinx4.jar jsapi.jar TIDIGITS_8gau_13dCep_16k_40mel_130Hz_6800Hz.jar
popd

