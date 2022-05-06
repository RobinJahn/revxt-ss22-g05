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
		sleep 3 && 
			echo "skript: startet ai" && 
			./ai_trivial -q &
		ii=$((ii+1))
	done

	#start server
	./server_nogl -m $mapName -d $depth -t $time
done
