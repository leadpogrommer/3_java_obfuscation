#!/bin/bash

set -e

cd s9_client && ./gradlew proguard && cd ..
cd s9_server && ./gradlew proguard && cd ..
cd s9_loader && ./gradlew proguard && cd ..

python3 s9_loader/insert_payload.py s9_loader/build/libs/app_pg.jar s9_server/build/libs/server_pg.jar server.jar
python3 s9_loader/insert_payload.py s9_loader/build/libs/app_pg.jar s9_client/build/libs/client_pg.jar client.jar