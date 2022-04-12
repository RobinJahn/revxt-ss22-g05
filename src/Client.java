package src;

import java.io.IOException;
import java.util.*;

class Moves {
	public ArrayList<Position> possibleMoves;
	public HashMap<Position, ArrayList<Integer>> movesToCheck;

	public Moves() {
		possibleMoves = new ArrayList<>();
		movesToCheck = new HashMap<>();
	}

	public void addPositionInAllDirections(int x, int y){
		ArrayList<Integer> directions = new ArrayList<>();
		for (int r = 0; r <= 7; r++){
			directions.add(r);
		}
		movesToCheck.put(new Position(x,y), directions);
	}
}

public class Client {
	//final variables
	final boolean moveRandom = true;
	final boolean printOn;

	//global variables
	Map map;
	ServerMessenger serverM;
	Heuristik heuristik;
	Random randomIndex = new Random(1);
	int myPlayerNr;


	public static void main(String[] args) {
		Client c1;

		//variables for the server
		String ip = "127.0.0.1";
		int port = 7777;

		//get call arguments
		for (int i = 0; i < args.length-1; i++){
			if (Objects.equals(args[i], "-i")){
				i++;
				ip = args[i];
			}
			if (Objects.equals(args[i], "-p")){
				i++;
				port = Integer.parseInt(args[i]);
			}
		}

		c1 = new Client(ip,port,true);
	}

	//functions that let the client play

	/**
	 * Constructor of the Client.
	 * Connects to Server and starts playing
	 * @param ip ip of the server
	 * @param port port of the server
	 */
	public Client(String ip, int port, boolean printOn){
		this.printOn = printOn;
		//try to connect with server
		try {
			serverM = new ServerMessenger(ip,port);
			if (printOn) System.out.println("Client Successfully connected to server");
		} catch (IOException e) {
			System.err.println("Couldn't connect to server");
			return;
		}

		//get map from server
		map = serverM.getMap();

		//check if it imported correctly
		if(map == null || !map.importedCorrectly) {
			System.err.println("Map couldn't be loaded correctly");
			return;
		}
		else {
			if (printOn) System.out.println("Map was loaded correctly");
		}

		//get own player number
		myPlayerNr = serverM.getPlayerNumber();
		if(printOn) System.out.println("Own Player Number is: " + myPlayerNr);

		//set variables after map was imported
		heuristik = new Heuristik(map, myPlayerNr,true);

		//start playing
		System.out.println();
		play();
	}

	/**
	 * Plays the Game. Get Messages from server and calls methods to handle the different kinds
	 */
	private void play(){
		int messageType;
		boolean gameOngoing = true;
		boolean firstPhase = true;

		System.out.println(map.toString(null,false,true));

		while (gameOngoing) {

			if (printOn) System.out.print("Waiting for Message - ");
			messageType = serverM.waitForMessage();

			switch (messageType) {

				case 4: //Move Request
					if (printOn) System.out.println("received Move Request");

					//read rest of move request
					serverM.readRestOfMoveRequest(); //ignore at the moment

					//Handle Move Request - Both functions print the map with the possible moves marked
					if (firstPhase) makeAMove();
					else setABomb();
					break;

				case 6: //Move
					if (printOn) System.out.println("received Move");

					//read rest of Message
					int[] moveInfos = serverM.readRestOfMove();
					Position posToSetKeystone = new Position(0,0);
					posToSetKeystone.x = moveInfos[0] + 1; //index shift
					posToSetKeystone.y = moveInfos[1] + 1; //index shift
					int additionalInfo = moveInfos[2];
					int moveOfPlayer = moveInfos[3];

					//Handle Move
					if (printOn) System.out.println("Player " + moveOfPlayer + " set keystone to " + posToSetKeystone + ". Additional: " + additionalInfo);
					if (firstPhase) updateMapWithMove(posToSetKeystone, additionalInfo, moveOfPlayer, map);
					else updateMapAfterBombingBFS(posToSetKeystone.x, posToSetKeystone.y, map);
					System.out.println(map.toString(null,false,true));
					break;

				case 7: //Disqualification
					if (printOn) System.out.println("received Disqualification");
					int player = serverM.readRestOfDisqualification();
					map.disqualifyPlayer(player);
					if (printOn) System.out.println("Player " + player + " was disqualified");
					if (player == myPlayerNr) gameOngoing = false;
					break;

				case 8: //End of Phase 1
					if (printOn) System.out.println("received end of phase 1");
					serverM.readRestOfNextPhase();
					firstPhase = false;
					break;

				case 9: //End of Phase 2
					if (printOn) System.out.println("received end of phase 2 - game ended");
					serverM.readRestOfNextPhase();
					gameOngoing = false;
					break;

				case -1:
					gameOngoing = false;
					System.err.println("Received some Message that couldn't be handled");
					break;
			}
		}
	}

	//phase 1

	/**
	 * Method to make a move after a move request was sent to the Client
	 */
	private void makeAMove(){
		int[] positionAndInfo = new int[3];
		boolean moveIsPossible = false;
		double valueOfMap;
		Position posToSetKeystone = new Position(0, 0);
		Scanner sc = new Scanner(System.in);

		map.setPlayer(myPlayerNr);

		//calculate possible moves and print map with these
		ArrayList<Position> validMoves = getValidMoves(map);
		if (printOn) System.out.println(map.toString(validMoves, false, true));

		//calculate value of map and print it
		valueOfMap = (double)Math.round(heuristik.evaluate()*100)/100;
		if (printOn) System.out.println("Value of Map is " + valueOfMap);

		//no check necessary if valid moves are empty because server says there not

		if (printOn) {
			System.out.println("Possible Moves:");
			System.out.println(Arrays.toString(validMoves.toArray()));
		}

   		while (!moveIsPossible) {
			//enter the move
			if (!moveRandom && printOn) System.out.print("Enter the next Move (x,y): ");

			//make a random move
			if (moveRandom) {
				positionAndInfo = getNextMoveWithHeuristik(validMoves, true);
				posToSetKeystone = new Position(positionAndInfo[0], positionAndInfo[1]);
				moveIsPossible = true; //no need to check th move again
				if (printOn) System.out.println("Set Keystone at: " + posToSetKeystone);
			}
			//let player enter a move
			if (!moveRandom) {
				posToSetKeystone.x = sc.nextInt();
				posToSetKeystone.y = sc.nextInt();

				if (printOn) System.out.println();

				//check if the move is valid
				ArrayList<Integer> directions = new ArrayList<>();
				for (int i = 0; i <= 7; i++) directions.add(i);
				boolean movePossible = checkIfMoveIsPossible(posToSetKeystone, directions, map);
				if (!movePossible) {
					System.err.println("Move isn't possible");
				}
				else {
					moveIsPossible = true;
				}
			}
		}

	   	//TODO: Do the following better - random player doesn't need all that
		//check where we would set our keystone on and act accordingly
		char fieldValue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

		Character choiceChar = null;
		Integer choiceInt = null;
		switch (fieldValue) {
			case 'b': {
				if (!moveRandom) {
					if (printOn) System.out.println("Do you want a bomb (b) or an overwrite-stone (o)?");
					choiceChar = sc.next().charAt(0);
				}
				else {
					if (positionAndInfo[2] == 20){
						choiceChar = 'b';
					}
					else if (positionAndInfo[2] == 21){
						choiceChar = 'o';
					}
				}
				break;

			}
			case 'c': {
				if (!moveRandom) {
					if (printOn) System.out.println("Mit wem wollen sie die Farbe tauschen ?");
					choiceInt = sc.nextInt();
				}
				else {
					choiceInt = positionAndInfo[2];
				}
				break;
			}
		}

		//send message where to move
		char additionalInfo = '0';
		if (choiceChar != null) additionalInfo = choiceChar;
		if (choiceInt != null) additionalInfo = choiceInt.toString().charAt(0);
		serverM.sendMove(posToSetKeystone.x, posToSetKeystone.y, additionalInfo, myPlayerNr);
	}

	/**
	 * Method to update the Map according to the move that was sent to the client
	 */
	private static void updateMapWithMove(Position posToSetKeystone, int additionalInfo, int moveOfPlayer, Map map) {
		char fieldValue;

		map.setPlayer(moveOfPlayer); //set playing player because server could have skipped some

		//get value of field where next keystone is set
		fieldValue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

		//color the map
		colorMap(posToSetKeystone, map);

		//handle special moves
		switch (additionalInfo){
			case 0: //could be normal move, overwrite move, or inversion move
				//for a normal move there are no further actions necessary
				//overwrite move
				if ((Character.isDigit(fieldValue) && fieldValue != '0') || fieldValue == 'x') {
					map.decreaseOverrideStonesOfPlayer();
				}
				//inversion move
				else if (fieldValue == 'i') {
					map.Inversion();
				}
				break;
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8: //choice
				map.swapStonesWithOnePlayer(additionalInfo);
				break;
			case 20: //bonus and want a bomb
				map.increaseBombsOfPlayer();
				break;
			case 21: //bonus and want an overwrite-stone
				map.increaseOverrideStonesOfPlayer();
				break;
			default:
				System.err.println("Field value is invalid");
				break;
		}

		map.nextPlayer();
	}

	//phase 2 - bomb phase

	private void setABomb(){
		int[] positionAndInfo = new int[3];
		char fieldValue;
		ArrayList<Position> validMoves = new ArrayList<>();
		boolean moveIsPossible = false;
		Position posToSetKeystone = new Position(0, 0);
		Scanner sc = new Scanner(System.in);

		//gets the possible positions to set a bomb at
		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){
				fieldValue = map.getCharAt(x, y);
				if (fieldValue != '-' && fieldValue != 't'){
					validMoves.add(new Position(x,y));
				}
			}
		}

		if (printOn) System.out.println(map.toString(validMoves, false, true));

		//print possible moves
		if (printOn) {
			System.out.println("Possible Moves:");
			System.out.println(Arrays.toString(validMoves.toArray()));
		}

		//get a valid move
		while (!moveIsPossible) {
			//enter the move
			if (!moveRandom && printOn) System.out.print("Enter the next move (x,y): ");

			//make a random move
			if (moveRandom) {
				int index;
				positionAndInfo = getNextMoveWithHeuristik(validMoves, false);
				posToSetKeystone = new Position(positionAndInfo[0], positionAndInfo[1]);
				System.out.println("Set Keystone at: " + posToSetKeystone);
			}
			//let player enter a move
			if (!moveRandom) {
				posToSetKeystone.x = sc.nextInt();
				posToSetKeystone.y = sc.nextInt();
			}
			if (printOn) System.out.println();

			//check if the move is valid
			moveIsPossible = validMoves.contains(posToSetKeystone);
		}

		//send the move
		serverM.sendMove(posToSetKeystone.x, posToSetKeystone.y, '0', myPlayerNr);
	}

	/**
	 * Updates Map by breadth-first search
	 * @param x x coordinate where the bomb was set
	 * @param y y coordinate where the bomb was set
	 */
	private static void updateMapAfterBombingBFS(int x, int y, Map map){
		char charAtPos;
		int explosionRadius = map.getExplosionRadius();

		//for breadth-first search
		Queue<int[]> posQ = new LinkedList<>(); //int array: [0] == x, [1] == y, [2] == distance from explosion
		int[] currPosAndDist;
		Position nextPos;
		Position posAfterStep;
		int counterForExpRad = 0;

		//for transitions
		Integer newR;
		char transitionEnd1;
		Character transitionEnd2;

		//first element
		map.setCharAt(x, y, '+');
		posQ.add(new int[]{x,y, counterForExpRad});

		while (!posQ.isEmpty()){
			currPosAndDist = posQ.poll();
			nextPos = new Position(currPosAndDist[0], currPosAndDist[1]);
			counterForExpRad = currPosAndDist[2];

			//if explosion radius allows it
			if (counterForExpRad < explosionRadius) {

				//go in every possible direction
				for (int r = 0; r <= 7; r++) {
					//get position it will move to
					posAfterStep = Position.goInR(nextPos, r);
					//check what's there
					charAtPos = map.getCharAt(posAfterStep);

					//if there's a transition go through and delete it (not the char though)
					if (charAtPos == 't') {
						//go one step back because we need to come from where the transition points
						posAfterStep = Position.goInR(posAfterStep, (r + 4) % 8);
						//tries to go through transition
						newR = doAStep(posAfterStep, r, map); //takes Position it came from. Because from there it needs to go through
						if (newR != null) {
							//removes transition pair from the hash List - if it's here it wen through the transition
							transitionEnd1 = Transitions.saveInChar(posAfterStep.x, posAfterStep.y, (newR + 4) % 8);
							transitionEnd2 = map.transitionen.get(transitionEnd1);
							map.transitionen.remove(transitionEnd1);
							map.transitionen.remove(transitionEnd2);
						}
					}

					if (charAtPos != '+' && charAtPos != '-') { // + is grey, - is black
						map.setCharAt(posAfterStep.x, posAfterStep.y, '+');
						posQ.add(new int[]{posAfterStep.x, posAfterStep.y, counterForExpRad + 1});
					}
				}
			}
			map.setCharAt(nextPos.x, nextPos.y, '-'); //next position is still the position it came from
		}
	}

	//calculate next move
	private int[] getNextMoveWithHeuristik(ArrayList<Position> validMoves, boolean phaseOne){
		ArrayList<Double> valueOfMap = new ArrayList<>();
		ArrayList<int[]> valueOfMapPosAndInfo = new ArrayList<>();
		ArrayList<Position> validMovesChoice = new ArrayList<>();
		Map nextMap;
		Heuristik nextHeuristik;
		char charAtNextPos;
		int additionalInfo = 0;
		int indexOfHighest;

		for (Position pos : validMoves){
			nextMap = new Map(map);
			nextMap.setPlayer(myPlayerNr);
			charAtNextPos = nextMap.getCharAt(pos);
			if (phaseOne) {
				if (charAtNextPos == 'c') {
					validMovesChoice.add(pos);
					continue;
				}
				if (charAtNextPos == 'b') {
					//split in two

					//first branche
					additionalInfo = 20;

					updateMapWithMove(pos, additionalInfo, myPlayerNr, nextMap);
					nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
					valueOfMap.add(nextHeuristik.evaluate());
					valueOfMapPosAndInfo.add(new int[]{pos.x, pos.y, 20});

					//second branche
					additionalInfo = 21;

					updateMapWithMove(pos, additionalInfo, myPlayerNr, nextMap);
					nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
					valueOfMap.add(nextHeuristik.evaluate());
					valueOfMapPosAndInfo.add(new int[]{pos.x, pos.y, 21});
					continue;
				}

				//is also executed for normal moves
				updateMapWithMove(pos, additionalInfo, myPlayerNr, nextMap);
			}
			else {
				updateMapAfterBombingBFS(pos.x, pos.y, nextMap);
			}

			nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
			valueOfMap.add(nextHeuristik.evaluate());
			valueOfMapPosAndInfo.add(new int[]{pos.x,pos.y,0});
		}

		for (Position pos : validMovesChoice){
			for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
				if (playerNr == myPlayerNr) continue;

				nextMap = new Map(map);
				nextMap.setPlayer(myPlayerNr);

				updateMapWithMove(pos, playerNr, myPlayerNr, nextMap);

				nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
				valueOfMap.add(nextHeuristik.evaluate());
				valueOfMapPosAndInfo.add(new int[]{pos.x, pos.y, playerNr});
			}
		}

		Double highest = Collections.max(valueOfMap);
		indexOfHighest = valueOfMap.indexOf(highest);

		return valueOfMapPosAndInfo.get(indexOfHighest); //returns the position and the additional info of the move that has the highest evaluation
	}

	//functions to calculate possible moves

	/**
	 * returns the possible moves on the current map
	 * @param map map to check for possible moves
	 * @return returns an Array List of Positions
	 */
	public static ArrayList<Position> getValidMoves(Map map) {
		Moves moves = new Moves();

		//TODO: add separation wich move finder should be used
		getCandidatesByNeighbour(map, moves);
		deleteNotPossibleMoves(map, moves);
		
		return moves.possibleMoves;
	}

	/**
	 * Checks the whole map for enemy players and adds blank fields around it (by Neighbour) to candidates in moves
	 * @param map the map to check for candidates
	 * @param moves the data structure to store the possible moves and the candidates in
	 */
	private static void getCandidatesByNeighbour(Map map, Moves moves){
		int currentlyPlaying = map.getCurrentlyPlayingI();
		char currChar;

		//goes over every field
		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){

				//gets char of current position
				currChar = map.getCharAt(x,y);

				//if there's an expansions field and the player has overwrite-stones
				if (Character.isAlphabetic(currChar) && currChar == 'x' && map.getOverwriteStonesForPlayer(currentlyPlaying) > 0){
					//Add finally - mo need to check
					moves.possibleMoves.add(new Position(x,y));
				}

				//if a player is there
				if (Character.isDigit(currChar) && currChar != '0'){

					//If the spot is taken, and you own an OverrideStone you have to check this spot
					if (map.getOverwriteStonesForPlayer(currentlyPlaying) > 0)
					{
						moves.addPositionInAllDirections(x, y);
					}
					//check all neighbours and add it if the field is 0
					checkAllNeighbors(map, moves, x, y);
				}
				
				if(currChar == 'i') {
					moves.addPositionInAllDirections(x, y);
				}
				if(currChar == 'c') {
					moves.addPositionInAllDirections(x, y);
				}
				if(currChar == 'b') {
					moves.addPositionInAllDirections(x, y);
				}
			}
		}
	}

	/**
	 * Used for getCandidatesByNeighbour. Does the checking around an enemy keystone
	 * @param map map the check takes place on
	 * @param moves Data structure where all the moves to check are stored
	 * @param x x position of enemy keystone to check
	 * @param y y position of enemy keystone to check
	 */
	private static void checkAllNeighbors(Map map, Moves moves, int x, int y){
		Position startPos = new Position(x,y);
		Position currPos;
		Integer newR;
		int oppositeDirection;
		char blankField = '0';
		char fieldInDirectionR;

		// go in every direction and check if there's a free field where you could place a keystone
		for (int r = 0; r <= 7; r++){
			//resets position
			currPos = startPos.clone(); //resets currPos to startPos

			//change x and y according to direction
			newR = doAStep(currPos,r,map); //is only executed once per for loop so change of r doesn't affect it
			if (newR == null) continue;

			//get char at position
			fieldInDirectionR = map.getCharAt(currPos);

			//for a blank field add a possible move
			if (fieldInDirectionR == blankField){
				//get opposite direction/ the direction it needs to go to check the possibility of the move

				oppositeDirection = (newR+4)%8;
				//get the directions that ware already added to this field
				ArrayList<Integer> directions = moves.movesToCheck.get(currPos);
				//if position didn't exist yet
				if (directions == null) {
					directions = new ArrayList<>();
					directions.add(oppositeDirection);
					moves.movesToCheck.put(currPos,directions);
				}
				//if position already existed
				else {
					directions.add(oppositeDirection);
				}
			}
		}
	}

	private static ArrayList<int[]> getFieldsByOwnColor(Map map){
		//TODO: complete
		return null;
	}

	/**
	 * Goes over every move to check in the Moves-data-structure and calls checkIfMovePossible for it to test if it's a valid move
	 * @param map the map the check takes place
	 * @param moves the moves to check
	 */
	private static void deleteNotPossibleMoves(Map map, Moves moves){
		boolean connectionFound;

		//check every move if it's possible
		for (Position pos : moves.movesToCheck.keySet()){

			//gets directions to check in
			ArrayList<Integer> directions;
			directions = moves.movesToCheck.get(pos);

			//function that checks if move is possible
			connectionFound = checkIfMoveIsPossible(pos, directions, map);
			//if the move is possible
			if (connectionFound) moves.possibleMoves.add(pos);
		}
	}

	/**
	 * goes in every specified direction to check if it's possible to set a keystone at the specified position
	 * @param pos pos the keystone would be placed
	 * @param directions directions it needs to check
	 * @param map the map the check takes place on
	 * @return returns true if the move is possible and false otherwise
	 */
	private static boolean checkIfMoveIsPossible(Position pos, ArrayList<Integer> directions, Map map){
		Position StartingPos;
		Position currPos;
		Integer newR;
		boolean wasFirstStep;
		char currChar;

		//check if it's an expansions field
		if (map.getCharAt(pos) == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) return true;

		//go over every direction that needs to be checked
		for (Integer r : directions){
			//reset values
			StartingPos = pos.clone();
			currPos = pos.clone();
			wasFirstStep = true;
			newR = r;

			//go in one direction until there is something relevant //TODO: check if it can go forever
			while (true) {
				//does one step
				newR = doAStep(currPos, newR, map); //currPos is changed here
				if (newR == null) break; //if the step wasn't possible

				//check what's there
				currChar = map.getCharAt(currPos);
				//check for blank
				if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b') break;
				if(currPos.equals(StartingPos)) break;
				//check for players
				//if it's the first move - finding an own keystone isn't a connection but cancels the search in that direction
				if (wasFirstStep) {
					if (currChar == map.getCurrentlyPlayingC()) break;
					wasFirstStep = false;
				}
				//if it's not the first move - finding an own keystone is a connection
				else {
					if (currChar == map.getCurrentlyPlayingC()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	//coloring of map

	/**
	 * colors the map when the keystone is placed in the specified position
	 * @param pos position where the keystone is placed
	 * @param map the map on wich it is placed
	 */
	private static void colorMap(Position pos, Map map){
		Position StartingPos;
		Position currPos;
		Integer newR;
		boolean wasFirstStep;
		boolean foundEnd;
		char currChar;
		LinkedHashSet<Position> positionsToColor = new LinkedHashSet<>(); //doesn't store duplicates
		ArrayList<Position> positionsAlongOneDirection;

		//TODO: handle overwrite stones right
		if (map.getCharAt(pos) == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
			map.setCharAt(pos.x, pos.y, map.getCurrentlyPlayingC());
		}

		//checks every direction for a connection and adds the positions in between to color them later
		for (int r = 0; r <= 7; r++){
			//reset values
			StartingPos = pos.clone();
			currPos = pos.clone();
			wasFirstStep = true;
			newR = r;
			positionsAlongOneDirection = new ArrayList<>();
			positionsAlongOneDirection.add(currPos.clone());
			foundEnd = false;

			//go in one direction until there is something relevant //TODO: check if it can go forever
			while (true) {
				//does one step
				newR = doAStep(currPos, newR, map); //currPos is changed here
				if (newR == null) break; //if the step wasn't possible
				if(currPos.equals(StartingPos)) break;
				//check what's there
				currChar = map.getCharAt(currPos);
				//check for blank
				if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b') break;
				//check for players
				//if it's the first move - finding an own keystone isn't a connection but cancels the search in that direction
				if (wasFirstStep) {
					//if there is a keystone of your own, and it's the first step
					if (currChar == map.getCurrentlyPlayingC()) break;
					wasFirstStep = false;
				}
				//if it's not the first move - finding an own keystone is a connection
				else {
					//if there is a keystone of your own, and it's not the first step
					if (currChar == map.getCurrentlyPlayingC()) {
						foundEnd = true;
						break;
					}
				}
				//when adding the position to the list HERE it doesn't add the ones that break the loop
				positionsAlongOneDirection.add(currPos.clone());
			}
			//if it found a connection it adds all the moves along the way to the positions to color
			if (foundEnd) positionsToColor.addAll(positionsAlongOneDirection); //doesn't add duplicates because of LinkedHashSet
		}

		//colors the positions
		for (Position posToColor : positionsToColor) {
			map.setCharAt(posToColor.x, posToColor.y, map.getCurrentlyPlayingC());
		}
	}


	/**
	 * goes one step in the specified direction. If there's a wall or the end of the map it returns null if there's a transition it goes through it
	 * @param pos start position
	 * @param r direction to do the step in
	 * @param mapToDoTheStepOn mapToDoTheStepOn where you are
	 * @return returns null if move isn't possible and the direction after the move if it is possible. If a transition changes the direction this is where to get the new one
	 */
	private static Integer doAStep(Position pos, int r, Map mapToDoTheStepOn){
		char transitionLookup;
		char charAtPos;
		Character transitionEnd;
		int newR = r;
		Position newPos;

		//check if step is valid in x direction
		if (pos.x == 0){
			if (r == 7 || r == 6 || r == 5) return null;
		}
		if (pos.x == mapToDoTheStepOn.getWidth()-1){
			if (r == 1 || r == 2 || r == 3) return null;
		}
		//check if step is valid in y direction
		if (pos.y == 0) {
			if (r == 7 || r == 0 || r == 1) return null;
		}
		if (pos.y == mapToDoTheStepOn.getHeight()-1){
			if (r == 3 || r == 4 || r == 5) return null;
		}

		//do the step
		newPos = Position.goInR(pos, r);

		charAtPos = mapToDoTheStepOn.getCharAt(newPos);

		//check if there's a wall
		if (charAtPos == '-') return null;

		//check if there is a transition
		if (charAtPos == 't') {
			//check if the direction matches the current one
			transitionLookup = Transitions.saveInChar(pos.x,pos.y,r); //pos is the old position
			transitionEnd = mapToDoTheStepOn.transitionen.get(transitionLookup);
			if (transitionEnd == null) return null; //if there isn't an entry

			//go through the transition
			newPos.x = Transitions.getX(transitionEnd);
			newPos.y= Transitions.getY(transitionEnd);
			newR = Transitions.getR(transitionEnd);
			newR = (newR+4)%8; //flips direction because transition came out of that direction, so you go through the other way
		}

		//sets the position to the new One (call by reference)
		pos.x = newPos.x;
		pos.y = newPos.y;
		return newR;
	}
}
