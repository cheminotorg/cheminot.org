#!/bin/bash

gulp
sbt dist
scp -r /Volumes/data/Projects/me/cheminot.org/target/universal/cheminotorg-1.0-SNAPSHOT.zip  sre@cheminot.org:sites/cheminot.org/app/
