#!/usr/bin/env bash 
echo "this script will prompt you for the GPG passphrase"
export GPG_TTY=$(tty)
mvn -DskipTests=true clean deploy 