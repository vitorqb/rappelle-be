#!/bin/bash
~/project/dev/rappelle-be-tools postgres &
~/project/dev/rappelle-be-tools wait_for_postgres
~/project/dev/rappelle-be-tools test --functional

