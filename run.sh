#!/bin/bash
# Optionally pass in the name of a config file on the command line.
$JAVA_HOME/bin/java -classpath build/src/:lib/*:lib/seleniumdeps/* com.bitclean.billscrape.BillDownload $1 
