package src;

import java.io.BufferedWriter;
import java.io.FileWriter;
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

		//default multipliers
		if (multipliers == null) {
			multipliers = new double[][]{
					{3, 2, 9, 5, 2},
					{9, 0, 9, 3, 2},
					{5, 7, 7, 2, 2}
			};
		}

		//set variables after map was imported
		StaticHeuristicPerPhase shpp = new StaticHeuristicPerPhase(map, multipliers, extendedPrint);
		//	the heuristic here is only needed when the output is enabled
		if (printOn) heuristic = new Heuristic(map, myPlayerNr, printOn, extendedPrint, multipliers, shpp);
		searchTree = new SearchTree(map, printOn, serverLog, extendedPrint, myPlayerNr, useAB, useMS, useBRS, useKH, useMCTS, multipliers, shpp);

		//Prints
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
		boolean CheeseMode = isMapCheeseAble();
		if((printOn||serverLog)&&CheeseMode) System.out.println("Activate CheeseMode");
		int[] timeAndDepth;
		int[] randomPos;
		moveCounter = 0;
		long timeOffset;
		long upperTimeLimit;
		long startTime;
		ClientServerComparator CSC = null;
		if (compare_to_Server) CSC = new ClientServerComparator(map.getWidth());

		int countOfOwnMoves = 0;
		int cheeseCounter = 0;
		Position NextPos = new Position(-1,-1);

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

					//set timed
					timed = time != 0;
					timeOffset = 500_000_000; // xxx_000_000 ns -> xxx ms
					upperTimeLimit = startTime + time * (long) 1_000_000 - timeOffset;

					if (timed && printOn) System.out.println("We have: " + time + "ms");

					if (depth == 0) depth = Integer.MAX_VALUE;

					//random move at first if no cheese found but prevents disqualifying
					if (firstMove && timed && !CheeseMode) {
						if(firstPhase)
						{
							randomPos = map.getRandomMove();
						}
						else {
							setABomb(upperTimeLimit);
							firstMove = false;
							continue;
						}

						if (printOn || serverLog) System.out.println("Made random first Move " + Arrays.toString(randomPos));
						serverM.sendMove(randomPos[0], randomPos[1], randomPos[2], myPlayerNr);
						firstMove = false;
						continue;
					}
					//Cheese Move if one is found
					else if(firstMove && cheeseCounter == 0)
					{
						firstCheeseMove(NextPos);
						firstMove = false;
						cheeseCounter++;
						continue;
					}
					//follow Up to Cheese Move
					if(CheeseMode && cheeseCounter == 1)
					{
						CheeseMode = false;
						if(NextPos.x != -1 && map.getCharAt(NextPos) == 'b')
						{
							serverM.sendMove(NextPos.x, NextPos.y,20,myPlayerNr);
							continue;
						}
					}
					//Staging
					if(printOn) System.out.println("Fill Percentage: " + String.format("%.2f",map.getFillPercentage()*100) + "%");

					//set me as the player
					map.setPlayer(myPlayerNr);

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
						System.out.println("Average Depth hit: " + (searchTree.getTotalDepth()/(double) countOfOwnMoves));
						System.out.println("Transpositions Hits: " + searchTree.TT.getTranspositionHits());
						System.out.println("Transpositions Miss: " + searchTree.TT.getTranspositionMiss());
						System.out.println("Transposition Replacements: " + searchTree.TT.getReplacements());
						searchTree.ZH.printZobristTable();
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
	//Cheese Detection
	private boolean isMapCheeseAble()
	{
		if(map.getOverwriteStonesForPlayer(myPlayerNr) >= 1 ) {
			if (map.getCountOfReachableBonusFields() >= 1 ) {
				if (map.getCountOfReachableFields() <= (((map.getExplosionRadius()*2+1)*(map.getExplosionRadius()*2+1))*0.8))
				{
					return true;
				}
			}
		}
		return false;
	}

	//CheeseMode
	private void firstCheeseMove(Position NextPos)
	{
		Position positionToSet = new Position(0,0);
		int maxLength = 0;
		Position newPos;
		Integer newR;
		int length;
		for (Position p : map.getReachableBonusFields())
		{
			for(int r = 0; r < 8; r++)
			{
				newPos = p.clone();
				length = 0;
				newR = r;
				while (newR != null)
				{
					length++;
					if(length > 2 && length > maxLength && (map.getCharAt(newPos)=='x'||map.getCharAt(newPos) == '0'))
					{
						positionToSet = newPos.clone();
						maxLength = length;
						NextPos.x = p.x;
						NextPos.y = p.y;
					}
					newR = map.doAStep(newPos,newR);
				}
			}
		}
		if(map.getCharAt(positionToSet)=='x')
		{
			map.decreaseOverrideStonesOfPlayer();
		}
		serverM.sendMove(positionToSet.x,positionToSet.y,0,myPlayerNr);
	}

	//phase 1
	/**
	 * Method to make a move after a move request was sent to the Client
	 */
	private void makeAMove(long upperTimeLimit){
		//calculated Move
		int[] validPosition;
		final boolean phaseOne = true;
		final boolean randomMove = false;

		//make a calculated move
		if (!randomMove) {
			validPosition = searchTree.getMove(map, timed, depth, phaseOne, upperTimeLimit, moveCounter);
		}
		else {
			validPosition = map.getRandomMove();
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

			if (printOn) System.out.println("Recommended Move: (" + validPosition[0] + "," + validPosition[1] + "," + validPosition[2] + ")");

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
		final boolean phaseOne = false;

        //get a move
        if (calculateMove)
		{
				validPosition = searchTree.getBombPosition(map,upperTimeLimit);
        }
        //let player pick a move
        else {
			Position posToSetKeystone = new Position(0, 0);
			Scanner sc = new Scanner(System.in);
			boolean moveIsPossible = false;

            while (!moveIsPossible) {


                //enter the move
                if (printOn) System.out.print("Enter the next move (x,y): ");

                posToSetKeystone.x = sc.nextInt();
                posToSetKeystone.y = sc.nextInt();

                if (printOn) System.out.println();

                //check if the move is valid
                if (map.getCharAt(posToSetKeystone) != '-' && map.getCharAt(posToSetKeystone) != 't') moveIsPossible = true;
				//moveIsPossible = true;
            }
			validPosition = new int[]{posToSetKeystone.x, posToSetKeystone.y};
			sc.close();
        }

		//send the move
		serverM.sendMove(validPosition[0], validPosition[1], 0, myPlayerNr);
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
}