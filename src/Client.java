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

		//get call argumets
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
			System.err.println("Couln't connect to server");
			return;
		}

		//get map from server
		map = serverM.getMap();

		//check if it imported correctly
		if(map == null || !map.importedCorrectly) {
			System.err.println("Map coun't be loaded correctly");
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
	 * Plays the Game. Gets Messages from server and calls methods to handle the different kinds
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
					if (printOn) System.out.println("recieved Move Request");

					//read rest of move request
					serverM.readRestOfMoveRequest(); //ignore at the moment

					//Hanlde Move Request - Both functions print the map with the possible moves marked
					if (firstPhase) makeAMove();
					else setABomb();
					break;

				case 6: //Move
					if (printOn) System.out.println("recieved Move");

					//read rest of Message
					int[] moveInfos = serverM.readRestOfMove();
					Position posToSetKeystone = new Position(0,0);
					posToSetKeystone.x = moveInfos[0] + 1; //index shift
					posToSetKeystone.y = moveInfos[1] + 1; //index shift
					int addditionalInfo = moveInfos[2];
					int moveOfPlayer = moveInfos[3];

					//Handle Move
					if (printOn) System.out.println("Player " + moveOfPlayer + " set keystone to " + posToSetKeystone + ". Additional: " + addditionalInfo);
					if (firstPhase) updateMapWithMove(posToSetKeystone, addditionalInfo, moveOfPlayer, map);
					else updateMapAfterBombingBFS(posToSetKeystone.x, posToSetKeystone.y, map);
					System.out.println(map.toString(null,false,true));
					break;

				case 7: //Disqualification
					if (printOn) System.out.println("recieved Disqualification");
					int player = serverM.readRestOfDisqualification();
					map.disqualifyPlayer(player);
					if (printOn) System.out.println("Player " + player + " was disqualified");
					if (player == myPlayerNr) gameOngoing = false;
					break;

				case 8: //End of Phase 1
					if (printOn) System.out.println("recieved end of phase 1");
					serverM.readRestOfNextPhase();
					firstPhase = false;
					break;

				case 9: //End of Phase 2
					if (printOn) System.out.println("recieved end of phase 2 - game ended");
					serverM.readRestOfNextPhase();
					gameOngoing = false;
					break;

				case -1:
					gameOngoing = false;
					System.err.println("Recieved some Message that couln't be handled");
					break;
			}
		}
	}

	//phase 1

	/**
	 * Method to make a move after a move request was sent to the Client
	 */
	private void makeAMove(){
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

		//no check neccessaray if valid moves are empty because server says there not

		if (printOn) {
			System.out.println("Possible Moves:");
			System.out.println(Arrays.toString(validMoves.toArray()));
		}

   		while (!moveIsPossible) {
			//enter the move
			if (!moveRandom && printOn) System.out.print("Geben Sie den naechsten zug ein (x,y): ");

			//make a random move
			if (moveRandom) {
				int index;
				index = getNextMoveWithHeuristik(validMoves, true);
				posToSetKeystone = validMoves.get(index);
				if (printOn) System.out.println("Set Keystone at: " + posToSetKeystone);
			}
			//let player enter a move
			if (!moveRandom) {
				posToSetKeystone.x = sc.nextInt();
				posToSetKeystone.y = sc.nextInt();
			}
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


		//check where we would set oure keystone on and act accordingly
		char fieldvalue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

		Character choiceChar = null;
		Integer choiceInt = null;
		switch (fieldvalue) {
			case 'b': {
				if (!moveRandom) {
					if (printOn) System.out.println("Wollen sie eine Bombe (b) oder einen Ueberschreibstein (u)?");
					choiceChar = sc.next().charAt(0);
				}
				else {
					if (randomIndex.nextInt(2) == 1){ //2 is excluded
						choiceChar = 'b';
					}
					else {
						choiceChar = 'u';
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
					choiceInt = randomIndex.nextInt(8) + 1;
				}
				break;
			}
		}

		//send message where to move
		char addidionalInfo = '0';
		if (choiceChar != null) addidionalInfo = choiceChar;
		if (choiceInt != null) addidionalInfo = choiceInt.toString().charAt(0);
		serverM.sendMove(posToSetKeystone.x, posToSetKeystone.y, addidionalInfo, myPlayerNr);
	}

	/**
	 * Method to update the Map according to the move that was sent to the client
	 */
	private static void updateMapWithMove(Position posToSetKeystone, int addditionalInfo, int moveOfPlayer, Map map) {
		char fieldvalue;

		map.setPlayer(moveOfPlayer); //set playing player because server could have skipped some

		//get value of field where next keystone is set
		fieldvalue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

		//color the map
		colorMap(posToSetKeystone, map);

		//handle special moves
		switch (addditionalInfo){
			case 0: //could be normal move, overwrite move, or inversion move
				//for a normal move there are no futher actions neccessary
				//overwrite move
				if ((Character.isDigit(fieldvalue) && fieldvalue != '0') || fieldvalue == 'x') {
					map.DecreaseOverrideStonesofPlayer();
				}
				//inversion move
				else if (fieldvalue == 'i') {
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
			case 8: //choise
				map.swapStonesWithOnePlayer(addditionalInfo);
				break;
			case 20: //bonus and want a bomb
				map.IncreaseBombsofPlayer();
				break;
			case 21: //bonus and want an overwrite stone
				map.IncreaseOverrideStonesofPlayer();
				break;
			default:
				System.err.println("Fieldvalue ist ungültig");
				break;
		}

		map.nextPlayer();
	}

	//phase 2 - bomb phase

	private void setABomb(){
		char fieldvalue;
		ArrayList<Position> validMoves = new ArrayList<>();
		boolean moveIsPossible = false;
		Position posToSetKeystone = new Position(0, 0);
		Scanner sc = new Scanner(System.in);

		//gets the possible positions to set a bomb at
		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){
				fieldvalue = map.getCharAt(x, y);
				if (fieldvalue != '-' && fieldvalue != 't'){
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
			if (!moveRandom && printOn) System.out.print("Geben Sie den naechsten zug ein (x,y): ");

			//make a random move
			if (moveRandom) {
				int index;
				index = getNextMoveWithHeuristik(validMoves, false);
				posToSetKeystone = validMoves.get(index);
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
		Queue<int[]> posQ = new LinkedList<>(); //int array: [0] == x, [1] == y, [2] == distance from explostion
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

					//if there's a transition go throu and delete it (not the char though)
					if (charAtPos == 't') {
						//go one step back because we need to come from where the transition points
						posAfterStep = Position.goInR(posAfterStep, (r + 4) % 8);
						//tries to go through transition
						newR = doAStep(posAfterStep, r, map); //takes Position it came from. Because from there it needs to go through
						if (newR != null) {
							//removes transtion pair from the hash List - if it's here it wen through the transition
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

	//caculate next move
	private int getNextMoveWithHeuristik(ArrayList<Position> validMoves, boolean phaseOne){
		ArrayList<Double> valueOfMap = new ArrayList<>();
		ArrayList<Position> validMovesCoise = new ArrayList<>();
		Map nextMap;
		Heuristik nextHeuristik;
		char charAtNextPos;
		int additionalInfo = 0;


		for (Position pos : validMoves){
			nextMap = new Map(map);
			nextMap.setPlayer(myPlayerNr);
			charAtNextPos = nextMap.getCharAt(pos);
			if (phaseOne) {
				if (charAtNextPos == 'c') {
					validMovesCoise.add(pos);
					continue;
				}
				if (charAtNextPos == 'b') {
					//split in two
					additionalInfo = 20;

					//first branche
					updateMapWithMove(pos, additionalInfo, myPlayerNr, nextMap);
					nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
					valueOfMap.add(nextHeuristik.evaluate());

					//second branche
					additionalInfo = 21;
				}
				updateMapWithMove(pos, additionalInfo, myPlayerNr, nextMap);
			}
			else {
				updateMapAfterBombingBFS(pos.x, pos.y, nextMap);
			}

			nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
			valueOfMap.add(nextHeuristik.evaluate());
		}

		for (Position pos : validMovesCoise){
			for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
				if (playerNr == myPlayerNr) continue;

				nextMap = new Map(map);
				nextMap.setPlayer(myPlayerNr);

				updateMapWithMove(pos, playerNr, myPlayerNr, nextMap);

				nextHeuristik = new Heuristik(nextMap, myPlayerNr, false);
				valueOfMap.add(nextHeuristik.evaluate());
			}
		}

		Double heighest = Collections.max(valueOfMap);
		int indexOfHeighest = 0;
		for (Double d : valueOfMap){
			if (d == heighest) indexOfHeighest = valueOfMap.indexOf(d); //Objects.equals(d, heighest)
		}
		return indexOfHeighest;
	}

	//functions to calculate possible moves

	/**
	 * returns the possible moves on the current map
	 * @param map map to check for possible moves
	 * @return returns an Array List of Positions
	 */
	public static ArrayList<Position> getValidMoves(Map map) {
		Moves moves = new Moves();

		//TODO: add seperation wich move finder should be jused
		getCandidatesByNeighbour(map, moves);
		deleteNotPossibleMoves(map, moves);
		
		return moves.possibleMoves;
	}

	/**
	 * Checks the whole map for enemy players and adds blank fields around it (by Neighboure) to candidates in moves
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
					//Add finaly - mo need to check
					moves.possibleMoves.add(new Position(x,y));
				}

				//if a player is there
				if (Character.isDigit(currChar) && currChar != '0'){

					//If the spot is taken, and you own an OverrideStone you have to check this spot
					if (map.getOverwriteStonesForPlayer(currentlyPlaying) > 0)
					{
						moves.addPositionInAllDirections(x, y);
					}
					//check all neighboures and add it if the field is 0
					checkAllNeighboures(map, moves, x, y);
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
	 * Used for getCandidatesByNeighboure. Does the checking around an enemy keystone
	 * @param map map the check takes place on
	 * @param moves Data structure where all the moves to check are stored
	 * @param x x position of enemy keystone to check
	 * @param y y position of enemy keystone to check
	 */
	private static void checkAllNeighboures(Map map, Moves moves, int x, int y){
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
			newR = doAStep(currPos,r,map); //is only executed once per for loop so change of r doesn't affact it
			if (newR == null) continue;

			//get char at position
			fieldInDirectionR = map.getCharAt(currPos);

			//for a blank field add a possible move
			if (fieldInDirectionR == blankField){
				//get opposite direction/ the direction it needs to go to check the posibility of the move

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
	 * Goes over every move to check in the Moves data stucture and calls checkIfMovePossible for it to test if it's a valid move
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

			//go in one direction until there is something relevant //TODO: check if it can go forver
			while (true) {
				//does one step
				newR = doAStep(currPos, newR, map); //currPos is changed here
				if (newR == null) break; //if the step wasn't possible

				//check what's there
				currChar = map.getCharAt(currPos);
				//check for blank
				if (currChar == '0') break;
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
	 * colores the mapToColor when the keystone is placed in the specified position
	 * @param pos position where the keystone is placed
	 * @param mapToColor the mapToColor on wich it is placed
	 */
	private static void colorMap(Position pos, Map mapToColor){
		Position StartingPos;
		Position currPos;
		Integer newR;
		boolean wasFirstStep;
		boolean foundEnd;
		char currChar;
		LinkedHashSet<Position> positionsToColor = new LinkedHashSet<>(); //doesn't store duplicates
		ArrayList<Position> positionsAlongOneDirection;

		//TODO: handle overwirtte stones right
		if (mapToColor.getCharAt(pos) == 'x' && mapToColor.getOverwriteStonesForPlayer(mapToColor.getCurrentlyPlayingI()) > 0) mapToColor.setCharAt(pos.x, pos.y, mapToColor.getCurrentlyPlayingC());

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

			//go in one direction until there is something relevant //TODO: check if it can go forver
			while (true) {
				//does one step
				newR = doAStep(currPos, newR, mapToColor); //currPos is changed here
				if (newR == null) break; //if the step wasn't possible
				if(currPos.equals(StartingPos)) break;
				//check what's there
				currChar = mapToColor.getCharAt(currPos);
				//check for blank
				if (currChar == '0' || currChar == 'i' || currChar == 'c' ||currChar == 'b') break;
				//check for players
				//if it's the first move - finding an own keystone isn't a connection but cancels the search in that direction
				if (wasFirstStep) {
					//if there is a keystone of your own, and it's the first step
					if (currChar == mapToColor.getCurrentlyPlayingC()) break;
					wasFirstStep = false;
				}
				//if it's not the first move - finding an own keystone is a connection
				else {
					//if there is a keystone of your own, and it's not the first step
					if (currChar == mapToColor.getCurrentlyPlayingC()) {
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
			mapToColor.setCharAt(posToColor.x, posToColor.y, mapToColor.getCurrentlyPlayingC());
		}
	}


	/**
	 * goes one step in the specified direction. If there's a wall or the end of the map it returns null if there's a transition it goes throug it
	 * @param pos strat position
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
			newR = (newR+4)%8; //flips direction because transition came out of that direction, so you go throu the other way
		}

		//sets the position to the new One (call by reference)
		pos.x = newPos.x;
		pos.y = newPos.y;
		return newR;
	}


}
