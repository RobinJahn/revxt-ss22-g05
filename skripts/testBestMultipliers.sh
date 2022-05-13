#!/bin/bash

helpMsg() {
  echo "Parameters for $0"
  echo "-d <depth> sets the depth"
  echo "-t <time> sets the time"
  echo "-p enables print of game"
  echo "-e endables extended print"
  exit 1
  }

#default values
depth=4
time=0
print=false
extendedPrint=false

#read in parameters
while getopts "d:t:peh" opt
do
   case "$opt" in
      d ) depth="$OPTARG" ;;
      t ) time="$OPTARG" ;;
      p ) print=true ;;
      e ) extendedPrint=true ;;
      h ) helpMsg ;;
      ? ) helpMsg ;;
   esac
done

clear

echo "depth: $depth"
echo "time: $time"
echo "print on: $print"
echo "extended print on: $extendedPrint"

i=1
max=100 #how many games should be played

outFile="serverOut.txt"

bestResult=0 #0 is worst placement

bestM1=1
bestM2=1
bestM3=1
bestM4=1

#get all Maps	
cd ..
cd Maps
maps=($(ls | grep ".*.map"))
echo "maps: "
echo "${maps[@]}"

#compile newest version of client
cd ..
ant -S jar

#change directory to the one where the server and ai is
cd serverAndAi

while [ $i -le $max ]
do
	#set m's to random variables
	m1=$(($RANDOM % 10))
	m2=$(($RANDOM % 10))
	m3=$(($RANDOM % 10))
	m4=$(($RANDOM % 10))

	echo "script: Set m's to:"
	echo "m1: $m1"
       	echo "m2: $m2"
      	echo "m3: $m3" 
      	echo "m4: $m4"
	
	#start games on different maps
	result=0
	for mapName in "${maps[@]}"
	do
		if $extendedPrint; then echo ""; fi
		echo "script: now Playing on: $mapName"
		

		# start own client
		if $extendedPrint; then echo "script: start client in 3 sec"; fi
		sleep 3 && 
			echo "script: started client" &&
			java -jar ../bin/client05.jar -i 127.0.0.1 -p 7777 -m $m1 $m2 $m3 $m4 -ab -c &> "clientOut.txt" &
		pid1=$!


		#get countOfPlayer
		countOfPlayer=$(awk '(NR==1){printf("%d",$1)}' "../Maps/$mapName")
		if $extendedPrint; then echo "script: count of players: $countOfPlayer"; fi
	
		#start trivial ai's
		if $extendedPrint; then echo "script: start trivial AIs in 3 sec"; fi
		ii=1
		pidAIs=()
		while [ $ii -lt $countOfPlayer ]
		do
			if $extendedPrint; then echo "script: start ai $ii"; fi
			sleep 3 && 
				echo "script: started ai" &&
				./ai_trivial -q &
			pidAIs+=(ii)
			ii=$((ii+1))
		done

		#start server
		if $extendedPrint; then echo "script: server started"; fi

		if [ $time -eq 0 ]
    	then
    		if [ $depth -eq 0 ]; then
    		  if $print; then
    		    echo "script: with output"
    			  ./server_nogl -C -m ../Maps/$mapName | tee $outFile #with output of server
    			else
    			  echo "script: without output"
            ./server_nogl -C -m ../Maps/$mapName &> $outFile #without
          fi
    		else
    		  if $print; then
    		    echo "script: with output -d"
    			  ./server_nogl -C -m ../Maps/$mapName -d $depth | tee $outFile #with output of server
    			else
    			  echo "script: without output -d"
            ./server_nogl -C -m ../Maps/$mapName -d $depth &> $outFile #without
          fi
    		fi
    	else
    		if [ $depth -eq 0 ]; then
    		  if $print; then
    		    echo "script: with output -t"
    			  ./server_nogl -C -m ../Maps/$mapName -t $time | tee $outFile #with output of server
          else
            echo "script: without output -t"
            ./server_nogl -C -m ../Maps/$mapName -t $time &> $outFile #without
          fi
    		else
    		  if $print; then
    		    echo "script: with output -d -t"
    		    ./server_nogl -C -m ../Maps/$mapName -t $time -d $depth | tee $outFile #with output of server
    		  else
    		    echo "script: without output -d -t"
            ./server_nogl -C -m ../Maps/$mapName -t $time -d $depth &> $outFile #without
          fi
    		fi
    	fi
		
	
		#when game is over
		if $extendedPrint; then echo "skript: game is over"; fi
		
		#wait for client
		wait $pid1
		if $extendedPrint; then echo "skript: client ended"; fi
	
		#wait for ai's
		for pid2 in "${pidAIs[@]}"
		do
			wait $pid
		done
		if $extendedPrint; then echo "skript: all AIs ended"; fi
		
		#get result of game
		resultOfGame=$(awk -v groupID=5 -f ../skripts/getOwnResults.awk $outFile)
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
	isOne=$( echo "$result >= 0.99" | bc -l )
	if [ ${isOne} -eq 1 ]
	then
		exit
	fi

	#check if it's a new best
	res=$( echo "$result > $bestResult" | bc -l ) #maybe line splitting of bc is the problem
	echo "skript: comparison: $result > $bestResult = $res"
	if [[ ${res} -eq 1 ]]
	then
	  echo "skript: set best values to current values"
		#if it is set the best values to the current ones
		bestResult=$result
		bestM1=$m1
		bestM2=$m2
		bestM3=$m3
		bestM4=$m4
	fi

	echo "skript: Currently best results:"
	echo "skript: result: $bestResult"
	echo "skript: best m1: $bestM1"
	echo "skript: best m2: $bestM2"
	echo "skript: best m3: $bestM3"
	echo "skript: best m4: $bestM4"
	echo ""

	i=$((i+1))
	
	#read -p "Press enter to start next game"
	#clear
done


