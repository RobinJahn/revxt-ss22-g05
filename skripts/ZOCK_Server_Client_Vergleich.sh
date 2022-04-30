#!/bin/bash
clear

client=client05.jar
korrekte_ausgaben=0
anzahl_karten=0

echo "Client: $client"

chmod 770 $client
chmod 770 ZOCK_Server_Client_Vergleich.jar

#get all Maps
maps=($(ls | grep "Map."))

#makes all maps readable
for mapName in "${maps[@]}" 
do
    chmod 770 $mapName
	anzahl_karten=$((anzahl_karten+1))
done    

for mapName in "${maps[@]}" 
do
    # start own client
    sleep 3 && java -jar $client -s &
    pid1=$!

	#get anzPlayer 
	anzPlayer=$(awk '(NR==1){printf("%d",$1)}' "$mapName")

    ii=1
	pidAIs=()
	while [ $ii -lt $anzPlayer ]
	do
		sleep 3 && ./ai_trivial -q &
		pidAIs+=(ii)
		ii=$((ii+1))
	done

    #start server
    #./server_nogl -m -C $mapName > Server_View.txt  #without output of server
    ./server_nogl -C -m $mapName | tee Server_View.txt #with output of server

    #wait for client
    wait $pid1
	
	#wait for ai's
	for pid2 in "${pidAIs[@]}"
	do
		wait $pid
	done
    echo "Spiel Fertig"

    errorcount=$(java -jar ZOCK_Server_Client_Vergleich.jar)
	if [ $errorcount -eq 0 ]
	then
		echo "Client und Server View identisch"
		korrekte_ausgaben=$((korrekte_ausgaben+1))
	fi
	if [ $errorcount -gt 0 ]
	then
		echo "$errorcount Unterschiede in den Views"
		cp Client_View.txt Client_View_Fehler_Map_$((korrekte_ausgaben+1)).txt
		cp Server_View.txt Server_View_Fehler_Map_$((korrekte_ausgaben+1)).txt
	fi
    sleep 10
done
echo ""
echo "$korrekte_ausgaben / $anzahl_karten Karten sind Fehlerfrei"