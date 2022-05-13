BEGIN{
	#print groupID
}

/This is a .-player map\./{
	anzPlayer = substr($4,1,1)	
}

/Waiting for group id of client .\.\.\.OK. Group ID is .*/{
	group = substr($11,1,length($11)-1)
	#print "Player", substr($7,1,1) , "Group",group

	if (groupID == group){
		player = substr($7,1,1)
	}
}

/Player .: [0-9]* points/{
	currPlayer = substr($2,1,1)
	score = $3

	#print "Player", substr($2,1,1) , "Score",$3
	
	scores[currPlayer] = score

	if (currPlayer == player){
		oureScore = score
	}
}

END {
	#print "Anz Player", anzPlayer
	#print "Oure Player", player
	#print "Oure Score", oureScore

	#get placement
	placement = 1
	for (score in scores){
		if (scores[score] > oureScore && score != player){
			placement = placement + 1		
		}
	}
	#print "placement:", placement

	#get return value
	result = 1 - ((placement - 1) / (anzPlayer - 1))
	printf("%f",result)
}

