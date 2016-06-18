#!/bin/sh

version=`cat build.gradle | grep "^version " | sed -r "s/.*version = '(.*)'$/\1/"`

echo "Building container v$version"
docker build -t alfred .

echo "Tagging version $version"
docker tag alfred 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:$version
docker tag alfred 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:latest
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:$version
docker push 274685854631.dkr.ecr.us-east-1.amazonaws.com/alfred:latest
