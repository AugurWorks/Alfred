#!/bin/bash

# Fail fast
set -e

# Remove existing alfred container
docker rm -f alfred 

# Load up new container
docker run -d --name=alfred -p 8080:8080 alfred
