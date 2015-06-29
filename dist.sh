#!/bin/bash

VERSION=$(git describe --long --always)

gulp --mode=prod
sbt dist
dropbox_uploader.sh -p upload target/universal/cheminotorg-$VERSION.zip cheminotorg-latest.zip
dropbox_uploader.sh share cheminotorg-latest.zip
rm version
echo $VERSION > version
dropbox_uploader.sh -p upload version cheminotorg-version
