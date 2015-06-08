package mutant.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import mutant.ascii.representation.AscChar;
import mutant.util.Coords;
import mutant.util.Direction;
import mutant.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class AsciiParser {

	/**
	 * Builds array from String
	 * @param s
	 * @return array
	 */
	public static AscChar[][] buildArrayFromString(String s) {
		int strWidth = 0;
		int strHeight;
		
		ArrayList<String> lines = new ArrayList<String>();
		Scanner sc = new Scanner(s);
		
		// first, figure out the String's bounding box. We need this to build our rectangular array to operate on
		while(sc.hasNextLine()) {
			String nextLine = sc.nextLine();
			lines.add(nextLine);
			if (nextLine.length() > strWidth) {
				strWidth = nextLine.length();
			}
		}
	
		sc.close();
		strHeight = lines.size();
		
		System.out.println("string dimensions:" + strWidth + " x " + strHeight);
		
		
		AscChar[][] array = new AscChar[strHeight][strWidth];
		
		for (int yc = 0; yc < array.length; yc++) {
			String currentLine = lines.get(yc);
			for (int xc = 0; xc < array[0].length; xc++) {
				if (currentLine.length() <= xc) {
					array[yc][xc] = new AscChar(' ');
				} else {
					array[yc][xc] = new AscChar(currentLine.charAt(xc));
				}
			}
		}
		
		return array;
	
	}

	/**
	 * This method detects signals (abbreviated edges) and follows them
	 * @param array	input array
	 */
	public static void detectAndFollowSignals(AscChar[][] array, EdgeParser ep) {
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if (array[y][x].c == '[' && array[y][x].color == 0) {
					// found unknown signal
					int nextCol = Util.getNextColor();
					String signalName = ep.processSignal(array, x, y, nextCol, false);
					System.out.println("Found unknown signal:" + signalName + ":");
					// now check around the signal name for lines
					AscChar[][] leftSignalNeigh = Util.get8Neigh(x, y, array);
					AscChar[][] rightSignalNeigh = Util.get8Neigh(x+signalName.length()+1, y, array);
					
					Util.printArray(leftSignalNeigh);
					Util.printArray(rightSignalNeigh);
					//  012
					//   |  0
					//  -*- 1
					//   |  2
					
					boolean foundEdge = false;
					Direction dir = null;
					int dirx = 0;
					int diry = 0;
					int startx = x;
					int starty = y;
					if (leftSignalNeigh[0][1].c == '|') {
						// dir: NORTH
						foundEdge = true;
						dir = Direction.NORTH;
						dirx = 0;
						diry = -1;
						startx = x;
						starty = y-1;
					} else if (leftSignalNeigh[2][1].c == '|') {
						// dir: SOUTH
						foundEdge = true;
						dir = Direction.SOUTH;
						dirx = 0;
						diry = 1;
						startx = x;
						starty = y+1;
					} else if (leftSignalNeigh[1][0].c == '-') {
						// dir: WEST
						foundEdge = true;
						dir = Direction.WEST;
						dirx = -1;
						diry = 0;
						startx = x;
						starty = y;
					} else if (rightSignalNeigh[0][1].c == '|') {
						// dir: NORTH, right
						foundEdge = true;
						dir = Direction.NORTH;
						dirx = 0;
						diry = -1;
						startx = x+signalName.length()+1;
						starty = y-1;
					} else if (rightSignalNeigh[2][1].c == '|') {
						// dir: SOUTH, right
						foundEdge = true;
						dir = Direction.SOUTH;
						dirx = 0;
						diry = 1;
						startx = x + signalName.length() + 1;
						starty = y+1;
					} else if (rightSignalNeigh[1][2].c == '-') {
						// dir: EAST, right
						foundEdge = true;
						dir = Direction.EAST;
						dirx = 1;
						diry = 0;
						startx = x + signalName.length() + 1;
						starty = y;
					}
					
					// If an edge was found, follow it
					if (foundEdge) {
						System.out.println("Found edge, now following");
						ep.followLine(startx, starty, dir, array, nextCol, -1, false, false, ' ');
					}
				}
			}
		}
	}

	/**
	 * Get locations of arrowheads (<, >, v, ^, A) that have not been processed yet
	 * (Arrowheads are considered unprocessed if they have color = 0)
	 * @param input	input array
	 * @return	List of coordinates of arrowheads
	 */
	public static List<Coords> getArrowHeadLocations(AscChar[][] input) {
		char[] arrowheads = {'<', '>', 'v', '^', 'A'};
		List<Coords> coords = new ArrayList<Coords>();
		
		for(int y = 0; y < input.length; y++) {
			for (int i = 0; i < input[y].length; i++) {
				for (char currentHead : arrowheads) {
					if ((input[y][i].color == 0) && (input[y][i].c == currentHead)) {
						// head found, ignore coloured areas
						coords.add(new Coords(i, y));
					}
				}	
			}
		}
		
		return coords;
	}

}
