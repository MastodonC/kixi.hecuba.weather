#!/bin/bash

java -jar kixi.hecuba.weather-0.2.0-SNAPSHOT-standalone.jar -u${KHWUSER?NOT DEFINED} -p${KHWPASS?NOT DEFINED} -c${KHWDEVICESFILE?NOT DEFINED}
 
