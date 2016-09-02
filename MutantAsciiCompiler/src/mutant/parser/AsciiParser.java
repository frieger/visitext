package mutant.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscEdge;
import mutant.util.Coords;
import mutant.util.Direction;
import mutant.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class AsciiParser {

	private final static int RESERVED_COLOR = -9;
	
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
				if (array[y][x].c == '[' && (array[y][x].color == 0 || array[y][x].color == RESERVED_COLOR)) {
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
						ep.followLine(startx, starty, dir, array, nextCol, -1, false, false, false, " ");
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
		
		//TODO: Add check for 8-neigh here to allow comments anywhere
		for(int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[y].length; x++) {
				for (char currentHead : arrowheads) {
					if ((input[y][x].color == 0) && (input[y][x].c == currentHead)) {
						// head found, ignore coloured areas
						
						/*  neighborhood check:
						 * 1) <-
						 * 2) <'
						 * 3) <.
						 * 4) <(
						 * 5) <)
						 * 6) ->
						 * 7) '>
						 * 8) .>
						 * 9) (>
						 * 10) )>
						 * 11) @>
						 * 12) #>
						 * 13) <#
						 * 14) <@
						 * 15) ^   16)  ^   17) ^   18) ^   19) ^   20) ^
						 *     |        '       (       )       #       @
						 *     
						 * 21) |   22)  .   23) (   24) )   25) #   26) @
						 *     v        v       v       v       v       v
						 *     
						 * 27) A   28)  A   29) A   30) A   31) A   32) A
						 *     |        '       (       )       #       @
						 */
						
						AscChar[][] neigh = Util.get8Neigh(x, y, input);
						
						boolean isArrowhead = false;
						
						if (neigh[1][1].c == '<') {
							if (neigh[1][2].c == '-' || neigh[1][2].c == '\'' || neigh[1][2].c == '.' 
													 || neigh[1][2].c == '(' || neigh[1][2].c == ')'
													 || neigh[1][2].c == '@' || neigh[1][2].c == '#') {
								isArrowhead = true;
							}
						} else if (neigh[1][1].c == '>') {
							if (neigh[1][0].c == '-' || neigh[1][0].c == '\'' || neigh[1][0].c == '.' 
									 || neigh[1][0].c == '(' || neigh[1][0].c == ')'
									 || neigh[1][0].c == '@' || neigh[1][0].c == '#') {
								isArrowhead = true;
							}
						} else if (neigh[1][1].c == '^') {
							if (neigh[2][1].c == '|' || neigh[2][1].c == '\'' 
									 || neigh[2][1].c == '(' || neigh[2][1].c == ')'
									 || neigh[2][1].c == '@' || neigh[2][1].c == '#') {
								isArrowhead = true;
							}
						} else if (neigh[1][1].c == 'A') {
							if (neigh[2][1].c == '|' || neigh[2][1].c == '\'' 
									 || neigh[2][1].c == '(' || neigh[2][1].c == ')'
									 || neigh[2][1].c == '@' || neigh[2][1].c == '#') {
								isArrowhead = true;
							}
						} else if (neigh[1][1].c == 'v') {
							if (neigh[0][1].c == '|' || neigh[0][1].c == '.' 
									 || neigh[0][1].c == '(' || neigh[0][1].c == ')'
									 || neigh[0][1].c == '@' || neigh[0][1].c == '#') {
								isArrowhead = true;
							}
						}
								
						if (isArrowhead) {
							coords.add(new Coords(x, y));
						}
					}
				}	
			}
		}
		
		return coords;
	}
	
	/**
	 * Colors all uncolored edge labels and signal labels with the reserved color
	 * @param input input array. Will be modified.
	 */
	public static void reserveColorAllLabelsAndSignals(AscChar[][] input) {
		for (int y = 0; y < input.length; y++) {
			boolean insideLabel = false;
			for (int x = 0; x < input[0].length; x++) {
				if (input[y][x].color == 0) { // only color uncolored characters
					if (input[y][x].c == '{' || input[y][x].c == '[') {
						insideLabel = true;
					}
					
					if (insideLabel) {
						input[y][x].color = RESERVED_COLOR;
					}
					
					if (input[y][x].c == '}' || input[y][x].c == ']') {
						insideLabel = false;
					}
					// inside label
				}
			}
		}
	}
	
	/**
	 * Detects and follows bidirectional edges, adding them to the EdgeParser's edge list
	 * @param array	Input array
	 * @param ep Edge Parser
	 */
	public static void detectAndFollowBidirectionalEdges(AscChar[][] array, EdgeParser ep) {

		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if (array[y][x].color == 0) { // only check edges that have not been otherwise processed
					if (array[y][x].c == '|') {
						AscChar[][] neigh = Util.get8Neigh(x, y, array);
							// north
						if (neigh[0][1].color != 0 && neigh[0][1].c == '-') {	// detected some class to the north
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.NORTH, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.SOUTH, array, edgeColor, neigh[0][1].color, false, false, false, edgeMultiplicity);
							ep.makeReverseEdge(theEdge, false, false);
						} else if (neigh[2][1].color != 0 && neigh[2][1].c == '-') { // detected some class to the south
							// south
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.SOUTH, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.NORTH, array, edgeColor, neigh[2][1].color, false, false, false, edgeMultiplicity);
							ep.makeReverseEdge(theEdge, false, false);
						}
					} else if (array[y][x].c == '-') {
						AscChar[][] neigh = Util.get8Neigh(x, y, array);
						// (0,0) (0,1) (0,2)
						// (1,0) (1,1) (1,2)
						// (2,0) (2,1) (2,2)
						if (neigh[1][0].color != 0 && neigh[1][0].c == '|') { // detected some class to the west
							int edgeColor = Util.getNextColor();
							System.err.println("Detected some class to the west");
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.WEST, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.EAST, array, edgeColor, neigh[1][0].color, false, false, false, edgeMultiplicity);
							ep.makeReverseEdge(theEdge, false, false);
						} else if (neigh[1][2].color != 0 && neigh[1][2].c == '|') { // detected some class to the east
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.EAST, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.WEST, array, edgeColor, neigh[1][2].color, false, false, false, edgeMultiplicity);
							ep.makeReverseEdge(theEdge, false, false);

						}
					} else if (array[y][x].c == '#' || array[y][x].c == '@') {
						// containment / aggregation needs special handling
						System.err.println("found bidirectional containment/aggregation edge");
						AscChar[][] neigh = Util.get8Neigh(x, y, array);
						// (0,0) (0,1) (0,2)
						// (1,0) (1,1) (1,2)
						// (2,0) (2,1) (2,2)

						// |         |        ---   |
						// |#-     -#|         #    #
						// |         |         |   ---
						
						boolean isContainment = (array[y][x].c == '#');
						boolean isAggregation = (array[y][x].c == '@');
						
						System.err.println("character: <" + array[y][x].c + ">  " + isContainment + "  " + isAggregation);
						
						if (neigh[0][1].color != 0 && neigh[0][1].c == '-') {	// detected some class to the north
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.NORTH, true, array, edgeColor);
							//AscEdge theEdge = ep.followLine(startx, starty, startDir, array, lineColor, _startColor, _isContainment, _isInheritance, _isAggregation, _startMul)
							AscEdge theEdge = ep.followLine(x, y, Direction.SOUTH, array, edgeColor, neigh[0][1].color, false, false, false, edgeMultiplicity);
							/*theEdge.isContainment = isContainment;
							theEdge.isAggregation = isAggregation;
							
							int otherEndX = 0;
							int otherEndY = 0;
							if (theEdge.startx == x && theEdge.starty == y) {
								otherEndX = theEdge.endx;
								otherEndY = theEdge.endy;
							} else if (theEdge.endx == x && theEdge.endy == y) {
								otherEndX = theEdge.startx;
								otherEndY = theEdge.starty;
							}
							
							char charAtOtherEnd = array[otherEndY][otherEndX].c;
							boolean otherEndIsContainment = (charAtOtherEnd == '#');
							boolean otherEndIsAggregation = (charAtOtherEnd == '@');
							*/
							ep.makeReverseEdge(theEdge, isContainment, isAggregation);
						} else if (neigh[2][1].color != 0 && neigh[2][1].c == '-') { // detected some class to the south
							// south
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.SOUTH, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.NORTH, array, edgeColor, neigh[2][1].color, false, false, false, edgeMultiplicity);
							
							ep.makeReverseEdge(theEdge, isContainment, isAggregation);
						} else if (neigh[1][0].color != 0 && neigh[1][0].c == '|') { // detected some class to the west
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.WEST, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.EAST, array, edgeColor, neigh[1][0].color, false, false, false, edgeMultiplicity);
							
							ep.makeReverseEdge(theEdge, isContainment, isAggregation);
						} else if (neigh[1][2].color != 0 && neigh[1][2].c == '|') { // detected some class to the east
							int edgeColor = Util.getNextColor();
							String edgeMultiplicity = ep.getMultiplicity(x, y, Direction.EAST, true, array, edgeColor);
							AscEdge theEdge = ep.followLine(x, y, Direction.WEST, array, edgeColor, neigh[1][2].color, false, false, false, edgeMultiplicity);
														
							ep.makeReverseEdge(theEdge, isContainment, isAggregation);
						}


					}
				}
			}
		}
	}

}
