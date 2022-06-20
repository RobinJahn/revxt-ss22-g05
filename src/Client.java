package src;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Client{
	//final variables
	final private boolean calculateMove = true;


	final private boolean printOn;
	final private boolean extendedPrint;
	final private boolean useColors;
	final private boolean serverLog = true;
	final private boolean compare_to_Server;

	final private boolean useAB;
	final private boolean useMS;
	final private boolean useBRS = true;
	final private boolean useKH = true;
	final private boolean useMCTS;

	private boolean timed = true;

	//global variables
	private Map map;
	private ServerMessenger serverM;
	private Heuristic heuristic;
	private Heuristic heuristicForSimulation;
	private int myPlayerNr;
	private int depth;
	private int time;
	private int moveCounter;
	double approximation = 1;
	SearchTree searchTree;



	Random random = new Random(1);


	public static void main(String[] args) {
		boolean printOn = false;
		boolean useColors = true;
		boolean extendedPrint = false;
		boolean compare_to_Server = false;


		boolean useAB = true;
		boolean useMS = true;
		boolean useMCTS = false;

		//variables for the server
		String ip = "127.0.0.1";
		int port = 7777;
		int groupNumberAddition = -1;
		//variables for the heuristic
		final int countOfMultipliers = Heuristic.countOfMultipliers;
		final int countOfPhases = 3;
		double[][] multipliers = null;

		//get call arguments
		for (int i = 0; i < args.length; i++){
			switch (args[i])
			{
				case "--ip":
				case "-i": if(i<args.length -1) i++; ip = args[i]; break;

				case "--port":
				case "-p": if(i<args.length -1) i++; port = Integer.parseInt(args[i]); break;

				case "--quiet":
				case "-q": printOn = false; extendedPrint = false; break;

				case "--output":
				case "-o": printOn = true; break;

				case "--extended-print":
				case "-ep": extendedPrint = true; printOn = true; break;

				case "--disable-colour":
				case "-C": useColors = false; break;

				case "--compare-to-server":
				case "-cts": compare_to_Server = true; break;

				case "--disable-alpha-beta":
				case "-AB": useAB = false; break;

				case "--disable-sorting":
				case "-ds": useMS = false; break;

				case "--use-arrows":
				case "-ua": Map.useArrows = true; break;

				case "--activate-mcts":
				case "-mcts": useMCTS = true; break;

				case "--group-number-addition":
				case "-gna":
					if (i < args.length -1) i++;
					try {
						groupNumberAddition = Integer.parseInt(args[i]);
						break;
					}
					catch (NumberFormatException nfe){
						System.err.println("Number for group number addition couldn't be parsed");
						nfe.printStackTrace();
						printHelp();
						return;
					}


				case "--multiplier":
				case "-m":
					if (multipliers == null) multipliers = new double[countOfPhases][countOfMultipliers];
					int indexOfMultiplier = 0;
					int phase;
					if (i < args.length -1) i++;

					//read in n multipliers
					try {
						phase = Integer.parseInt(args[i]) - 1; //-1 because it's an index
						i++;

						while (i < args.length && indexOfMultiplier < countOfMultipliers) {
							multipliers[phase][indexOfMultiplier] = Double.parseDouble(args[i]);
							i++;
							indexOfMultiplier++;
						}
						i--; //reset the last i++
						break;
					}
					catch (NumberFormatException nfe){
						nfe.printStackTrace();
						printHelp();
					}
					break;

				default: System.out.print(args[i] + " is not an option\n");
				case "--help":
				case "-h":
					printHelp();
					return;
			}
		}

		//run client
		new Client(ip, port, multipliers, useAB, printOn, useColors, compare_to_Server, extendedPrint, useMS, useMCTS, groupNumberAddition);
	}

	private static void printHelp(){
		StringBuilder helpString = new StringBuilder();
		helpString.append("java -jar client05.jar accepts the following optional options:\n");
		helpString.append("-i or --ip <IP Address>\t\t\t\tApplies this IP\n");
		helpString.append("-p or --port <Port Number>\t\t\tApplies this Port Number\n");
		helpString.append("-q or --quiet\t\t\t\t\t\tDisables Console Output\n");
		helpString.append("-o or --output\t\t\t\t\t\tActivates output\n");
		helpString.append("-ep or --extended-print\t\t\t\tEnables Print and Extended Print\n");
		helpString.append("-C or --disable-colour\t\t\t\tDisables Coloured Output for the IntelliJ-IDE\n");
		helpString.append("-cts or --compare-to-server\t\t\tEnables the Output for Map Comparison with the Server\n");
		helpString.append("-AB or --disable-alpha-beta\t\t\tDisables alpha beta pruning\n");
		helpString.append("-ds or --disable-sorting\t\t\tDisables move sorting\n");
		helpString.append("-ua or --use-arrows\t\t\t\t\tEnables Arrows\n");
		helpString.append("-mcts or --activate-mcts\t\t\t\t\tEnables Monte carlo Tree search\n");
		helpString.append("-gna or --group-number-addition\t\tchanges the group number to 50 + the given number\n");

		helpString.append("-m or --multiplier <phase number> <");
		for (int i = 1; i <= Heuristic.countOfMultipliers; i++) helpString.append("m").append(i).append(" ");
		helpString.append(">\n");
		helpString.append("\t\t\t\t\t\t\t\t\t Sets the values given as multipliers for the Heuristic (m1 = stone count, m2 = move count, m3 = field Value, m4 = edge multiplier, m5 = wave count)\n");
		helpString.append("\t\t\t\t\t\t\t\t\t Special cases:\n");
		helpString.append("\t\t\t\t\t\t\t\t\t\tIf field Value and edge Multiplier arnt set wave count -> 0\n");
		helpString.append("\t\t\t\t\t\t\t\t\t\tAn Edge Multiplier of 1 makes no sense\n");

		helpString.append("-h or --help\t\t\t\t\t\tshow this blob\n");

		System.out.println(helpString);
	}

	//functions that let the client play

	/**
	 * Constructor of the Client.
	 * Connects to Server and starts playing
	 * @param ip ip of the server
	 * @param port port of the server
	 */
	public Client(String ip,
				  int port,
				  double[][] multipliers,
				  boolean useAB,
				  boolean printOn,
				  boolean useColors,
				  boolean compare_to_Server,
				  boolean extendedPrint,
				  boolean useMS,
				  boolean useMCTS,
				  int groupNumberAddition)
	{
		this.printOn = printOn;
		this.useColors = useColors;
		this.compare_to_Server = compare_to_Server;
		this.useAB = useAB;
		this.extendedPrint = extendedPrint;
		this.useMS = useMS;
		this.useMCTS = useMCTS;

		//try to connect with server
		try {
			serverM = new ServerMessenger(ip,port, groupNumberAddition);
			if (printOn) System.out.println("Client Successfully connected to server");
		} catch (Exception e) {
			System.err.println("Couldn't connect to server");
			return;
		}

		//print Multipliers
		if ((printOn || serverLog) && multipliers != null) {
			System.out.println(Arrays.deepToString(multipliers).replace("],","],\n"));
			System.out.println();
		}

		//get map from server
		//global variables
		StaticMap sMap = serverM.getMap(serverLog);

		//check if it imported correctly
		if(sMap == null || !sMap.wasImportedCorrectly()) {
			System.err.println("Map couldn't be loaded correctly");
			return;
		}
		else {
			if (printOn) System.out.println("Map was loaded correctly");
		}

		map = new Map(sMap);

		//get own player number
		myPlayerNr = serverM.getPlayerNumber();
		if(printOn) System.out.println("Own Player Number is: " + myPlayerNr);

		//set variables after map was imported
		heuristic = new Heuristic(map, myPlayerNr, printOn, extendedPrint, multipliers); // mark
		heuristicForSimulation = new Heuristic(map, myPlayerNr,false,false,multipliers);
		searchTree = new SearchTree(map, printOn, serverLog, extendedPrint, myPlayerNr, useAB, useMS, useBRS, useKH, useMCTS, multipliers);

		//Staging Preparations

		if (printOn) {
			if (extendedPrint) map.printReachableFields();
			System.out.println("Count of reachable Fields: " + map.getCountOfReachableFields());
			System.out.println("Fill Percentage: " + String.format("%.2f",map.getFillPercentage()*100) + "%");
			System.out.println();
		}
		//start playing
		play();
	}


	/**
	 * Plays the Game. Get Messages from server and calls methods to handle the different kinds
	 */
	private void play()
	{
		int messageType;
		boolean gameOngoing = true;
		boolean firstPhase = true;
		boolean firstMove = true;
		int[] timeAndDepth;
		int[] randomPos;
		moveCounter = 0;
		long timeOffset;
		long upperTimeLimit;
		long startTime;
		ClientServerComparator CSC = null;
		if (compare_to_Server) CSC = new ClientServerComparator(map.getWidth());

		int countOfOwnMoves = 0;

		if (extendedPrint) System.out.println(map.toString(null,true,useColors));

		while (gameOngoing) {

			if (printOn) System.out.print("\nWaiting for Message - ");
			messageType = serverM.waitForMessage();

			switch (messageType) {

				case 4: //Move Request
				{
					//Start Timer
					startTime = System.nanoTime();

					if (printOn) {
						System.out.println("received Move Request");
						System.out.println("Move: " + moveCounter);
					}

					//read rest of move request
					timeAndDepth = serverM.readRestOfMoveRequest();
					time = timeAndDepth[0];
					depth = timeAndDepth[1];

					if (time == -1 || depth == -1) {
						System.err.println("Time and Depth couldn't be read");
						gameOngoing = false;
						break;
					}

					//random move at first but prevents disqualifying
					if (firstMove) {
						randomPos = map.getRandomMove();
						if (printOn || serverLog) System.out.println("Made random first Move");
						serverM.sendMove(randomPos[0], randomPos[1], 0, myPlayerNr);
						firstMove = false;
						continue;
					}

					//set timed
					timed = time != 0;
					timeOffset = 500_000_000;// xxx_000_000 ns -> xxx ms
					upperTimeLimit = startTime + time * (long) 1_000_000 - timeOffset;

					if (timed && printOn) System.out.println("We have: " + time + "ms");

					if (depth == 0) depth = Integer.MAX_VALUE;

					//Staging
					if(printOn) System.out.println("Fill Percentage: " + String.format("%.2f",map.getFillPercentage()*100) + "%");

					//Handle Move Request - Both functions print the map with the possible moves marked
					if (firstPhase) {
						makeAMove(upperTimeLimit);
					} else {
						setABomb(upperTimeLimit);
					}
					countOfOwnMoves++;
					break;
				}

				case 6: //Move
				{
					if (printOn) {
						System.out.println("received Move");
 						System.out.println("Move: " + moveCounter);
					}

					moveCounter++;

					//read rest of Message
					int[] moveInfos = serverM.readRestOfMove();
					Position posToSetKeystone = new Position(0, 0);
					posToSetKeystone.x = moveInfos[0] + 1; //index shift
					posToSetKeystone.y = moveInfos[1] + 1; //index shift
					int additionalInfo = moveInfos[2];
					int moveOfPlayer = moveInfos[3];
					if (printOn) System.out.println("Player " + moveOfPlayer + " set keystone to " + posToSetKeystone + ". Additional: " + additionalInfo);

					if (compare_to_Server) {
						ArrayList<int[]> validMoves = null;
						ArrayList<Position> differentPositions;
						map.setPlayer(moveOfPlayer);
						if(firstPhase){
							try {
								validMoves = Map.getValidMoves(map, false, false, false, 0, null);
								CSC.setClientString(map.toString_Server(validMoves));
							}
							catch (ExceptionWithMove EWM) {
								EWM.printStackTrace();
							}
						}
						else {
							CSC.setClientString(map.toString_Server(null));
						}
						differentPositions = CSC.compare(moveCounter-1);
						System.out.println("Server and Client found the same moves: " + differentPositions.isEmpty());

						if (!differentPositions.isEmpty()) {
							System.out.println("received Move");
							System.out.println("Move: " + moveCounter);
							System.out.println("Player " + moveOfPlayer + " set keystone to " + posToSetKeystone + ". Additional: " + additionalInfo);

							System.out.println("Different Positions:");
							System.out.println(Arrays.toString(differentPositions.toArray()));

							System.out.println("Map of Client colored:");
							System.out.println(map.toString(validMoves, false, differentPositions));

							System.out.println("Map of Client normal:");
							System.out.println(map.toString(validMoves, false, true));

							System.out.println("Map of Server:");
							System.out.println(Map.toString(CSC.getMapFromServer(moveCounter-1), true));

							return;
						}
					}

					//Handle Move
					if (firstPhase) Map.updateMapWithMove(posToSetKeystone, additionalInfo, moveOfPlayer, map, printOn);
					else Map.updateMapAfterBombingBFS(posToSetKeystone.x, posToSetKeystone.y, moveOfPlayer, map);

					if (printOn) {
						if (firstPhase && Map.useArrows) {
							ArrayList<int[]> validMovesByArrows;
							ArrayList<int[]> validMovesByOwnColor = null;
							//calculate possible moves
							validMovesByArrows = map.getValidMovesByArrows(firstPhase, heuristic);
							try {
								validMovesByOwnColor = Map.getFieldsByOwnColor(map, timed, printOn, serverLog, Long.MAX_VALUE, heuristic);
							}
							catch (TimeoutException e){
								System.err.println("Something went wrong - timeout exception was thrown even if no time limit was set");
							}

							//prints map
							if (!compareValidMoves(firstPhase, validMovesByOwnColor)) {
								System.out.println("With own color");
								System.out.println(map.toString(validMovesByOwnColor, false, useColors));
								System.out.println("With move carry along");
								System.out.println(map.toString(map.getValidMovesByArrows(firstPhase, heuristic), false, useColors));

								//check if arrows are correct
								System.out.println("Reference in Affected Arrows: " + map.checkForReferenceInAffectedArrows());
								System.out.println("All valid Moves are correct: " + map.checkValidMoves());
								System.out.println("All overwrite Moves are correct: " + map.checkOverwriteMoves());
								System.out.println();

								return;
							} else {
								System.out.println(map.toString(validMovesByArrows, false, useColors));

								//check if arrows are correct
								System.out.println("Reference in Affected Arrows: " + map.checkForReferenceInAffectedArrows());
								System.out.println("All valid Moves are correct: " + map.checkValidMoves());
								System.out.println("All overwrite Moves are correct: " + map.checkOverwriteMoves());
								System.out.println();
							}

						}
						else {
							ArrayList<int[]> validMoves;
							validMoves = null;
							try {
								validMoves = Map.getValidMoves(map, timed, printOn, serverLog, Long.MAX_VALUE, heuristic);
							}
							catch (TimeoutException e){
								System.err.println("Something went wrong - timeout exception was thrown even if no time limit was set");
							}
							System.out.println(map.toString(validMoves, false, useColors));
						}

						double valueOfMap;

						//calculate value of map and print it
						try
						{
							 valueOfMap = (double) Math.round(heuristic.evaluate(firstPhase,timed, serverLog, Long.MAX_VALUE) * 100) / 100;
						}
						catch (TimeoutException TE)
						{
							System.out.println("TimeoutException in Handle Move");
							return;
						}
						System.out.println("Value of Map is " + valueOfMap);
					}
					break;
				}

				case 7: //Disqualification
				{
					if (printOn || serverLog) System.out.println("received Disqualification");
					int player = serverM.readRestOfDisqualification();
					map.disqualifyPlayer(player);
					if (printOn || serverLog) System.out.println("Player " + player + " was disqualified");
					if (player == myPlayerNr) gameOngoing = false;
					break;
				}

				case 8: //End of Phase 1
				{
					serverM.readRestOfNextPhase();
					firstPhase = false;
					approximation = 1;
					if (printOn || serverLog) {
						System.out.println("received end of phase 1");
						System.out.println("reset approximation");
					}
					break;
				}

				case 9: //End of Phase 2
				{
					if (printOn || serverLog) System.out.println("received end of phase 2 - game ended");

					serverM.readRestOfNextPhase();
					gameOngoing = false;

					if(compare_to_Server && CSC != null)
					{
						try {
							FileWriter FW = new FileWriter("ErrorCounts.txt", true);
							BufferedWriter BW = new BufferedWriter(FW);
							String toAdd = "ErrorCount: " + CSC.getErrorCount() + "\n";
							BW.write(toAdd);
							BW.close();
							FW.close();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}

					if (printOn || serverLog) {
						System.out.println("Average Depth hit: " + (searchTree.getTotalDepth()/(double) countOfOwnMoves) );
					}

					break;
				}

				case -1: {
					gameOngoing = false;
					System.err.println("Server closed connection or a message was received that couldn't be handled");
					break;
				}
			}
		}
	}

	//phase 1
	/**
	 * Method to make a move after a move request was sent to the Client
	 */
	private void makeAMove(long upperTimeLimit){
		//calculated Move
		int[] positionAndInfo;
		int[] validPosition = new int[]{-1,-1,-1};
		int[] validPosition1;
		final boolean phaseOne = true;

		//general
		double valueOfMap;
		ArrayList<int[]> validMoves;
		ArrayList<int[]> validMovesByOwnColor;

		map.setPlayer(myPlayerNr);
		//calculate possible moves
		if (printOn) System.out.println("Get Valid Moves in make a move");
		try {
			validMoves = Map.getValidMoves(map, timed, printOn, serverLog, upperTimeLimit, heuristic);
		} catch (ExceptionWithMove e) {
			validPosition = e.PosAndInfo;
			//send message where to move
			serverM.sendMove(validPosition[0], validPosition[1], validPosition[2], myPlayerNr);
			return;
		}


		//calculate value of map and print it

		if (printOn){
			try {
				valueOfMap = (double)Math.round(heuristic.evaluate(phaseOne,timed, serverLog, upperTimeLimit)*100)/100;
			}
			catch (TimeoutException TE) {
				System.out.println("TimeOutException in MakeAMove");
				return;
			}
			System.out.println("Value of Map is " + valueOfMap);
		}
		//check for error
		if (validMoves.isEmpty()) {
			System.err.println("Something's wrong - Valid Moves are empty but server says they're not");
			return;
		}

		//make a calculated move
		if (calculateMove) {
			validPosition = searchTree.getMove(map, timed, depth, phaseOne, validMoves, upperTimeLimit, moveCounter);
			//validPosition = getMoveTimeDepth(phaseOne, validMoves);
			//validPosition = validMoves.get( random.nextInt(validMoves.size()) );
		}
		//let player enter a move
		else {
			//Variables needed for human player
			Statistic statistic = new Statistic(0);
			boolean moveIsPossible = false;
			double[] valueAndIndex;
			char fieldValue;
			int additionalInfo = 0;
			Position posToSetKeystone = new Position(0, 0);
			Scanner sc = new Scanner(System.in);
			ArrayList<Integer> directions;
			char choiceChar;
			KillerArray KillerArray = new KillerArray();
			//print moves and evaluation
			try {
				valueAndIndex = getNextMoveDFS(validMoves, true, depth, statistic, -1,KillerArray);
			}
			catch (TimeoutException te){
				valueAndIndex  = new double[]{0, -1};
				if (printOn) {
					System.out.println("Timeout Exception thrown");
					te.printStackTrace();
				}
			}
			if (valueAndIndex[1] != -1) {
				positionAndInfo = validMoves.get( (int)valueAndIndex[1] );
				if (printOn) System.out.println("Recommended Move: (" + positionAndInfo[0] + "," + positionAndInfo[1] + "," + positionAndInfo[2] + ")");
			}
			else {
				System.err.println("Something went wrong. Didn't get a value back");
			}


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
				boolean movePossible = Map.checkIfMoveIsPossible(posToSetKeystone, directions, map);
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
						if (printOn) System.out.println("With whom do you want to swap colors ?");
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

			validPosition[0] = posToSetKeystone.x;
			validPosition[1] = posToSetKeystone.y;
			validPosition[2] = additionalInfo;

			if (printOn) System.out.println("Set Keystone at: (" + validPosition[0] + "," + validPosition[1] + "," + validPosition[2] + ")");
		}

		//send message where to move
		serverM.sendMove(validPosition[0], validPosition[1], validPosition[2], myPlayerNr);
	}

	//phase 2 - bomb phase
	private void setABomb(long upperTimeLimit){
		int[] validPosition;
		ArrayList<int[]> validMoves;
		boolean moveIsPossible = false;
		Position posToSetKeystone = new Position(0, 0);
		final boolean phaseOne = false;

		final boolean pickARandom = false;

		map.setPlayer(myPlayerNr);
		validMoves = Map.getPositionsToSetABomb(map);

		if (validMoves.isEmpty()) {
			System.err.println("Something's wrong - Positions to set a Bomb are empty but server says they're not");
			return;
		}

        //get a move
        if (calculateMove)
		{
            if (!pickARandom)
			{
				validPosition = searchTree.getMove(map, timed, depth, phaseOne, validMoves, upperTimeLimit, moveCounter);
				//validPosition = getMoveTimeDepth(phaseOne, validMoves);

				posToSetKeystone = new Position(validPosition[0], validPosition[1]);
            }
            else
			{
                int[] posAndInfo = validMoves.get((int)Math.round( Math.random() * (validMoves.size()-1) ));
                posToSetKeystone = new Position(posAndInfo[0], posAndInfo[1]);
            }
        }
        //let player pick a move
        else {
			Scanner sc = new Scanner(System.in);

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
				//moveIsPossible = true;
            }
			sc.close();
        }

		if (printOn) System.out.println("Set Keystone at: " + posToSetKeystone);

		//send the move
		serverM.sendMove(posToSetKeystone.x, posToSetKeystone.y, 0, myPlayerNr);
	}

	private boolean compareValidMoves(boolean phaseOne, ArrayList<int[]> validMoves) {
		if (!Map.useArrows || validMoves == null) return true;
		boolean contains;
		boolean containsAll = true;
		ArrayList<int[]> validMovesByColorExtra = new ArrayList<>();
		ArrayList<int[]> validMovesByArrowExtra = new ArrayList<>();

		for (int[] posAndR1 : validMoves){
			contains = false;
			for (int[] posAndR2 : map.getValidMovesByArrows(phaseOne, heuristic)){
				if (Arrays.compare(posAndR1, posAndR2) == 0) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				containsAll = false;
				validMovesByColorExtra.add(posAndR1);
			}
		}

		for (int[] posAndR1 : map.getValidMovesByArrows(phaseOne, heuristic)){
			contains = false;
			for (int[] posAndR2 : validMoves){
				if (Arrays.compare(posAndR1, posAndR2) == 0) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				containsAll = false;
				validMovesByArrowExtra.add(posAndR1);
			}
		}


		if (!containsAll){
			System.err.println("Moves of arrows and moves by own color do not match");
			System.out.println("Moves, that only valid Moves by color contain:");
			System.out.println( Arrays.deepToString(validMovesByColorExtra.toArray()) );
			System.out.println("Moves, that only valid Moves by arrows contain:");
			System.out.println( Arrays.deepToString(validMovesByArrowExtra.toArray()) );
			return false;
		}
		return true;
	}

	//Functions to calculate the best next move
	private int[] getMoveTimeDepth(boolean phaseOne, ArrayList<int[]> everyPossibleMove) {
		//declarations
		Statistic statistic;
		double[] valueAndIndex;
		int[] validPosition;
		//Timing
		long startTime = System.nanoTime();
		long timeOffset = 500_000_000; //ns -> xx ms
		long timeNextDepth = 0;
		long upperTimeLimit = startTime + (long)time * 1_000_000 - timeOffset;
		double leavesNextDepth;
		double totalNodesToGoOver;

		int currDepth;

		//Killer Heuristic
		KillerArray KillerArray = new KillerArray();

		//get a random valid position to always have a valid return value
		validPosition = everyPossibleMove.get(0);

		//if there is a time limit
		if (timed) {
			//iterative deepening

			for (currDepth = 1; (upperTimeLimit - System.nanoTime() - timeNextDepth > 0); currDepth++) { //check if we can calculate next layer

				//reset statistic
				statistic = new Statistic(currDepth);

				//print
				if (printOn) System.out.println("DEPTH: " + currDepth);

				//get the best move for this depth
				try {
					valueAndIndex = getNextMoveDFS(everyPossibleMove, phaseOne, currDepth, statistic, upperTimeLimit,KillerArray); //takes time
				}
				//if it noticed we have no more time
				catch (TimeoutException te){
					if (printOn|| serverLog) {
						System.out.println("For Move: " + moveCounter + ", Depth: " + currDepth + ", Move: " + Arrays.toString(validPosition));
						System.out.println("Time out Exception thrown");
						System.out.println("Time Remaining: " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
						te.printStackTrace();
					}
					return validPosition;
				}

				//if we got a valid Position without running out of time - update it
				validPosition = everyPossibleMove.get( (int)valueAndIndex[1] );

				//calculate time needed for next depth
				leavesNextDepth = statistic.leafNodes * statistic.branchFactor();
				totalNodesToGoOver = statistic.totalNodesSeen + leavesNextDepth;

				//time comparison prints
				if (printOn){
					System.out.println("Expected Time needed for this depth: " + timeNextDepth/ 1_000_000 + "ms");
					System.out.println("Actual time needed: " + (double)statistic.totalComputationTime/ 1_000_000 + "ms");
					System.out.println("Approximation: " + approximation);
				}

				//If we know we won or lost -> no need to check deeper
				if (valueAndIndex[0] == Double.NEGATIVE_INFINITY || valueAndIndex[0] == Double.POSITIVE_INFINITY){
					return validPosition;
				}

				//calculate time needed for the next depth
				if (timeNextDepth == 0) {
					timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime());
				}
				else {
					approximation = (approximation + ((double)statistic.totalComputationTime /timeNextDepth) ) / 2;
					timeNextDepth = Math.round(totalNodesToGoOver * statistic.getAverageComputationTime() * approximation);
				}

				//prints after one depth
				if (printOn) {
					//print recommended move
					if (phaseOne) System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + "," + validPosition[2] + ")");
					else System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + ")");

					//print statistic
					System.out.println(statistic);

					//print timing information
					System.out.println("Expected time needed for next depth: " + (double)timeNextDepth/ 1_000_000 + "ms");
					System.out.println("Time Remaining: " + (double)(upperTimeLimit - System.nanoTime()) / 1_000_000 + "ms");
					System.out.println("Expected remaining time after calculating next depth: " + (double)(upperTimeLimit - System.nanoTime() - timeNextDepth)/ 1_000_000 + "ms");
					System.out.println();
				}
			}
		}


		//if we have no time limit
		else {
			currDepth = depth;
			statistic = new Statistic(depth);
			//catch will never happen
			try {
				valueAndIndex = getNextMoveDFS(everyPossibleMove, phaseOne, depth, statistic, 0,KillerArray);
				validPosition = everyPossibleMove.get( (int)valueAndIndex[1] );
			}
			catch (TimeoutException ts){
				if (printOn) System.out.println("Something went wrong - Time out Exception thrown but no time limit was set");
			}
		}

		if(serverLog) {
			System.out.println("For Move: " + moveCounter + ", Depth: " + currDepth + ", Move: " + Arrays.toString(validPosition));
		}

		return validPosition;
	}

	private double[] getNextMoveDFS(ArrayList<int[]> everyPossibleMove, boolean phaseOne, int depth, Statistic statistic, long UpperTimeLimit, KillerArray KillerArray) throws TimeoutException {
		//For DFS
		double[] valueAndIndex;
		//For alpha beta
		double alpha = Double.NEGATIVE_INFINITY;
		double beta = Double.POSITIVE_INFINITY;
		//For Statistics
		long startTime = System.nanoTime(); //start timing
		long totalTime;


		//check if an error occurred
		if (everyPossibleMove.isEmpty()){
            System.err.println("Something is wrong - There is no move to check");
            return null;
        }

		//simulate all moves and return best value
		valueAndIndex = getBestValueAndIndexFromMoves(map, everyPossibleMove, depth, phaseOne, alpha, beta, statistic, UpperTimeLimit,0,KillerArray);

		//end of timing
		totalTime = System.nanoTime() - startTime;
		statistic.totalComputationTime += totalTime;

		return valueAndIndex;
	}

	private double DFSVisit(Map map, int depth, boolean phaseOne, double alpha, double beta, Statistic statistic,long UpperTimeLimit, int brsCount,KillerArray KillerArray) throws TimeoutException{
		//Out of Time ?
		if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
			if (printOn|| serverLog) System.out.println("Out of Time (DFSVisit - start of Method)");
			throw new TimeoutException();
		}

		//declarations
		ArrayList<int[]> everyPossibleMove = new ArrayList<>();
		double currBestValue;
		double currAlpha = alpha;
		double currBeta = beta;
		double[] valueAndIndex;
		int enemyStoneAndOverwriteCount = 0;

		//If we have no stones and no overwrite stones -> LOSS
		if (map.getCountOfStonesOfPlayer(myPlayerNr) == 0 && map.getOverwriteStonesForPlayer(myPlayerNr) == 0){
			if (printOn && extendedPrint) System.out.println("DFSVisit found situation where we got eliminated");
			return Double.NEGATIVE_INFINITY;
		}

		//If we have all stones and no enemy has an OverwriteStone -> WINN
		for (int playerNr = 1; playerNr <= map.getAnzPlayers(); playerNr++){
			if (playerNr == myPlayerNr) continue;
			enemyStoneAndOverwriteCount += map.getCountOfStonesOfPlayer(playerNr);
			enemyStoneAndOverwriteCount += map.getOverwriteStonesForPlayer(playerNr);
		}
		if (enemyStoneAndOverwriteCount == 0) {
			if (printOn && extendedPrint) System.out.println("DFSVisit found situation where we WON");
			return Double.POSITIVE_INFINITY;
		}

		//get moves for the next player
		phaseOne = getMovesForNextPlayer(map, everyPossibleMove, phaseOne,timed,printOn, serverLog, UpperTimeLimit, heuristic);

		//check if it reached the end of the game
		if (everyPossibleMove.isEmpty()) {
			heuristicForSimulation.updateMap(map); //computing-intensive
			return heuristicForSimulation.evaluate(phaseOne,timed, serverLog,UpperTimeLimit); //computing-intensive
		}

		//simulate all moves and return best value
		valueAndIndex = getBestValueAndIndexFromMoves(map, everyPossibleMove, depth, phaseOne, currAlpha, currBeta, statistic,UpperTimeLimit,brsCount,KillerArray);
		currBestValue = valueAndIndex[0];

		return currBestValue;
	}

	private static boolean getMovesForNextPlayer(Map map, ArrayList<int[]> movesToReturn, boolean phaseOne,boolean timed,boolean printOn,boolean ServerLog,long UpperTimeLimit, Heuristic heuristic) throws TimeoutException{
		ArrayList<int[]> everyPossibleMove;
		int skippedPlayers = 0;

		//checks if players can make a move
		while (true) {
			//get valid moves depending on stage of game
			if (phaseOne) { //phase one
				everyPossibleMove = Map.getValidMoves(map,timed,printOn,ServerLog,UpperTimeLimit, heuristic);
			}
			else { //bomb phase
				//if we have bombs
				if (map.getBombsForPlayer(map.getCurrentlyPlayingI()) > 0) everyPossibleMove = Map.getPositionsToSetABomb(map);
				//if not
				else everyPossibleMove = new ArrayList<>(); //empty list
			}

			//if there are possible moves
			if (!everyPossibleMove.isEmpty()) {
				break;
			}

			//if there are no possible moves

			//check if the next player can make a move
			map.nextPlayer();
			skippedPlayers++;

			//if no player can make a move
			if (skippedPlayers >= map.getAnzPlayers()-1){ //shouldn't be greater - just for safety
				//if no player can make a move in phase 1 switch to phase 2
				if (phaseOne) {
					phaseOne = false; //end of phase 1
					skippedPlayers = 0;
					//continues while but in phase 2 -> checks if players can place bombs
				}
				//if no player can make a move in phase 2 the game ends
				else {
					everyPossibleMove = new ArrayList<>(); //empty list
					break;
				}
			}
		}

		//returning all moves by call by reference
		movesToReturn.addAll(everyPossibleMove);

		//returns change of phase
		return phaseOne;
	}

	private double[] getBestValueAndIndexFromMoves(Map map,
												   ArrayList<int[]> everyPossibleMove,
												   int depth,
												   boolean phaseOne,
												   double currAlpha,
												   double currBeta,
												   Statistic statistic,
												   long UpperTimeLimit,
												   int brsCount,
												   KillerArray KillerArray) throws TimeoutException
	{
		//declarations
		Map nextMap;
		boolean isMax;
		double currBestValue;
		double evaluation;
		int indexOfBest = 0;
		int currIndex;
		ArrayList<Map> mapList = new ArrayList<>();
		ArrayList<Integer> indexList = new ArrayList<>(everyPossibleMove.size());
		boolean firstCall = (statistic.timesNodesGotAdded == 0);

		//BRS+
		int PhiZugIndex = -1;

		//Get if we 're a maximizer or a minimizer - set starting values for alpha-beta-pruning
		//	Maximizer
		if (map.getCurrentlyPlayingI() == myPlayerNr) {
			isMax = true;
			currBestValue = Double.NEGATIVE_INFINITY;
			//BRS+Algorithm
			brsCount = 0;
		}
		//	Minimizer
		else {
			isMax = false;
			currBestValue = Double.POSITIVE_INFINITY;
			//PhiZugIndex Random Choice Here we save the Index of our PhiMove
			PhiZugIndex = (int)(Math.random()*(everyPossibleMove.size()-1));

			if(brsCount == 2 && useBRS)
			{
				int[] PhiZug = everyPossibleMove.get(PhiZugIndex);
				everyPossibleMove = new ArrayList<>();
				everyPossibleMove.add(PhiZug);

			}
		}

		//add values to statistic
		statistic.addNodes(everyPossibleMove.size(), depth);

		//prints
		if (firstCall && printOn) {
			System.out.println("Calculating values for " + everyPossibleMove.size() + " Moves");
			System.out.println("Currently at: ");
		}

		//fill index List
		for (int i = 0; i < everyPossibleMove.size(); i++){
			indexList.add(i);
		}

		//sort moves
		//	without arrows
		if (useMS && (!Map.useArrows || !phaseOne)) {
			for (int[] positionAndInfo : everyPossibleMove) {
				//Out of Time ?
				if(timed && (UpperTimeLimit - System.nanoTime() < 0)) {
					if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Move Sorting - start of for)");
					throw new TimeoutException();
				}

				//clones Map
				nextMap = new Map(map, phaseOne);

				//Out of Time ?
				if(timed && (UpperTimeLimit - System.nanoTime() < 0)) {
					if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Move Sorting - after clone)");
					throw new TimeoutException();
				}

				//if it's the first phase
				if (phaseOne) {
					Map.updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], nextMap.getCurrentlyPlayingI(), nextMap, false);
				}
				//if it's the bomb phase
				else {
					Map.updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap.getCurrentlyPlayingI(), nextMap);
				}

				mapList.add(nextMap);
			}

			indexList.sort(new Comparator<Integer>() {
				@Override
				public int compare(Integer i1, Integer i2) {
					/*
					if(i1>= mapList.size()|| i2>= mapList.size())
					{
						System.err.printf("IndexListSize: " + indexList.size() + "\nMapListSize: " + mapList.size());
						System.err.println("\nI1: " + i1 +" \nI2" + i2);
						return 0;
					}
					*/
					Map m1 = mapList.get(i1);
					Map m2 = mapList.get(i2);
					double valueM1 = Heuristic.fastEvaluate(m1, myPlayerNr);
					double valueM2 = Heuristic.fastEvaluate(m2, myPlayerNr);
					if (isMax) return Double.compare(valueM2, valueM1);
					else return Double.compare(valueM1, valueM2);
				}
			});
		}
		//	with arrows
		if (useMS && Map.useArrows && phaseOne){
			//don't know why but this is needed so we can access it inside the Comparator
			ArrayList<int[]> finalEveryPossibleMove = everyPossibleMove;

			indexList.sort(new Comparator<Integer>() {
				@Override
				public int compare(Integer i1, Integer i2) {
					int[] positionAndInfo1 = finalEveryPossibleMove.get(i1);
					int[] positionAndInfo2 = finalEveryPossibleMove.get(i2);
					double valueM1 = Map.getStoneCountAfterMove(map, myPlayerNr, positionAndInfo1);
					double valueM2 = Map.getStoneCountAfterMove(map, myPlayerNr, positionAndInfo2);
					if (isMax) return Double.compare(valueM2, valueM1);
					else return Double.compare(valueM1, valueM2);
				}
			});
		}

		//Out of Time ?
		if(timed && (UpperTimeLimit - System.nanoTime() < 0)) {
			if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Move Sorting - after sort)");
			throw new TimeoutException();
		}

		//Resort Array to include Killer Heuristic
		if(useKH){
			int anzahl = 0;
			for(int i = 0;i< KillerArray.getLength();i++)
			{
				//Out of Time ?
				if(timed && (UpperTimeLimit - System.nanoTime() < 0)) {
					if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - In Killer Heuristic)");
					throw new TimeoutException();
				}

				for (int j = 0;j<everyPossibleMove.size();j++)
				{
					int[] positionAndInfo = everyPossibleMove.get(j);

					//If We found a Move which cuts off we place it in front
					if(Arrays.equals(KillerArray.getPositionAndInfo(i), positionAndInfo))
					{
						if(j < indexList.size()) {
							Integer temp = indexList.get(j);
							indexList.set(j,indexList.get(anzahl));
							indexList.set(anzahl,temp);
							anzahl++;
						}
						if(printOn) System.out.println("Killer Heuristic swapped a move forward");
						break;
					}
				}
			}
		}

		//go over every possible move
		for (int i = 0; i < everyPossibleMove.size(); i++){

			//Out of Time ?
			if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
				if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - in go over moves)");
				throw new TimeoutException();
			}

			//set values
			currIndex = indexList.get(i);
			int[] positionAndInfo = everyPossibleMove.get( currIndex );

			//prints
			if (printOn && firstCall) {
				if (!extendedPrint) System.out.print((i+1) + ", ");
				else System.out.println((i+1) + ", ");
			}


			//With move sorting - get simulated map
			if (useMS && !Map.useArrows) {
				nextMap = mapList.get( currIndex );
			}
			//Without move sorting - simulate move
			else {
				//clones Map
				nextMap = new Map(map, phaseOne);

				//if it's the first phase
				if (phaseOne) {
					Map.updateMapWithMove(new Position(positionAndInfo[0], positionAndInfo[1]), positionAndInfo[2], nextMap.getCurrentlyPlayingI(), nextMap, false);
				}
				//if it's the bomb phase
				else {
					Map.updateMapAfterBombingBFS(positionAndInfo[0], positionAndInfo[1], nextMap.getCurrentlyPlayingI(), nextMap);
				}
			}


			//Call DFS to start building part-tree of children
			if (depth > 1) {
				//BrsCount here
				if (PhiZugIndex != i && PhiZugIndex != -1)
				{
					brsCount++;
				}

				if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
					if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - Before DFS Visit call)");
					throw new TimeoutException();
				}

				evaluation = DFSVisit(nextMap, depth -1, phaseOne, currAlpha, currBeta, statistic,UpperTimeLimit,brsCount,KillerArray);
			}
			//get evaluation of map when it's a leaf
			else {
				heuristicForSimulation.updateMap(nextMap); //computing-intensive
				if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
					if (printOn || serverLog) System.out.println("Out of time (After UpdateMap Before Evaluate)");
					throw new TimeoutException();
				}
				evaluation = heuristicForSimulation.evaluate(phaseOne,timed, serverLog,UpperTimeLimit); //computing-intensive // Here TIME LEAK !!!!!!!
			}

			//Out of Time ?
			if(timed && (UpperTimeLimit - System.nanoTime()<0)) {
				if (printOn|| serverLog) System.out.println("Out of time (getBestValueAndIndexFromMoves - after DFS Visit call)");
				throw new TimeoutException();
			}

			//print infos
			if (extendedPrint){
				System.out.print("DFS-V("+ depth +"): ");
				if (phaseOne) System.out.printf("[(%2d,%2d,%2d)= %.2f], ",positionAndInfo[0],positionAndInfo[1],positionAndInfo[2],evaluation);
				else System.out.printf("[(%2d,%2d)= %.2f], ",positionAndInfo[0],positionAndInfo[1],evaluation);
				System.out.println();
			}

			//Get highest or lowest value
			if (isMax) {
				//get highest value
				if (evaluation > currBestValue) {
					currBestValue = evaluation;
					indexOfBest = currIndex;
				}
			}
			else {
				//get lowest value
				if (evaluation < currBestValue) {
					currBestValue = evaluation;
					indexOfBest = currIndex;
				}
			}

			//Use Alpha Beta Pruning
			if (useAB) {
				//Maximizer
				if (isMax) {
					//update Alpha ?
					if (currBestValue > currAlpha) {
						currAlpha = currBestValue;
						if (extendedPrint) System.out.println("Alpha Updated: " + currAlpha);
					}


					//Cutoff ?
					if (currBestValue >= currBeta) {
						int countOfCutoffSiblings = everyPossibleMove.size() - everyPossibleMove.indexOf(positionAndInfo);
						//delete nodes out of statistic

						//statistic.reduceNodes(countOfCutoffSiblings, depth);
						//Killer Heuristic
						if(useKH) {
							KillerArray.add(new PositionAndInfo(positionAndInfo), countOfCutoffSiblings*(2/(depth+1)));
						}
						//Print before return
						if (extendedPrint) {
							System.out.println("Cutoff: Current highest value (" + currBestValue + ") >= current Beta (" + currBeta + ") - " + countOfCutoffSiblings + " values skipped");
						}
						return new double[]{currBestValue, indexOfBest};
					}
				}
				//Minimizer
				else {
					//update Beta ?
					if (currBestValue < currBeta) {
						currBeta = currBestValue;
						if (extendedPrint) System.out.println("Beta Updated: " + currBeta);
					}

					//Cutoff ?
					if (currBestValue <= currAlpha) {
						int countOfCutoffLeaves = everyPossibleMove.size()- everyPossibleMove.indexOf(positionAndInfo);
						//delete nodes out of statistic
						//statistic.reduceNodes(countOfCutoffLeaves, depth);
						//Killer Heuristic
						if(useKH) {
							KillerArray.add(new PositionAndInfo(positionAndInfo), countOfCutoffLeaves*(2/(depth+1)));
						}
						//Print before return
						if (extendedPrint) {
							System.out.println("Cutoff: Current lowest value (" + currBestValue + ") <= current Alpha (" + currAlpha + ") - " + countOfCutoffLeaves + " values skipped");
						}
						return new double[]{currBestValue, indexOfBest};
					}
				}
			}
		}

		if (printOn && (extendedPrint || firstCall)) {
			System.out.print("DFS-V(" + depth + "): ");
			System.out.println("returning: " + currBestValue);
		}

		return new double[]{currBestValue, indexOfBest};
	}


}
