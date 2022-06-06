#!/bin/bash
clear

#client=client05.jar
client=revxt-ss22-g05.jar
korrekte_ausgaben=0
anzahl_karten=0

echo "Client: $client"
cd ..
cd bin
chmod 770 $client

#get all Maps
cd ..
cd Maps
maps=($(ls | grep "Map."))

#makes all maps readable
for mapName in "${maps[@]}" 
do
    chmod 770 $mapName
	anzahl_karten=$((anzahl_karten+1))
done

cd ..
cd serverAndAi

for mapName in "${maps[@]}" 
do
    # start own client
    sleep 3 && java -jar ../bin/revxt-ss22-g05.jar --server &
    pid1=$!

	#get anzPlayer 
	anzPlayer=$(awk '(NR==1){printf("%d",$1)}' "../Maps/$mapName")

    ii=1
	pidAIs=()
	while [ $ii -lt $anzPlayer ]
	do
		sleep 3 && ./ai_trivial -q &
		pidAIs+=(ii)
		ii=$((ii+1))
	done

    #start server
    #./server_nogl -m -C ../Maps/$mapName > Server_View.txt  #without output of server
    ./server_nogl -C -m ../Maps/$mapName | tee Server_View.txt #with output of server

    #wait for client
    wait $pid1
	
	#wait for ai's
	for pid2 in "${pidAIs[@]}"
	do
		wait $pid
	done
    echo "Spiel Fertig"

    sleep 10
done