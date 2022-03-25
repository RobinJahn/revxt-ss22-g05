package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

class Position {
	public int x;
	public int y;

	public Position(int x, int y){
		this.x = x;
		this.y = y;
	}

	public Position(int[] pos){
		this.x = pos[0];
		this.y = pos[1];
	}

	@Override
	public int hashCode() {
		return 100*y + x;
	}

	@Override
	public boolean equals(Object obj) {
		Position p = (Position) obj;
		if (this.x == p.x && this.y == p.y) return true;
		return false;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}

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
		/* Test
		HashMap<Position, Integer> map = new HashMap<>();
		Position a = new Position(1,2),
				b = new Position(3,4);
		map.put(a, 15);
		map.put(b,2);

		System.out.println(map.get(new Position(1,2)));
		*/

		Map map = new Map();
		Scanner sc = new Scanner(System.in);

		System.out.println(map);
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
			boolean movePossible = searchForConnections(new Position(x,y), directions, map);
			if (!movePossible) {
				System.err.println("Move isn't possible");
				continue;
			}

			//do the move
			map.setCharAt(x, y, map.getCurrentlyPlayingC());

			System.out.println(map);

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
					//moves.possibleMoves.add(new Position(x,y)); //TODO: comment in
				}
				//if a player is there
				if (Character.isDigit(currChar) && currChar != '0'){
					int currNumber = Integer.parseInt(Character.toString(currChar));
					//if it's one of the own keystones an overwrite-stone could be set
					if (currNumber == currentlyPlaying && map.getOverwriteStonesForPlayer(currentlyPlaying) > 0){
						//add and check all directions
						//moves.addPositionInAllDirections(x,y); //TODO: comment in
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
		int xSaved = x, ySaved = y;
		char blankField = '0';

		// go in every direction and check if there's a free field where you could place a keystone
		for (int r = 0; r <= 7; r++){
			char fieldInDirectionR = '-'; //only neccessary to initialize because the compiler want's it initialized in all cases
			//reset x and y
			x = xSaved;
			y = ySaved;

			//change x and y according to direction
			switch (r) {
				case 0:
					y = y - 1;
					break;
				case 1:
					x = x + 1;
					y =y - 1;
					break;
				case 2:
					x = x + 1;
					break;
				case 3:
					x= x + 1;
					y = y + 1;
					break;
				case 4:
					y = y + 1;
					break;
				case 5:
					x = x - 1;
					y = y + 1;
					break;
				case 6:
					x = x - 1;
					break;
				case 7:
					x = x - 1;
					y = y - 1;
					break;
			}
			fieldInDirectionR = map.getCharAt(x,y);

			//TODO: TEST
			//for a blank field add apossible move
			if (fieldInDirectionR == blankField){
				int oppositeDirection = (r+4)%8;
				Position pos = new Position(x,y);
				ArrayList<Integer> directions = moves.movesToCheck.get(pos);
				//if position didn't exist yet
				if (directions == null) {
					directions = new ArrayList<>();
					directions.add(oppositeDirection);
					moves.movesToCheck.put(pos,directions);
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

			connectionFound = searchForConnections(pos, directions, map);
			if (connectionFound) moves.possibleMoves.add(pos);
		}
	}

	private static boolean searchForConnections(Position pos, ArrayList<Integer> directions, Map map){
		int xSaved = pos.x,
				ySaved = pos.y;
		boolean stepPossible;
		boolean wasFirstStep = true;
		char currChar;

		for (Integer r : directions){
			//reset x and y
			pos.x = xSaved;
			pos.y = ySaved;
			while (true) {
				stepPossible = doAStep(pos, r, map); //call by reference
				if (!stepPossible) break;

				//check what's there
				//check for blank or not in field
				currChar = map.getCharAt(pos.x, pos.y);
				if (currChar == '-' || currChar == '0') break;
				//check for players
				if (wasFirstStep) {
					if (currChar == map.getCurrentlyPlayingC()) break;
					wasFirstStep = false;
				} else {
					if (currChar == map.getCurrentlyPlayingC()) {
						pos.x = xSaved;
						pos.y = ySaved;
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

		switch (r){
			case 0:
				pos.y -= 1;
				break;
			case 1:
				pos.x += 1;
				pos.y -= 1;
				break;
			case 2:
				pos.x += 1;
				break;
			case 3:
				pos.x += 1;
				pos.y += 1;
				break;
			case 4:
				pos.y += 1;
				break;
			case 5:
				pos.y += 1;
				pos.x -= 1;
				break;
			case 6:
				pos.x -= 1;
				break;
			case 7:
				pos.x -= 1;
				pos.y -= 1;
				break;
		}
		return true;
	}


}
