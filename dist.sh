#!/bin/bash

sbt dist

dropbox_uploader.sh -p upload target/universal/cheminotorg-1.0-SNAPSHOT.zip cheminotorg-latest.zip
dropbox_uploader.sh share cheminotorg-latest.zip
