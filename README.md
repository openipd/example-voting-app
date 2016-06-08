This is a fork of the docker compose application and deconstructed to deploy microservices on Cloud Foundry. 


Example Voting App
==================

This is an example Docker app with multiple services. It is run with Docker Compose and uses Docker Networking to connect containers together. You will need Docker Compose 1.6 or later.

More info at https://blog.docker.com/2015/11/docker-toolbox-compose/

Architecture
-----

* A Python webapp which lets you vote between two options
* A Redis queue which collects new votes provided by Cloud Foundry service broker
* A Java worker which consumes votes and stores them in…
* A Postgres database backed by a Cloud Foundry service broker
* A Node.js webapp which shows the results of the voting in real time

Running
-------

* create redis service named voterappRedis (via apps console or commandline "cf cs rediscloud 30mb voterappRedis")
* create postgresql service named workerPostgresSQL (via apps console or command line "cf cs elephantsql turtle workerPostgresSQL") 
* go to folder voting-app/ and execute a "cf push" in that folder
* go to folder worker/ and execute a "mvn clean", "mvn package" and "cf push" in that folder
* go to folder result-app/ and execute a "cf push" in that folder

