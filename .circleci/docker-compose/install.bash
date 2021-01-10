#!/bin/bash

DEST=${HOME}/.local/bin/docker-compose

function run() { echo -e "\n==>> Running: [$@]" ; "$@" ; }

run mkdir -p $(dirname $DEST)
run curl -L "https://github.com/docker/compose/releases/download/1.27.4/docker-compose-$(uname -s)-$(uname -m)" -o $DEST
run chmod +x $DEST
