#!/bin/bash

CONTAINER_ID=$(docker ps | grep alfred | awk '{print $1}')

if [ -z "$CONTAINER_ID" ]
then
  echo "Unable to find container for alfred. Containers are:"
  docker ps
  exit 1
fi

docker exec -i -t $CONTAINER_ID bash
