#!/bin/bash

DATA="/path/to/live-device-sensors.csv"

## For each row of the file, build the url for the curl command
for row in ${DATA};
do echo "http://getembed.com/4/entities/${row#*,}/devices/${row#,*}/measurements/"
done;

			     
