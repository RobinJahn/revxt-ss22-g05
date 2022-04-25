#!/bin/bash
clear

i=1
max=100 #how many games should be played

outFile="serverOut.txt"

bestResult=0 #worst placement

bestM1=1
bestM2=1
bestM3=1

extendedPrint=false

while [ $i -le $max ]
do
	#set m's to random varaibles
	m1=$(($RANDOM % 10))
	m2=$(($RANDOM % 10))
	m3=$(($RANDOM % 10))

	echo "skript: Set m's to:"
	echo "skript: m1: $m1"
	echo "skript: m2: $m2"
	echo "skript: m3: $m3"

	#get all Maps
	maps=($(ls | grep "Map."))
	
	#start games on different maps
	result=0
	for mapName in "${maps[@]}"
	do
		if $extendedPrint; then echo ""; fi
		echo "skript: now Playing on: $mapName"
		

		# start own client
		if $extendedPrint; then echo "script: start client in 3 sec"; fi
		sleep 3 && java -jar client05.jar -i 127.0.0.1 -p 7777 -m $m1 $m2 $m3 &
		pid1=$!


		#get anzPlayer 
		anzPlayer=$(awk '(NR==1){printf("%d",$1)}' "$mapName")
		if $extendedPrint; then echo "script: anzahl der Player: $anzPlayer"; fi
	
		#start trivial ai's
		if $extendedPrint; then echo "skript: start trivial AIs"; fi
		ii=1
		pidAIs=()
		while [ $ii -lt $anzPlayer ]
		do
			if $extendedPrint; then echo "skript: start ai $ii"; fi
			sleep 3 && ./ai_trivial -q &
			pidAIs+=(ii)
			ii=$((ii+1))
		done
	
		
		#start server
		if $extendedPrint; then echo "script: server started"; fi
		#./server_nogl -C -m $mapName | tee $outFile #with output of server
		./server_nogl -C -m $mapName > $outFile #without
		
	
	
		#when game is over
		if $extendedPrint; then echo "skript: game is over"; fi
		
		#wait for client
		wait $pid1
		if $extendedPrint; then echo "skript: client ended"; fi
	
		#wait for clients
		for pid2 in "${pidAIs[@]}"
		do
			wait $pid
		done
		if $extendedPrint; then echo "skript: all AIs ended"; fi
		
		#get result of game
		resultOfGame=$(awk -f getOwnResults.awk $outFile)
		resultOfGame=$( echo "$resultOfGame" | tr ',' '.')
		echo "skript: result: $resultOfGame"
		
		result=$(echo "$result + $resultOfGame" | bc -l )

		#sleep 4

	done #done playing games on all maps

	echo ""

	anzMaps=${#maps[@]}
	
	echo "skript: sum of results: $result and sum of games played: $anzMaps"
	
	result=$( echo "$result / $anzMaps" | bc -l )
	echo "skript: average result of games: $result"

	#check if it's a new best
	if [ $( echo "$result > $bestResult" | bc -l ) ]
	then 
		#if it is set the best values to the current ones
		bestResult=$result
		bestM1=$m1
		bestM2=$m2
		bestM3=$m3
	fi

	echo "skript: Currently best results:"
	echo "skript: result: $bestResult"
	echo "skript: m1: $bestM1"
	echo "skript: m2: $bestM2"
	echo "skript: m3: $bestM3"
	echo ""

	i=$((i+1))
	
	#read -p "Press enter to start next game"
	#clear
done


