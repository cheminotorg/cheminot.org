#!/bin/bash

function setjdk() {
  if [ $# -ne 0 ]; then
   removeFromPath '/System/Library/Frameworks/JavaVM.framework/Home/bin'
   if [ -n "${JAVA_HOME+x}" ]; then
    removeFromPath $JAVA_HOME
   fi
   export JAVA_HOME=`/usr/libexec/java_home -v $@`
   export PATH=$JAVA_HOME/bin:$PATH
  fi
  echo JAVA_HOME set to $JAVA_HOME
  java -version
}

function removeFromPath() {
  export PATH=$(echo $PATH | sed -E -e "s;:$1;;" -e "s;$1:?;;")
}

setjdk 1.7

VERSION=$(git describe --long --always)

gulp --mode=prod
sbt dist
dropbox_uploader.sh -p upload target/universal/cheminotorg-$VERSION.zip cheminotorg-latest.zip
dropbox_uploader.sh share cheminotorg-latest.zip
rm version
echo $VERSION > version
dropbox_uploader.sh -p upload version cheminotorg-version

setjdk 1.8
