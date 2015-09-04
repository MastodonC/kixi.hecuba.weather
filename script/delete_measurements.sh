#!/bin/bash

DATA="/home/eleonore/Documents/weather-live-data.csv"

## For each row of the file, build the url for the curl command
for row in $(cat $DATA);
do echo "http://getembed.com/4/entities/${row#*,}/devices/${row#,*}/measurements/"
done;

			     
