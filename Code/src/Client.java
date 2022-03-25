package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

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
	
	public static void main(String[] args) {

		Map map = new Map();
		Scanner sc = new Scanner(System.in);

		System.out.println(map.toString(null));
		while (true){
			System.out.println("Possible Moves:");
			ArrayList<Position> validMoves = getValidMoves(map);
			System.out.println(Arrays.toString(validMoves.toArray()));

			System.out.print("Geben Sie den naechsten zug ein (x,y): ");
			int x = sc.nextInt();
			int y = sc.nextInt();
			System.out.println();

			//check the move
			ArrayList<Integer> directions = new ArrayList<>();
			for (int i = 0; i <= 7; i++) directions.add(i);
			boolean movePossible = checkIfMoveIsPossible(new Position(x,y), directions, map);
			if (!movePossible) {
				System.err.println("Move isn't possible");
				continue;
			}

			//do the move
			map.setCharAt(x, y, map.getCurrentlyPlayingC());

			System.out.println(map.toString(validMoves));

		}
	}
	
	
	static ArrayList<Position> getValidMoves(Map map) {
		Moves moves = new Moves();

		//TODO: add seperation wich move finder should be jused
		getCandidatesByNeighbour(map, moves);
		deleteNotPossibleMoves(map, moves);
		
		return moves.possibleMoves;
	}

	private static void getCandidatesByNeighbour(Map map, Moves moves){
		int currentlyPlaying = map.getCurrentlyPlayingI();
		char currChar;

		//goes over every field
		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){

				currChar = map.getCharAt(x,y);

				//check for expansions field
				if (Character.isAlphabetic(currChar) && currChar == 'x' && map.getOverwriteStonesForPlayer(currentlyPlaying) > 0){
					//Add finaly
					moves.possibleMoves.add(new Position(x,y));
				}
				//if a player is there
				if (Character.isDigit(currChar) && currChar != '0'){
					int currNumber = Integer.parseInt(Character.toString(currChar));
					//if it's one of the own keystones an overwrite-stone could be set
					if (currNumber == currentlyPlaying && map.getOverwriteStonesForPlayer(currentlyPlaying) > 0){
						//add and check all directions
						moves.addPositionInAllDirections(x,y);
					}
					else if (currNumber > 0){
						//check all neighboures and add it if the field is 0
						checkAllNeighboures(map, moves, x, y);
					}
				}
			}
		}
	}

	private static void checkAllNeighboures(Map map, Moves moves, int x, int y){
		Position startPos = new Position(x,y);
		Position currPos;
		char blankField = '0';

		// go in every direction and check if there's a free field where you could place a keystone
		for (int r = 0; r <= 7; r++){
			char fieldInDirectionR = '-'; //only neccessary to initialize because the compiler want's it initialized in all cases
			//reset x and y
			currPos = startPos; //resets currPos to startPos

			//change x and y according to direction
			currPos = Position.goInR(currPos,r);

			fieldInDirectionR = map.getCharAt(currPos);

			//TODO: TEST
			//for a blank field add apossible move
			if (fieldInDirectionR == blankField){
				int oppositeDirection = (r+4)%8;
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


	private static void deleteNotPossibleMoves(Map map, Moves moves){
		boolean connectionFound;

		char myColor = map.getCurrentlyPlayingC();

		//check every move if it's possible
		for (Position pos : moves.movesToCheck.keySet()){

			//go over every direction and check if there is a Connection
			ArrayList<Integer> directions;
			directions = moves.movesToCheck.get(pos);

			connectionFound = checkIfMoveIsPossible(pos, directions, map);
			if (connectionFound) moves.possibleMoves.add(pos);
		}
	}

	private static boolean checkIfMoveIsPossible(Position pos, ArrayList<Integer> directions, Map map){
		Position currPos;
		boolean stepPossible;
		boolean wasFirstStep;
		char currChar;

		if (map.getCharAt(pos) == 'x' && map.getOverwriteStonesForPlayer(map.getCurrentlyPlayingI()) > 0) return true;

		for (Integer r : directions){
			//reset x and y
			currPos = pos.clone();
			wasFirstStep = true;
			while (true) {
				stepPossible = doAStep(currPos, r, map); //call by reference
				if (!stepPossible) break;

				//check what's there
				//check for blank or not in field
				currChar = map.getCharAt(currPos);
				if (currChar == '-' || currChar == '0') break;
				//check for players
				if (wasFirstStep) {
					if (currChar == map.getCurrentlyPlayingC()) break;
					wasFirstStep = false;
				} else {
					if (currChar == map.getCurrentlyPlayingC()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean doAStep(Position pos, int r, Map map){
		//check if step is valid in x direction
		if (pos.x == 0){
			if (r == 7 || r == 6 || r == 5) return false;
		}
		if (pos.x == map.getWidth()-1){
			if (r == 1 || r == 2 || r == 3) return false;
		}
		//check if step is valid in y direction
		if (pos.y == 0) {
			if (r == 7 || r == 0 || r == 1) return false;
		}
		if (pos.y == map.getHeight()-1){
			if (r == 3 || r == 4 || r == 5) return false;
		}

		Position newPos = Position.goInR(pos, r);
		pos.x = newPos.x;
		pos.y = newPos.y;
		return true;
	}


}
