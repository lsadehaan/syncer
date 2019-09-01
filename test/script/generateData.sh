#!/usr/bin/env bash
set -e

source ${UTIL_LIB}

lines=$1
env=$2
start=$3
reGenerate=$4

if [[ -z ${reGenerate} ]]; then
    reGenerate=true
fi

if [[ -z ${start} ]]; then
    start=0
fi

function generateMysqlTestData() {
    logi "---------------------"
    logi "generateMysqlTestData"
    logi "---------------------"

    start=$2
    for (( i = 0; i < ${MYSQL_INSTANCE}; ++i )); do
        for f in generator/*.sql; do
            filename=`basename ${f}`
            dir=${filename%".sql"}
            mkdir -p data/mysql/${i}/csv/${dir}
            exists=`find data/mysql/${i}/csv/${dir} -name '*.csv'`
        done
        if [[ -z "$exists" || ${reGenerate} = true ]]; then

            for f in generator/*.sql; do
                name=`basename ${f}`
                docker run -v "$(pwd)"/data:/data --rm generator:test /data/mysql/${i} /${name} $1 ${start} 4  >> "${LOG_FILE}"
            done
        fi
        start=$(($start + $1))
    done

    cd ${TEST_DIR}
}

function generateMongoTestData() {
    logi "---------------------"
    logi "generateMongoTestData"
    logi "---------------------"

    mkdir -p ${TEST_DIR}/data/mongo/
    cd ${TEST_DIR}/../syncer-core/
    mvn test -q -Dtest=com.github.zzt93.syncer.common.data.MongoGenerator -DargLine="-Dnum=$1 -DfileName=${TEST_DIR}/data/mongo/simple_type.json" >> "${LOG_FILE}"
    cd ${TEST_DIR}
}

if [[ ${env} = "mongo" ]]; then
    generateMongoTestData ${lines}
else
    generateMysqlTestData ${lines} ${start}
fi
