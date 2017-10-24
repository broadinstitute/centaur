#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "${SCRIPT_DIR}/../../.." && pwd )"
JAR_DIR="$( cd "${ROOT_DIR}/target/scala-2.12" && pwd )"

java -jar "${JAR_DIR}/centaur-cwl-runner-1.0.jar" $@
