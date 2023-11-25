#!/bin/bash

BASE_DIR=$(realpath $(dirname $0))
ROOT_DIR=$(realpath $BASE_DIR/..) 

rm -rf $BASE_DIR/build/tai-e-all-0.5.1-SNAPSHOT.jar
./gradlew fatjar
cp $BASE_DIR/build/tai-e-all-0.5.1-SNAPSHOT.jar $ROOT_DIR/judge/user/submission_jar/
mv $ROOT_DIR/judge/user/submission_jar/tai-e-all-0.5.1-SNAPSHOT.jar \
    $ROOT_DIR/judge/user/submission_jar/submission.jar

cd $ROOT_DIR/judge

rm $ROOT_DIR/judge/user/submission_jar/result.json
bash $ROOT_DIR/judge/init.sh
bash $ROOT_DIR/judge/run_jar.sh