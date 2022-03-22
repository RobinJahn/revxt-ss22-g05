package src;

import java.util.ArrayList;

class Moves {
	public ArrayList<int[]> possibleMoves;
	public ArrayList<int[]> movesToCheck;

	public Moves() {
		possibleMoves = new ArrayList<>();
		movesToCheck = new ArrayList<>();
	}

	public void addPositionInAllDirections(int x, int y){
		for (int r = 0; r <= 7; r++){
			movesToCheck.add(new int[]{x,y,r});
		}
	}
}

public class Client {
	
	public static void main(String[] args) {

		Map map = new Map();

		validMoves(map);

		System.out.println(map);
	}
	
	
	static ArrayList<int[]> validMoves(Map map) {
		Moves moves = new Moves();

		getFildsByNeighbour(map, moves);
		deleteNotPossibleMoves(map, moves);
		
		return moves.possibleMoves;
	}

	private static void getFildsByNeighbour(Map map, Moves moves){
		int currentlyPlaying = map.getCurrentlyPlaying();
		char currChar;

		for (int y = 0; y < map.getHeight(); y++){
			for (int x = 0; x < map.getWidth(); x++){

				currChar = map.getCharAt(x,y);

				if (Character.isAlphabetic(currChar) && currChar == 'x' && map.getOverwriteStonesForPlayer(currentlyPlaying) > 0){ //expansion Field
					//Add finaly
					moves.possibleMoves.add(new int[]{x, y});
				}
				if (Character.isDigit(currChar)){
					int currNumber = Integer.parseInt(Character.toString(currChar));
					if (currNumber == currentlyPlaying){
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
		for (int r = 0; r <= 7; r++){
			char fieldInDirectionR = '-';
			switch (r) {
				//TODO: check if thats compatible with java 11
				case 0 -> fieldInDirectionR = map.getCharAt(x, y-1);
				case 1 -> fieldInDirectionR = map.getCharAt(x+1,y-1);
				case 2 -> fieldInDirectionR = map.getCharAt(x+1,y);
				case 3 -> fieldInDirectionR = map.getCharAt(x+1,y+1);
				case 4 -> fieldInDirectionR = map.getCharAt(x,y+1);
				case 5 -> fieldInDirectionR = map.getCharAt(x-1,y+1);
				case 6 -> fieldInDirectionR = map.getCharAt(x-1,y);
				case 7 -> fieldInDirectionR = map.getCharAt(x-1,y-1);
			}
			if (fieldInDirectionR == '0'){
				int oppositeDirection = (r+4)%8;
				moves.movesToCheck.add(new int[]{x,y,oppositeDirection});
			}
		}
	}

	private static ArrayList<int[]> getFieldsByOwnColor(Map map){
		//TODO: complete
		return null;
	}

	private static void deleteNotPossibleMoves(Map map, Moves moves){

	}
	

}
