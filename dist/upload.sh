#!/bin/bash

chmod a+x ./dist/dropbox_uploader.sh

if [ -n "${TRAVIS_TAG}" ]; then
    ./dist/dropbox_uploader.sh -f ./dropbox_uploader -p upload target/*-one-jar.jar cheminotorg-${TRAVIS_TAG}.jar
else
    ./dist/dropbox_uploader.sh -f ./dropbox_uploader -p upload target/*-one-jar.jar cheminotorg-latest.jar
fi
