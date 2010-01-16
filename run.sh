#!/bin/bash

$JAVA_HOME/bin/java -classpath build/src/:lib/*:lib/seleniumdeps/* com.bitclean.billscrape.BillDownload $1 
