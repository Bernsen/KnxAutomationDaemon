#!/bin/sh
BASEDIR=/opt/kad
/usr/bin/java -Dkad.basedir=$BASEDIR -cp $BASEDIR/bin/*:$BASEDIR/plugins/* de.root1.kad.logicplugin.ScriptCheck $1 $2 $3
