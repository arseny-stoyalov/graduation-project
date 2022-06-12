#!/bin/bash

JAR=/usr/share/gp/gp.jar
CONF_FILE=/etc/gp/application.conf
LOGCONF_FILE=/etc/gp/logback.xml
JAVA_HOME="/usr"

${JAVA_HOME}/bin/java \
        -XX:MaxRAMPercentage=60 \
        -XX:+UseG1GC \
        -XX:+UseStringDeduplication \
        -XX:ParallelGCThreads=${PAR_GC_THREADS:-10} \
        -Dfile.encoding=UTF-8 -XX:-OmitStackTraceInFastThrow \
        -jar \
        -Dconfig.file=${CONF_FILE} \
        -Dlogback.configurationFile=${LOGCONF_FILE} \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=${HEAPDUMP_PATH:-/var/lib/gp} \
        ${JAR}
