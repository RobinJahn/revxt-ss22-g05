#!/bin/bash

cd ..
cd serverAndAi

mapName=${1?"argument 1 needs to be map name"}

depth=${2?"argument 2 needs to be the depth"}

time=${3?"argument 3 needs to be the time"}

if [[ ! -f "$mapName" ]]; then
    echo "$mapName doesn't exists."
	exit
fi


while true
do 
	#start ai's

	#	get anzPlayer 
	anzPlayer=$(awk '(NR==1){printf("%d",$1)}' "$mapName")
	if $extendedPrint; then echo "script: anzahl der Player: $anzPlayer"; fi

	#	start trivial ai's
	if $extendedPrint; then echo "skript: start trivial AIs in 3 sec"; fi
	ii=1
	while [ $ii -lt $anzPlayer ]
	do
		if $extendedPrint; then echo "skript: start ai $ii"; fi
		sleep 10 &&
			echo "skript: startet ai" && 
			./ai_trivial -q &
		ii=$((ii+1))
	done

	#start server
	if (( $(echo "$time==0" | bc -l) ))
	then
		if [ $depth -eq 0 ]; then
			echo "./server_nogl -m $mapName"
			./server_nogl -m $mapName # --moves "replay.txt"
		else
			echo "./server_nogl -m $mapName -d $depth"
			./server_nogl -m $mapName -d $depth # --moves "replay.txt"
		fi
	else
		if [ $depth -eq 0 ]; then
			echo "./server_nogl -m $mapName -t $time"
			./server_nogl -m $mapName -t $time # --moves "replay.txt"

		else
      echo "./server_nogl -m $mapName -d $depth -t $time"
      ./server_nogl -m $mapName -d $depth -t $time # --moves "replay.txt"
		fi
	fi

done
