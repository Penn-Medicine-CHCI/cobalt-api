#!/bin/sh
java -Xms128m -Xmx1024m -cp classes:dependency/* -Dpolyglot.engine.userResourceCache=graalvm-cache --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED com.cobaltplatform.api.App
