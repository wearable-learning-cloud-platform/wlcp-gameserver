#!/bin/bash

if [ "$SPRING_PROFILES_ACTIVE" = "ecs" ]
then
export ECS_INSTANCE_IP_ADDRESS=$(curl http://169.254.170.2/v2/metadata | jq '.Containers[0].Networks[0].IPv4Addresses[0]' | tr -d '"')
echo ${ECS_INSTANCE_IP_ADDRESS}
fi

/usr/bin/java -jar app.jar