#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "Не передано имя Игрока"
    exit 1
fi

java -classpath /home/devslair/prjs/ipc/signal/target/classes devs.lair.ipc.signal.PlayerStarter $1