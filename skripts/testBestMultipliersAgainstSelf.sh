#!/bin/bash

helpMsg() {
  echo "Parameters for $0"
  echo "-d <depth> sets the depth"
  echo "-t <time> sets the time"
  echo "-p enables print of game"
  echo "-e enables extended print"
  exit 1
}

#default values
depth=0
time=1
print=false
extendedPrint=false
map=""

#read in parameters
while getopts "d:t:m:peh" opt
do
   case "$opt" in
      d ) depth="$OPTARG" ;;
      t ) time="$OPTARG" ;;
      p ) print=true ;;
      e ) extendedPrint=true ;;
      m ) map="$OPTARG" ;;
      h ) helpMsg ;;
      ? ) helpMsg ;;
   esac
done

clear

echo "depth: $depth"
echo "time: $time"
echo "print on: $print"
echo "extended print on: $extendedPrint"

sleepingTime=4
i=1
max=100 #how many games should be played

outFileServer="serverOutAgainstSelf.txt"
outFileClient="clientOutAgainstSelf.txt"


bestResult="-1" # 0 is worst placement

bestM1=1
bestM2=1
bestM3=1
bestM4=1



#get all Maps
maps=($(ls "../Maps/" | grep "\.map"))


#print maps
echo "maps: "
printf '%s\n' "${maps[@]}"
echo ""

#compile newest version of client
(cd ..; ant -S jar)

#play sets of games
while [ $i -le $max ]
do

  multipliersP1=()
  multipliersP2=()
  multipliersP3=()
  multipliersP4=()
  multipliersP5=()
  multipliersP6=()
  multipliersP7=()
  multipliersP8=()

  for ((i=0; i<15; i++)); do
    multipliersP1+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP2+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP3+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP4+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP5+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP6+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP7+=($(($RANDOM % 10)))
  done
  for ((i=0; i<15; i++)); do
    multipliersP8+=($(($RANDOM % 10)))
  done

  echo "multipliers for players"
  echo "multipliersP1"
  echo "${multipliersP1[@]}"
  echo "multipliersP2"
  echo "${multipliersP2[@]}"
  echo "multipliersP3"
  echo "${multipliersP3[@]}"
  echo "multipliersP4"
  echo "${multipliersP4[@]}"
  echo "multipliersP5"
  echo "${multipliersP5[@]}"
  echo "multipliersP6"
  echo "${multipliersP6[@]}"
  echo "multipliersP7"
  echo "${multipliersP7[@]}"
  echo "multipliersP8"
  echo "${multipliersP8[@]}"


	#start games on different maps
	result=0
	for mapName in "${maps[@]}"
	do
		echo "script: now Playing on: $mapName"

		#get countOfPlayer
    countOfPlayer=$(awk '(NR==1){printf("%d",$1)}' "../Maps/$mapName")
    if $extendedPrint; then echo "script: count of players: $countOfPlayer"; fi

    #check if amount of players is valid (valid are 2, 4 and 8)
    if [ $countOfPlayer -ne 2 ] || [ $countOfPlayer -ne 2 ] || [ $countOfPlayer -ne 2 ]; then
      echo "invalid amount of players"
      exit
    fi

		#clients
		if $extendedPrint; then echo "script: start client in 3 sec"; fi
		sleep $sleepingTime &&
			echo "script: started client" &&
			java -jar ../bin/client05.jar -i 127.0.0.1 -p 7777 -m 1 "${multipliersP1[0]}" "${multipliersP1[1]}" "${multipliersP1[2]}" "${multipliersP1[3]}" "${multipliersP1[4]}" -m 2 "${multipliersP1[5]}" "${multipliersP1[6]}" "${multipliersP1[7]}" "${multipliersP1[8]}" "${multipliersP1[8]}" -m 3 "${multipliersP1[9]}" "${multipliersP1[10]}" "${multipliersP1[11]}" "${multipliersP1[12]}" "${multipliersP1[13]}" -c > "$outFileClient" &
		pid1=$!



	
		#start trivial ai's
		if $extendedPrint; then echo "script: start trivial AIs in 3 sec"; fi
		ii=1
		pidAIs=()
		while [ $ii -lt $countOfPlayer ]
		do
			if $extendedPrint; then echo "script: start ai $ii"; fi
			sleep $sleepingTime &&
				echo "script: started ai" &&
				../serverAndAi/ai_trivial -q &> /dev/null &
			pidAIs+=(ii)
			ii=$((ii+1))
		done

		#start server
		echo "script: server started"

		if (( $(echo "$time==0" | bc -l) )); then
      if [ $depth -eq 0 ]; then
        if $print; then
          echo "script: with output"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName | tee "../skripts/$outFileServer") #with output of server
        else
          echo "script: without output"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName > "../skripts/$outFileServer") #without
        fi
      else
        if $print; then
          echo "script: with output -d"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -d $depth | tee "../skripts/$outFileServer") #with output of server
        else
          echo "script: without output -d"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -d $depth > "../skripts/$outFileServer") #without
        fi
      fi
    else
      if [ $depth -eq 0 ]; then
        if $print; then
          echo "script: with output -t"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -t $time | tee "../skripts/$outFileServer") #with output of server
        else
          echo "script: without output -t"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -t $time > "../skripts/$outFileServer") #without
        fi
      else
        if $print; then
          echo "script: with output -d -t"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -t $time -d $depth | tee "../skripts/$outFileServer") #with output of server
        else
          echo "script: without output -d -t"
          (cd ../serverAndAi/; ./server_nogl -C -m ../Maps/$mapName -t $time -d $depth > "../skripts/$outFileServer") #without
        fi
      fi
    fi
		
	
		#when game is over
		if $extendedPrint; then echo "script: game is over"; fi
		
		#wait for client
		wait $pid1
		if $extendedPrint; then echo "script: client ended"; fi
	
		#wait for ai's
		for pid2 in "${pidAIs[@]}"
		do
			wait $pid
		done
		if $extendedPrint; then echo "script: all AIs ended"; fi
		
		#get result of game
		resultOfGame=$(awk -v groupID=5 -f ./getOwnResults.awk $outFileServer)
		resultOfGame=$( echo "$resultOfGame" | tr ',' '.')
		echo "script: result: $resultOfGame"
		
		result=$(echo "$result + $resultOfGame" | bc -l )

		#sleep 4

	done #done playing games on all maps

	echo ""

	anzMaps=${#maps[@]}
	
	echo "script: sum of results: $result and sum of games played: $anzMaps"
	
	result=$( echo "$result / $anzMaps" | bc -l )
	echo "script: average result of games: $result"
	isOne=$( echo "$result >= 0.99" | bc -l )
	if [ ${isOne} -eq 1 ]; then exit; fi


	#check if it's a new best
	res=$( echo "$result > $bestResult" | bc -l ) #maybe line splitting of bc is the problem
	echo "script: comparison: $result > $bestResult = $res"
	if [[ ${res} -eq 1 ]]
	then
	  echo "script: set best values to current values"
		#if it is set the best values to the current ones
		bestResult=$result
		bestM1=("${multipliers1[@]}")
		bestM2=("${multipliers2[@]}")
		bestM3=("${multipliers3[@]}")
	fi

	echo "script: Currently best results:"
	echo "script: result: $bestResult"
  echo "BestMultipliers1"
  echo "${bestM1[@]}"
  echo "BestMultipliers2"
  echo "${bestM2[@]}"
  echo "BestMultipliers3"
  echo "${bestM3[@]}"
	echo ""

	i=$((i+1))
	
	#read -p "Press enter to start next game"
	#clear
done


