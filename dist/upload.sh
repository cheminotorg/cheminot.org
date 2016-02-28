#!/bin/bash

chmod a+x ./dist/dropbox_uploader.sh

./dist/dropbox_uploader.sh -f ./dropbox_uploader -p upload target/scala-2.11/web_2.11-0.1.0-one-jar.jar cheminotorg.jar
