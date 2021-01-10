#!/bin/bash
export RAPPELLEDEV_VERSION=0.6.0

function run() { echo -e "\n==>> Running: [$@]" ; "$@" ; }

pushd ${HOME}
run curl -fLo ./rappelledev.tar.gz "https://github.com/vitorqb/rappelledev/archive/${RAPPELLEDEV_VERSION}.tar.gz"
run tar -vzxf ./rappelledev.tar.gz
run rm -rfv ./rappelledev.tar.gz
run mv -v ./rappelledev-${RAPPELLEDEV_VERSION} ./rappelledev
run chmod +x ./rappelledev/rappelledev.py
popd

run ls ${HOME}/rappelledev
run mkdir -p ~/.config/rappelledev
run cp -v ./.circleci/rappelledev/config.json ~/.config/rappelledev/config.json
run cp -v ./.circleci/rappelledev/docker-compose.override.yaml ~/.config/rappelledev/docker-compose.override.yaml
