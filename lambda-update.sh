#!/bin/bash

gradle shadowJar
mv build/libs/alfred-*.jar build/libs/alfred.jar
aws lambda update-function-code --function-name arn:aws:lambda:us-east-1:274685854631:function:Alfred-Lambda --zip-file fileb://build/libs/alfred.jar
