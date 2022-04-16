package src;

import java.io.IOException;
import java.rmi.ServerError;
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
	final boolean calculateMove = true;
	final boolean printOn;

	//global variables
	Map map;
	ServerMessenger serverM;
	Heuristik heuristik;
	Heuristik heuristikForSimulation;
	int myPlayerNr;
	int depth;
	int time;
	int moveCounter;

	public static void main(String[] args) {
		boolean printOn = false;
		boolean intellijPrint = false;
		//variables for the server
		String ip = "127.0.0.1";
		int port = 7777;

		//get call arguments
		for (int i = 0; i < args.length; i++){
			if (i < args.length-1) {
				if (Objects.equals(args[i], "-i")) {
					i++;
					ip = args[i];
				}
				if (Objects.equals(args[i], "-p")) {
					i++;
					port = Integer.parseInt(args[i]);
				}
			}
			if (Objects.equals(args[i], "-h")) printOn = true;
			if (Objects.equals(args[i], "-c")) intellijPrint = true;
		}

		//runn client
		new Client(ip,port,printOn);
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
		heuristik = new Heuristik(map, myPlayerNr,printOn);
		heuristikForSimulation = new Heuristik(map, myPlayerNr,false);

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
		int[] timeAndDepth;
		moveCounter = 0;

		if (printOn) System.out.println(map.toString(null,false,true));

		while (gameOngoing) {

			if (printOn) System.out.print("\nWaiting for Message - ");
			messageType = serverM.waitForMessage();

			switch (messageType) {

				case 4: //Move Request
					if (printOn) {
						System.out.println("received Move Request");
						System.out.println("Move: " + moveCounter);
					}

					//read rest of move request
					timeAndDepth = serverM.readRestOfMoveRequest(); //ignore at the moment
					if (time == -1 || depth == -1) {
						System.err.println("Time and Depth couldn't be read");
						gameOngoing = false;
						break;
					}
					else {
						time = timeAndDepth[0];
						depth = timeAndDepth[1];
					}

					//Handle Move Request - Both functions print the map with the possible moves marked
					if (firstPhase) makeAMove();
					else setABomb();
					break;

				case 6: //Move
					if (printOn) {
						System.out.println("received Move");
						System.out.println("Move: " + moveCounter++);
					}

					//read rest of Message
					int[] moveInfos = serverM.readRestOfMove();
					Position posToSetKeystone = new Position(0,0);
					posToSetKeystone.x = moveInfos[0] + 1; //index shift
					posToSetKeystone.y = moveInfos[1] + 1; //index shift
					int additionalInfo = moveInfos[2];
					int moveOfPlayer = moveInfos[3];

					//Handle Move
					if (printOn) System.out.println("Player " + moveOfPlayer + " set keystone to " + posToSetKeystone + ". Additional: " + additionalInfo);
					if (firstPhase) updateMapWithMove(posToSetKeystone, additionalInfo, moveOfPlayer, map, printOn);
					else updateMapAfterBombingBFS(posToSetKeystone.x, posToSetKeystone.y, map);
					if (printOn) {
						System.out.println(map.toString(null,false,true));
						//calculate value of map and print it
						double valueOfMap = (double)Math.round(heuristik.evaluate()*100)/100;
						System.out.println("Value of Map is " + valueOfMap);
					}
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
					System.err.println("Server closed connection or a message was received that couldn't be handled");
					break;
			}
		}
	}

	//phase 1

	/**
	 * Method to make a move after a move request was sent to the Client
	 */
	private void makeAMove(){
		//calculated Move
		int[] positionAndInfo = new int[3];

		//general
		double valueOfMap;
		ArrayList<int[]> validMoves;


		map.setPlayer(myPlayerNr);

		//calculate possible moves and print map with these
		validMoves = getValidMoves(map);
		if (printOn) System.out.println(map.toString(validMoves, false, true));

		//calculate value of map and print it
		valueOfMap = (double)Math.round(heuristik.evaluate()*100)/100;
		if (printOn) System.out.println("Value of Map is " + valueOfMap);

		if (validMoves.isEmpty()) {
			System.err.println("Something's wrong - Valid Moves are empty but server says they're not");
			return;
		}

		//make a calculated move
		if (calculateMove) {
			positionAndInfo = getNextMoveDFS(validMoves, true, depth);
			if (printOn) System.out.println("Set Keystone at: (" + positionAndInfo[0] + "," + positionAndInfo[1] + "," + positionAndInfo[2] + ")");
		}

		//let player enter a move
		if (!calculateMove) {
			//Variables needed for human player
			boolean moveIsPossible = false;
			char fieldValue;
			int additionalInfo = 0;
			Position posToSetKeystone = new Position(0, 0);
			Scanner sc = new Scanner(System.in);
			ArrayList<Integer> directions;
			char choiceChar;

			//print moves and evaluation
			positionAndInfo = getNextMoveDFS(validMoves, true, depth);
			if (printOn) System.out.println("Recommended Move: (" + positionAndInfo[0] + "," + positionAndInfo[1] + "," + positionAndInfo[2] + ")");

			//make a move
			while (!moveIsPossible) {
				if (printOn) System.out.print("Enter the next Move (x,y): ");

				try {
					//enter the move
					posToSetKeystone.x = sc.nextInt();
					posToSetKeystone.y = sc.nextInt();
				}
				catch (InputMismatchException e){
					System.err.println("Move isn't possible");
					sc = new Scanner(System.in);
					continue;
				}

				if (printOn) System.out.println();

				//check if the move is valid
				directions = new ArrayList<>();
				for (int i = 0; i <= 7; i++) directions.add(i);
				boolean movePossible = checkIfMoveIsPossible(posToSetKeystone, directions, map);
				if (!movePossible) {
					System.err.println("Move isn't possible");
				}
				else {
					moveIsPossible = true;
				}
			}

			//check if player set on a bonus or choice field
			moveIsPossible = false;

			//check where player would set their keystone on and act accordingly
			fieldValue = map.getCharAt(posToSetKeystone.x, posToSetKeystone.y);

			while (!moveIsPossible) {
				switch (fieldValue) {
					case 'b': {
						if (printOn) System.out.println("Do you want a bomb (b) or an overwrite-stone (o)?");
						choiceChar = sc.next().charAt(0);
						//convert
						if (choiceChar == 'b') {
							additionalInfo = 20;
							moveIsPossible = true;
						}
						else if (choiceChar == 'o') {
							additionalInfo = 21;
							moveIsPossible = true;
						}

						break;
					}
					case 'c': {
						if (printOn) System.out.println("Mit wem wollen sie die Farbe tauschen ?");
						additionalInfo = sc.nextInt();
						//check
						if (additionalInfo >= 1 && additionalInfo <= map.getAnzPlayers()) moveIsPossible = true;
						break;
					}
					default:
						moveIsPossible = true;
				}
				if (!moveIsPossible){
					if (printOn) System.out.println("Invalid Input");
				}
			}

			positionAndInfo[0] = posToSetKeystone.x;
			positionAndInfo[1] = posToSetKeystone.y;
			positionAndInfo[2] = additionalInfo;

			if (printOn) System.out.println("Set Keystone at: (" + positionAndInfo[0] + "," + positionAndInfo[1] + "," + positionAndInfo[2] + ")");
		}

		//send message where to move
		serverM.sendMove(positionAndInfo[0], positionAndInfo[1], positionAndInfo[2], myPlayerNr);
	}

	/**
	 * Method to update the Map according to the move that was sent to the client
	 */
	private static void updateMapWithMove(Position posToSetKeystone, int additionalInfo, int moveOfPlayer, Map map, boolean printOn) {
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
                    if (printOn) System.out.println("Overwrite Move");
					map.decreaseOverrideStonesOfPlayer();
				}
				//inversion move
				else if (fieldValue == 'i') {
                    if (printOn) System.out.println("Inversion Move");
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
                if (printOn) System.out.println("Choice Move");
				map.swapStonesWithOnePlayer(additionalInfo);
				break;
			case 20: //bonus and want a bomb
                if (printOn) System.out.println("Bonus Move - Bomb");
				map.increaseBombsOfPlayer();
				break;
			case 21: //bonus and want an overwrite-stone
                if (printOn) System.out.println("Bonus Move - Overwrite Stone");
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
		int[] positionAndInfo;
		ArrayList<int[]> validMoves;
		boolean moveIsPossible = false;
		Position posToSetKeystone = new Position(0, 0);
		Scanner sc = new Scanner(System.in);

		boolean pickARandom = true;

		validMoves = getPositionsToSetABomb(map);

		//print valid moves
		if (printOn) System.out.println(map.toString(validMoves, false, true));
		//calculate value of map and print it
		double valueOfMap = (double)Math.round(heuristik.evaluate()*100)/100;
		if (printOn) System.out.println("Value of Map is " + valueOfMap);

        //get a move
        if (calculateMove){
            if (!pickARandom) {
                positionAndInfo = getNextMoveDFS(validMoves, false, depth);
                posToSetKeystone = new Position(positionAndInfo[0], positionAndInfo[1]);
                if (printOn) System.out.println("Set Keystone at: " + posToSetKeystone);
            }
            else {
                int[] posAndInfo = validMoves.get((int)Math.floor( Math.random() * (validMoves.size()-1) ));
                posToSetKeystone = new Position(posAndInfo[0], posAndInfo[1]);
            }
        }
        //let player pick a move
        else {
            //change valid moves to a position list to use contains
            ArrayList<Position> possibleMoves = new ArrayList<>();
            for (int[] posAndInfo : validMoves) {
                possibleMoves.add(new Position(posAndInfo[0], posAndInfo[1]));
            }

            while (!moveIsPossible) {


                //enter the move
                if (printOn) System.out.print("Enter the next move (x,y): ");

                posToSetKeystone.x = sc.nextInt();
                posToSetKeystone.y = sc.nextInt();

                if (printOn) System.out.println();

                //check if the move is valid
                moveIsPossible = possibleMoves.contains(posToSetKeystone);
            }
        }

		//send the move
		serverM.sendMove(posToSetKeystone.x, posToSetKeystone.y, 0, myPlayerNr);
	}

	private static ArrayList<int[]> getPositionsToSetABomb(Map map) {
		ArrayList<int[]> validMoves = new ArrayList<>();
		char fieldValue;
		//gets the possible positions to set a bomb at
		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){
				fieldValue = map.getCharAt(x, y);
				if (fieldValue != '-' && fieldValue != 't'){
					validMoves.add(new int[]{x,y});
				}
			}
		}
		return validMoves;
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
	//depth 1
	private int[] getNextMoveWithHeuristik(ArrayList<Position> validMoves, boolean phaseOne){
		ArrayList<int[]> everyPossibleMove;
		ArrayList<Double> valueOfMap = new ArrayList<>();
		Map nextMap;
		int indexOfHighest;
		double evaluation;

		everyPossibleMove = getEveryPossibleMove(map, validMoves);

		for (int[] positionAndInfo : everyPossibleMove) {
			//clones Map
			nextMap = new Map(map);
			nextMap.setPlayer(myPlayerNr);

			//if it's the first phase
			if (phaseOne) {
				updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], myPlayerNr, nextMap, false);
			}
			//if it's the bomb phase
			else {
				updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap);
			}

			heuristikForSimulation.updateMap(nextMap);
			evaluation = heuristikForSimulation.evaluate();

			valueOfMap.add(evaluation);
		}

		Double highest = Collections.max(valueOfMap);
		indexOfHighest = valueOfMap.indexOf(highest);

		return everyPossibleMove.get(indexOfHighest); //returns the position and the additional info of the move that has the highest evaluation
	}

	//recursive DFS
	private int[] getNextMoveDFS(ArrayList<int[]> everyPossibleMove, boolean phaseOne, int depth){
		ArrayList<Double> valueOfMap = new ArrayList<>();
		Map nextMap;
		int indexOfHighest;
		double evaluation;

        if (everyPossibleMove.isEmpty()){
            System.err.println("Something is wrong - There is no move to check");
            return null;
        }

		for (int[] positionAndInfo : everyPossibleMove){
			//clones Map
			nextMap = new Map(map);
			nextMap.setPlayer(myPlayerNr);

			//if it's the first phase
			if (phaseOne) {
				updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], myPlayerNr, nextMap, false);
			}
			//if it's the bomb phase
			else {
				updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap); //also updates currently playing player
			}

			//Call DFS to start building part-tree of children
			if (depth > 1) {
				evaluation = DFSVisit(nextMap,depth-1, phaseOne, false);
			}
			else {
				heuristikForSimulation.updateMap(nextMap);
				evaluation = heuristikForSimulation.evaluate();
			}
			valueOfMap.add(evaluation);
		}

		//get the highest value for a Map evaluation and the index of it
		Double highest = Collections.max(valueOfMap);
		indexOfHighest = valueOfMap.indexOf(highest);

		if (printOn) {
			int[] posAndInfo;
			System.out.println("Found " + valueOfMap.size() + " valid Moves");
			System.out.print("DFS-N("+depth+"): ");
			for (int i = 0; i < valueOfMap.size(); i++){
				posAndInfo = everyPossibleMove.get(i);
				if (phaseOne) System.out.printf("[(%2d,%2d,%2d)=%3d], ",posAndInfo[0],posAndInfo[1],posAndInfo[2],Math.round(valueOfMap.get(i)*100)/100);
				else System.out.printf("[(%2d,%2d)=%3d], ",posAndInfo[0],posAndInfo[1],Math.round(valueOfMap.get(i)*100)/100);
			}
			System.out.println();
			System.out.println("returning: " + highest);
		}

		return everyPossibleMove.get(indexOfHighest); //returns the position and the additional info of the move that has the highest evaluation

	}

	private double DFSVisit(Map map, int depth, boolean phaseOne, boolean printOn){
		ArrayList<int[]> everyPossibleMove;
		ArrayList<Double> valueOfMap = new ArrayList<>();
		Map nextMap;
		int skippedPlayers = 0;
		double evaluation;
		Double highestOrLowest;

		//checks if players can make a move
		while (true) {
			//get valid moves depending on stage of game
			if (phaseOne) {
                everyPossibleMove = getValidMoves(map);
			}
			else {
				everyPossibleMove = getPositionsToSetABomb(map);
			}

			if (!everyPossibleMove.isEmpty()) {
				break;
			}

			map.nextPlayer();
			skippedPlayers++;
			if (skippedPlayers == map.getAnzPlayers()-1){
				if (phaseOne) {
					phaseOne = false; //end of phase 1
					//continues while but in phase 2
				}
				else {
					heuristikForSimulation.updateMap(map);
					return heuristikForSimulation.placePlayers(); //end of game
				}
			}
		}

		for (int[] positionAndInfo : everyPossibleMove){
			//clones Map
			nextMap = new Map(map);

			//if it's the first phase
			if (phaseOne) {
				updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], nextMap.getCurrentlyPlayingI(), nextMap, false);
			}
			//if it's the bomb phase
			else {
				updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap);
			}

			//Call DFS to start building part-tree of children
			if (depth > 1) {
				evaluation = DFSVisit(nextMap,depth-1, phaseOne, printOn);
			}
			else {
				heuristikForSimulation.updateMap(nextMap);
				evaluation = heuristikForSimulation.evaluate();
			}
			valueOfMap.add(evaluation);
		}

		if (map.getCurrentlyPlayingI() == myPlayerNr) {
			//get the highest value for a Map evaluation and the index of it
			highestOrLowest = Collections.max(valueOfMap);
		}
		else {
			highestOrLowest = Collections.min(valueOfMap);
		}

		if (printOn) {
			int[] posAndInfo;
			System.out.print("DFS-V("+depth+"): ");
			for (int i = 0; i < valueOfMap.size(); i++){
				posAndInfo = everyPossibleMove.get(i);
				if (phaseOne) System.out.printf("[(%2d,%2d,%2d)= %3d], ",posAndInfo[0],posAndInfo[1],posAndInfo[2],Math.round(valueOfMap.get(i)*100)/100);
				else System.out.printf("[(%2d,%2d)= %3d], ",posAndInfo[0],posAndInfo[1],Math.round(valueOfMap.get(i)*100)/100);
			}
			System.out.println("returning: " + highestOrLowest);
			if (depth > 1) System.out.println();
		}

		return highestOrLowest;
	}

	//functions to calculate possible moves

    //function to call
	/**
	 * returns the possible moves on the current map
	 * @param map map to check for possible moves
	 * @return returns an Array List of Positions
	 */
	public static ArrayList<int[]> getValidMoves(Map map) {
		Moves moves = new Moves();
        ArrayList<int[]> everyPossibleMove;
        final boolean useNeighbours = false;

		if (useNeighbours) {
            getCandidatesByNeighbour(map, moves);
            deleteNotPossibleMoves(map, moves);
            everyPossibleMove = getEveryPossibleMove(map, moves.possibleMoves);
        }
        else {
            everyPossibleMove = getFieldsByOwnColor(map);
        }
		
		return everyPossibleMove;
	}

    //  by Neighbour
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

    private static ArrayList<int[]> getEveryPossibleMove(Map map, ArrayList<Position> validMoves){
        ArrayList<int[]> everyPossibleMove = new ArrayList<>(validMoves.size());
        char charAtPos;

        for (Position pos : validMoves) {
            charAtPos = map.getCharAt(pos);

            //choice
            if (charAtPos == 'c') {
                for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++) {
                    everyPossibleMove.add(new int[]{pos.x, pos.y, playerNr});
                }
            }
            else {
                //bonus
                if (charAtPos == 'b'){
                    everyPossibleMove.add(new int[]{pos.x, pos.y, 20});
                    everyPossibleMove.add(new int[]{pos.x, pos.y, 21});
                }
                //normal
                else {
                    everyPossibleMove.add(new int[]{pos.x, pos.y, 0});
                }
            }
        }
        return everyPossibleMove;
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
				if(currPos.equals(StartingPos)) break; //TODO unnötig
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

    //  by own color
    private static ArrayList<int[]> getFieldsByOwnColor(Map map){
        HashSet<PositionAndInfo> everyPossibleMove = new HashSet<>();
        ArrayList<Position> movesToCheck = new ArrayList<>(map.getStonesOfPlayer(map.getCurrentlyPlayingI())); //adds all the stone positions of the player to the moves to check
        ArrayList<int[]> resultPosAndInfo = new ArrayList<>();
		int r;
        Integer newR;
        Position currPos;
        char currChar;

        //add x fields
        if (map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
            for (Position pos : map.getExpansionFields()) {
                everyPossibleMove.add(new PositionAndInfo(pos.x, pos.y, 0));
            }
        }

        //check if move is possible
        for (Position pos : movesToCheck){
            for (r = 0; r <= 7; r++){
                newR = r;
                currPos = pos.clone();

                //do the first step
                newR = doAStep(currPos, newR, map);
                if (newR == null) continue; //if the step wasn't possible
                currChar = map.getCharAt(currPos); //check what's there
                if (currChar == 'c' || currChar == 'b' || currChar == 'i' || currChar == '0' || currChar == map.getCurrentlyPlayingC()) continue; //if it's c, b, i, 0, myColor

                while (true){
                    newR = doAStep(currPos, newR, map);
                    if (newR == null) break; //if the step wasn't possible

                    //check what's there
                    currChar = map.getCharAt(currPos);

                    //player or 0
                    if (Character.isDigit(currChar)){
                        // 0
                        if (currChar == '0') {
                            everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 0));
                            break;
                        }
                        //player
                        else {
                            //player stone - overwrite move
                            if (map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
                                everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 0));
								//if it's an own stone don't go on
								if (currChar == map.getCurrentlyPlayingC()) break;
                            }
                        }
                    }
                    // c, b, i, x
                    else {
                        if (currChar == 'i') {
                            everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 0));
                            break;
                        }
                        if (currChar == 'c') {
                            for (int playerNr = 1; playerNr < map.getAnzPlayers(); playerNr++){
                                everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, playerNr));
                            }
                            break;
                        }
                        if (currChar == 'b'){
                            everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 20));
                            everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 21));
                            break;
                        }
                        if (currChar == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) {
                            everyPossibleMove.add(new PositionAndInfo(currPos.x, currPos.y, 0));
                        }
                    }
                }
            }
        }

		for (PositionAndInfo pai : everyPossibleMove){
			resultPosAndInfo.add(pai.toIntArray());
		}

        return resultPosAndInfo;
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
