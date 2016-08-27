#!/bin/sh

version=`cat build.gradle | grep "^version " | sed -r "s/.*version = '(.*)'$/\1/"`

echo "Building container v$version$1"
docker build -t alfred .

echo "Tagging version $version$1"
docker tag alfred 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:$version$1
docker tag alfred 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:latest$1
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:$version$1
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:latest$1
