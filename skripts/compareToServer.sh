#!/bin/bash

mapName=${1?"argument 1 needs to be map name"}

depth=${2?"argument 2 needs to be the depth"}

time=${3?"argument 3 needs to be the time"}

if [[ ! -f "$mapName" ]]; then
    echo "$mapName doesn't exists."
	exit
fi

echo "time: $time"
echo "depth: $depth"

#	get anzPlayer
anzPlayer=$(awk '(NR==1){printf("%d",$1)}' "$mapName")

cd ..
cd serverAndAi

#	start trivial ai's
ii=1
while [ $ii -lt $anzPlayer ]
do
  sleep 3 &&
    echo "skript: startet ai" &&
    ./ai_trivial -q &
  ii=$((ii+1))
done

#start server
if (( $(echo "$time==0" | bc -l) ))
then
  if [ $depth -eq 0 ]; then
    echo "./server_nogl -m $mapName"
    ./server_nogl -C -m $mapName | tee Server_View.txt
  else
    echo "./server_nogl -m $mapName -d $depth"
    ./server_nogl -C -m $mapName -d $depth | tee Server_View.txt
  fi
else
  if [ $depth -eq 0 ]; then
    echo "./server_nogl -m $mapName -t $time"
    ./server_nogl -C -m $mapName -t $time | tee Server_View.txt
  else
    echo "./server_nogl -m $mapName -d $depth -t $time"
    ./server_nogl -C -m $mapName -d $depth -t $time | tee Server_View.txt
  fi
fi
