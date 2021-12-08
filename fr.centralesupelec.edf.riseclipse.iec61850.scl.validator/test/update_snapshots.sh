#!/bin/bash

VALIDATOR_VERSION=$(grep -oPm1 '(?<=version>)[^<]*' ../pom.xml)
JAR_NAME=RiseClipseValidatorSCL-${VALIDATOR_VERSION}.jar
JAR_PATH=../target/$JAR_NAME

UPDATED_SNAPSHOTS_COUNT=0
UPDATED_SNAPSHOTS=""

echo -n "Updating snapshots... "

for filepath in input/*; do
    OUTPUT=$(java -jar $JAR_PATH $filepath)
    SNAPSHOT_FILEPATH=snapshots/$(basename $filepath)
    SNAPSHOT_FILEPATH=${SNAPSHOT_FILEPATH%\.xml}.out
    printf "$OUTPUT" > $SNAPSHOT_FILEPATH
    UPDATED_SNAPSHOTS_COUNT=$((UPDATED_SNAPSHOTS_COUNT + 1))
    UPDATED_SNAPSHOTS="$UPDATED_SNAPSHOTS> $SNAPSHOT_FILEPATH\n"
done

echo "Done !"
printf "\n=== Updated snapshots: $UPDATED_SNAPSHOTS_COUNT ===\n"
printf "$UPDATED_SNAPSHOTS"