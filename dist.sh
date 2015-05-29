#!/bin/bash

sbt dist

ssh sre@cheminot.org 'rm /home/sre/sites/cheminot.org/app/cheminotorg-1.0-SNAPSHOT.zip'
scp target/universal/cheminotorg-1.0-SNAPSHOT.zip sre@cheminot.org:/home/sre/sites/cheminot.org/app/
