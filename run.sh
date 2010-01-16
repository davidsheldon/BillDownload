#!/bin/bash
# Optionally pass in the name of a config file on the command line.
if [ -e "$JAVA_HOME/bin/java" ] ; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi
$JAVA -classpath build/src/:lib/*:lib/seleniumdeps/* com.bitclean.billscrape.BillDownload $1 
