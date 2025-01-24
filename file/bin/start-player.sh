#!/bin/bash

if [ $# -eq 0 ]
  then
    echo "Не передано имя Игрока"
    exit 1
fi

java -cp /home/devslair/prjs/ipc/file/target/classes devs.lair.ipc.file.Player "$1"