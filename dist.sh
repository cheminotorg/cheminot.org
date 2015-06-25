#!/bin/bash

gulp --mode=prod
sbt dist
dropbox_uploader.sh -p upload target/universal/cheminotorg-1.0-SNAPSHOT.zip cheminotorg-latest.zip
dropbox_uploader.sh share cheminotorg-latest.zip
rm version
git describe --long --always > version
dropbox_uploader.sh -p upload version cheminotorg-version
