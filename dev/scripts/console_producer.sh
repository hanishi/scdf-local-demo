#!/usr/bin/env bash

source $(dirname $0)/common.sh

if [ $# -gt 0 ]; then
   $kafka_home/bin/kafka-console-producer.sh --topic $1 --broker-list $broker
else
   echo "Usage: "$(basename $0)" <topic>"
fi
