#!/bin/sh

docker build -f Dockerfile.build -t platform/build .
docker run --name=builder platform/build
docker cp builder:/app/target .
docker rm builder
docker build -f Dockerfile.run -t alfred .
rm -rf target
