#!/usr/bin/env bash 
# if you want to publish a release, make sure
# the version number in the project is a whole
# integer, not a `-SNAPSHOT` build. otherwise
# the build will be deployed to the staging repo
echo "this script will prompt you for the GPG passphrase"
export GPG_TTY=$(tty)
mvn -DskipTests=true -P publish clean deploy
