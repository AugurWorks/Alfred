#!/bin/sh

docker build -f Dockerfile.build -t alfred/build .
docker run --name=builder alfred/build
docker cp builder:/app/target .
docker rm builder
docker build -f Dockerfile.run -t alfred .
rm -rf target
