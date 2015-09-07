# kixi.hecuba.weather

A microservice for kixi.hecuba to upload weather observation data. 


## Building The Project
To manage dependecies we suggest using the Lein build too for Clojure projects. 

#### Changing the Endpoint

If you are running your own version kixi.hecuba then you will need to change the url in src/kixi/hecuba/weather/met_office_api.clj


To build run the following lein command:

	lein uberjar

from the command line. This will create a jar will all the required dependencies in the target folder. 

## Execution

In the script directory is a ```run-metoffice-daily.sh``` shell script. 

#### Setting the Variables
Before you run the script ensure you've set the correct variables either in your .profile file or exported them from the command line.

	export khwuser=[your username]
	export khwpass=[your password]
	export khwdevicesfile=[path to csv file with entity, device, sensor and synthetic sensor ids] 

#### Running the Script

When these are set you can run the script from the command:

	./run-metoffice-daily.sh
	
## License

Copyright © 2015 Mastodon C Ltd

Distributed under the Eclipse Public License version 1.0.
