#!/bin/bash

curl -H "Content-Type: application/json" -X POST -d '{"password":"cheminotorg"}' -u neo4j:neo4j http://localhost:7474/user/neo4j/password
