#!/usr/bin/env bash

mvn package -DskipTests=true

java -jar target/my-first-app-1.0-SNAPSHOT-fat.jar -conf src/main/resources/my-it-conf.json
