package mutant.parser;

import java.util.ArrayList;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscEdge;
import mutant.ascii.representation.AscSig;
import mutant.util.Direction;
import mutant.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class EdgeParser {

	public static boolean DEBUG = false;
	private static final int MULTIPLICITY_STRING_LENGTH = 8;
	private final String MULTIPLICITY_REGEX = "\\s*[0-9]+(\\.\\.)([0-9]+|\\*)" + "(\\s*.*)|((\\+|\\|).*)"; // should match multiplicities in the form of '(whitespace)* x..y'   where x = number, y = number or *.   Second part should match greedy whitespace followed by any characters or ( + or | (class vertices and edges) followed by any characters)  
	
	private ArrayList<AscEdge> edges = new ArrayList<AscEdge>();		// list of all edges
	private ArrayList<String> lineNames = new ArrayList<String>();		// list of all line names
	private ArrayList<String> signalNames = new ArrayList<String>();		// list of all signal names
	private ArrayList<AscSig> signals = new ArrayList<AscSig>();			// list of all signals



	public ArrayList<String> getSignalNames() {
		return signalNames;
	}

	public void setSignalNames(ArrayList<String> signalNames) {
		this.signalNames = signalNames;
	}

	public ArrayList<AscSig> getSignals() {
		return signals;
	}

	public void setSignals(ArrayList<AscSig> signals) {
		this.signals = signals;
	}

	public ArrayList<AscEdge> getEdges() {
		return edges;
	}

	public void setEdges(ArrayList<AscEdge> edges) {
		this.edges = edges;
	}

	
	public ArrayList<String> getLineNames() {
		return lineNames;
	}

	public void setLineNames(ArrayList<String> lineNames) {
		this.lineNames = lineNames;
	}

	/**
	 * Combines two edges into one edge.
	 * One edge needs to have a source, but no target. The other edge needs to have a target, but no source.
	 * Which one is which does not matter.
	 * @param e1	first edge
	 * @param e2	second edge
	 * @return		combined edge
	 * @throws Exception if edges could not be combined
	 */
	public static AscEdge combineEdges(AscEdge e1, AscEdge e2) throws Exception {
		System.out.println("Now combining " + e1 + " and " + e2);
		AscEdge startEdge;
		AscEdge endEdge;
		AscEdge resEdge;
		boolean isContainment = false;
		boolean isInheritance = false;
		String edgeLabel;
		
		if (e1.startColor == -1 && e1.endColor != -1 && e2.startColor != -1 && e2.endColor == -1) {
			startEdge = e2;
			endEdge = e1;
		} else if (e1.startColor != -1 && e1.endColor == -1 && e2.startColor == -1 && e2.endColor != -1) {
			startEdge = e1;
			endEdge = e2;
		} else {
			throw new Exception("Can not combine edges " + e1 + " and " + e2);
		}
		
		if (e1.label != null && e2.label != null) {
			throw new Exception("Can not combine edges " + e1 + " and " + e2 + ": Multiple labels detected: " + e1.label + " and " + e2.label);
		} else if (e1.label != null) {
			edgeLabel = e1.label;
		} else if (e2.label != null) {
			edgeLabel = e2.label;
		} else {
			edgeLabel = null;
		}
		
		if (e1.isContainment == true && e2.isContainment == true) {
			throw new Exception("Can not combine edges " + e1 + " and " + e2 + ": Both are containment edges.");
		} else if (e1.isContainment == true || e2.isContainment == true) {
			isContainment = true;
		}
		
		if (e1.isInheritance == true && e2.isInheritance == true) {
			throw new Exception("Can not combine edges " + e1 + " and " + e2 + ": Both are inheritance edges.");
		} else if (e1.isInheritance == true || e2.isInheritance == true) {
			isInheritance = true;
		}
		
		resEdge = new AscEdge(startEdge.startx, startEdge.starty, endEdge.endx, endEdge.endy, Util.getNextColor(), startEdge.startColor, endEdge.endColor, edgeLabel, null);
		resEdge.isSignalEdge = false; // the resulting edge is not a signal edge, as we have resolved the signal
		resEdge.isContainment = isContainment;
		resEdge.isInheritance = isInheritance;
		resEdge.startMultiplicity = startEdge.startMultiplicity;
		resEdge.endMultiplicity = endEdge.endMultiplicity;
		
		return resEdge;
	}

	/**
	 * Creates new edges from signal edges (abbreviated edges)
	 * This will modify the list of edges
	 * @throws Exception if edges could not be connected
	 */
	public void connectSignalEdges() throws Exception {
		
		for (AscSig s : signals) {
			if (s.incomingColors.size() > 1 && s.outgoingColors.size() > 1) {
				// error case, signal has multiple incoming and outgoing edges
				throw new Exception("Signal may only have 1 incoming or outgoing edge");
			} else if (s.incomingColors.size() == 1 && s.outgoingColors.size() == 1) {
				// signal has exactly one incoming and one outgoing edge
				// --> create new edge that connects both edges and delete old edges
				AscEdge e1 = Util.getEdgeForColor(s.incomingColors.get(0), edges);
				AscEdge e2 = Util.getEdgeForColor(s.outgoingColors.get(0), edges);
				AscEdge result = combineEdges(e1, e2);
				edges.remove(e1);
				edges.remove(e2);
				edges.add(result);
			} else if (s.incomingColors.size() > 1 && s.outgoingColors.size() == 1) {
				// one-to-many-edge:
				// outgoing edge is one, incoming edges are many
				// -> combine outgoing edge with every incoming edge, then delete
				AscEdge outgoing = Util.getEdgeForColor(s.outgoingColors.get(0), edges);
				for (int inColor : s.incomingColors) {
					AscEdge incoming = Util.getEdgeForColor(inColor, edges);
					AscEdge result = combineEdges(outgoing, incoming);
					edges.add(result);
					edges.remove(incoming);
				}
				edges.remove(outgoing);
			} else if (s.incomingColors.size() == 1 && s.outgoingColors.size() > 1) {
				// many-to-one-edge:
				// incoming edge is one, outgoing edges are many
				// -> combine incoming edge with every outgoing edge, then delete
				AscEdge incoming = Util.getEdgeForColor(s.incomingColors.get(0), edges);
				for (int outColor : s.outgoingColors) {
					AscEdge outgoing = Util.getEdgeForColor(outColor, edges);
					AscEdge result = combineEdges(outgoing, incoming);
					edges.add(result);
					edges.remove(outgoing);
				}
				edges.remove(incoming);
			}
		}
	}
	
	/**
	 * Follows a line, starting from an arrowhead 
	 * Will modify EDGES and LINENAMES
	 * @param startx	x coordinate of the arrowhead
	 * @param starty	y coordinate of the arrowhead
	 * @param array		input array
	 * @param lineColor	color the line should have
	 */
	public void followLineFromArrowhead(int startx, int starty, AscChar[][] array, int lineColor) {
		
		AscChar[][] startNeigh = Util.get8Neigh(startx, starty, array);
				
		int dirx = 0;
		int diry = 0;
		Direction currentDirection = null;
		
		int startColor = -1;
		
		boolean isContainment = false; // is the edge a containment edge (i.e. one that looks like #--->)
		boolean isInheritance = false; // is the edge an inheritance edge (i.e. one that looks like A
		//                                                                                          |
		
		// always start on an arrowhead
		if (startNeigh[1][1].c == '>') {
			// end of arrow reached, so go WEST to find other end
			dirx = -1;
			diry = 0;
			currentDirection = Direction.WEST;
			startColor = startNeigh[1][2].color;
		} else if (startNeigh[1][1].c == '<') {
			// end of arrow reached, so go EAST to find other end
			dirx = 1;
			diry = 0;
			currentDirection = Direction.EAST;
			startColor = startNeigh[1][0].color;
		} else if (startNeigh[1][1].c == '^') {
			// end of arrow reached, so go SOUTH to find other end
			dirx = 0;
			diry = 1;
			currentDirection = Direction.SOUTH;
			startColor = startNeigh[0][1].color;
		} else if (startNeigh[1][1].c == 'v') { 
			// end of arrow reached, so go NORTH to find other end
			dirx = 0;
			diry = -1;
			currentDirection = Direction.NORTH;
			startColor = startNeigh[2][1].color;
		} else if (startNeigh[1][1].c == 'A') {
			// abstract edge, so go south
			dirx = 0;
			diry = 1;
			currentDirection = Direction.SOUTH;
			startColor = startNeigh[0][1].color;
			isInheritance = true;
		}
		
		// figure out multiplicity at start
		
		//char startMul = 0;
		String startMul = "";
		
		//Util.printArray(mulNeigh);
		
		//System.err.println("edge parsing::::\n>" + Util.extractString(10, 1, 8, mulNeigh, false) + "<  r >" + Util.extractString(10, 1, 8, mulNeigh, true)+"<");
		
		/*
		if (currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH) {
			// multiplicity is left or right
			char mulLeft = startNeigh[1][0].c;
			char mulRight = startNeigh[1][2].c;
			if (mulLeft == '*' || mulLeft == '1') {
				startMul = mulLeft;
			} else if (mulRight == '*' || mulRight == '1') {
				startMul = mulRight;
			}
		} else if (currentDirection == Direction.WEST || currentDirection == Direction.EAST) {
			char mulTop = startNeigh[0][1].c;
			char mulBot = startNeigh[2][1].c;
			if (mulTop == '*' || mulTop == '1') {
				startMul = mulTop;
			} else if (mulBot == '*' || mulBot == '1') {
				startMul = mulBot;
			}
		}*/
		

		startMul = getMultiplicity(startx, starty, currentDirection, false, array);
		
		System.err.println("figured out multiplicity: " + startMul);
		
		followLine(startx, starty, currentDirection, array, lineColor, startColor, isContainment, isInheritance, startMul);
		
	}
	
	
	private String getMultiplicity(int startx, int starty, Direction currentDirection, boolean isOutgoingEdge, AscChar[][] array) {
		
		String startMul = "";
		AscChar[][] mulNeigh = Util.subArray(startx-MULTIPLICITY_STRING_LENGTH, starty-1, startx+MULTIPLICITY_STRING_LENGTH, starty+1, array);
		
		// if isOutgoingEdge is set, need to reverse W/E 
		if (currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH) {
			// multiplicity is left or right
			String mulLeft = Util.extractString(MULTIPLICITY_STRING_LENGTH, 1, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			String mulRight = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 1, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			System.err.println("\n\n\nNS_mulleft:~" + mulLeft + "~right~" + mulRight + "~");
			if (mulLeft.matches(MULTIPLICITY_REGEX)) {
				startMul = mulLeft;
			} else if (mulRight.matches(MULTIPLICITY_REGEX)) {
				startMul = mulRight;
			}
		} else if ((!isOutgoingEdge && currentDirection == Direction.EAST) | (isOutgoingEdge && currentDirection == Direction.WEST)) {
			// +-----------+ mul
			// | Class     |<-------
			// +-----------+ mul
			String mulTop = Util.extractString(MULTIPLICITY_STRING_LENGTH, 0, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			String mulBot = Util.extractString(MULTIPLICITY_STRING_LENGTH, 2, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			System.err.println("\n\n\nE_multop:~" + mulTop + "~bot~" + mulBot + "~");
			if (mulTop.matches(MULTIPLICITY_REGEX)) {
				startMul = mulTop;
			} else if (mulBot.matches(MULTIPLICITY_REGEX)) {
				startMul = mulBot;
			}
		} else if ((!isOutgoingEdge && currentDirection == Direction.WEST) | (isOutgoingEdge && currentDirection == Direction.EAST)) {
			//           mul+----------+
			//------------->| Class    |
			//          mul +----------+
			String mulTop = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 0, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			String mulBot = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 2, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			System.err.println("\n\n\nW_multop:~" + mulTop + "~bot~" + mulBot + "~");
			if (mulTop.matches(MULTIPLICITY_REGEX)) {
				startMul = mulTop;
			} else if (mulBot.matches(MULTIPLICITY_REGEX)) {
				startMul = mulBot;
			}
		}
		
		
		return startMul;
	}

	/**
	 * Follows a line.
	 * Will modify EDGES and LINENAMES
	 * @param startx			initial x coordinate
	 * @param starty			initial y coordinate
	 * @param startDir			initial direction
	 * @param array				input array
	 * @param lineColor			color the line should have
	 * @param _startColor		color of the start of the line
	 * @param _isContainment	is this line a containment edge
	 * @param _isInheritance	is this line an inheritance edge
	 * @param _startMul			multiplicity at the start of the line
	 */
	public void followLine(int startx, int starty, Direction startDir, AscChar[][] array, int lineColor, int _startColor, boolean _isContainment, boolean _isInheritance, String _startMul) {
		AscChar[][] startNeigh = Util.get8Neigh(startx, starty, array);
		
		String lineSignalName = null;
		String lineName = null;
		
		int dirx = 0;
		int diry = 0;
		Direction currentDirection = startDir;
		
		int startColor = _startColor;
		int endColor = -1;
		
		boolean isContainment = _isContainment; // is the edge a containment edge (i.e. one that looks like #--->)
		boolean isInheritance = _isInheritance; // is the edge an inheritance edge (i.e. one that looks like A
	
		String startMul = _startMul;
		String endMul = "";
		// ----------
		
		int currx = startx;
		int curry = starty;
		boolean foundEnd = false;
		boolean foundSignal = false;
		
		int counter = 1000;
		
		if (startDir == Direction.NORTH) {
			dirx = 0;
			diry = -1;
		} else if (startDir == Direction.SOUTH) {
			dirx = 0;
			diry = 1;
		} else if (startDir == Direction.WEST) {
			dirx = -1;
			diry = 0;
		} else {
			dirx = 1;
			diry = 0;
		}
		
		array[starty][startx].color = lineColor;
		while(!foundEnd && counter > 0) {
			counter--;
			currx = currx + dirx;
			curry = curry + diry;
			AscChar[][] currNeigh = Util.get8Neigh(currx, curry, array);
			
			
			if (DEBUG) {
				System.out.println("\n" + counter);
				System.out.println("current x:" + currx);
				System.out.println("current y:" + curry);
				System.out.println("current direction: " + currentDirection);
				Util.printArray(currNeigh);
			}
			
			// change direction?
			/*
			 * What should we do
			 * any direction, current char '#'  -> found a containment edge, STOP
			 * 
			 * characters that do not change the direction
			 * 
			 * DIRECTION WEST, current char '-' -> continue
			 * DIRECTION WEST; current char ')' -> continue
			 * DIRECTION WEST, current char '(' -> continue
			 * DIRECTION EAST; current char '-' -> continue
			 * DIRECTION EAST, current char ')' -> continue
			 * DIRECTION EAST, current char '(' -> continue
			 *     
			 *   crossing lines look like this:  
			 *     
			 *      |                         |
			 * -----)--->     or       -------(----->
			 *      |                         |
			 *      v                         v
			 *      
			 *      
			 * DIRECTION NORTH, current char '|' -> continue
			 * DIRECTION NORTH, current char '(' -> continue
			 * DIRECTION NORTH, current char ')' -> continue
			 * DIRECTION SOUTH, current char '|' -> continue
			 * DIRECTION SOUTH, current char '(' -> continue
			 * DIRECTION SOUTH, current char ')' -> continue
			 * 
			 * 
			 * characters that change the direction
			 * -----.        ^
			 *      |        |       
			 *      v    ----'       
			 *      
			 * DIRECTION WEST, current char '.'  -> direction SOUTH
			 * DIRECTION EAST, current char '.'  -> direction SOUTH
			 * DIRECTION WEST, current char ''' -> direction NORTH
			 * DIRECTION EAST, current char ''' -> direction NORTH
			 * 
			 * now it gets a little bit harder:
			 * DIRECTION NORTH, current char '.' -> direction west/east -> look at left/right to find out which direction to change to
			 * DIRECTION NORTH, current char ''' -> INVALID
			 * DIRECTION SOUTH, current char '.' -> INVALID
			 * DIRECTION SOUTH; current char ''' -> direction west/east -> look at left/right to find out which direction to change to 
			 *
			 *
			 *
			 *
			 *
			 *
			 * EDGE LABELS:
			 * direction NORTH, current char '|', char to the left '}' or char to the right '{' -> initialize label detection
			 * direction SOUTH, current char '|', char to the left '}' or char to the right '{' -> initialize label detection
			 * direction WEST, current char  '}' -> initialize label detection
			 * direction EAST, current char  '{' -> initialize label detection
			 * 
			 * 
			 * SIGNAL LABELS:
			 * direction NORTH, current char  '[' or ']' -> initialize signal detection
			 * direction SOUTH, current char  '[' or ']' -> initialize signal detection
			 * direction EAST, current char   '[' -> initialize signal detection
			 * direction WEST, current char   ']' -> initialize signal detection
			 */
			
			
			AscChar midChar = currNeigh[1][1];
			if (currentDirection == Direction.NORTH) {
				if (midChar.c == '|') {
					// check for label name
					if (currNeigh[1][0].c == '}' || currNeigh[1][2].c == '{') {
						// label
						int labelX;
						if (currNeigh[1][0].c == '}') {
							// label to the left
							labelX = currx - 1;
						} else {
							// label to the right
							labelX = currx + 1;
						}
						System.out.println("Found label");
						String lblName = detectLabel(array, labelX, curry, lineColor);
						System.out.println(">>>" + lblName + "<<<");
						lineName = lblName;
					}
					// continue
					dirx = 0;
					diry = -1;
				} else if (midChar.c == ')') {
					// continue
					dirx = 0;
					diry = -1;
				} else if (midChar.c == '(') {
					// continue
					dirx = 0;
					diry = -1;
				} else if (midChar.c == '.') {
					// change direction
					// new direction could be either west/east, so look at 8-neigh and decide
					if (currNeigh[1][0].c == '-') { // left -> go WEST
						currentDirection = Direction.WEST;
						dirx = -1;
						diry = 0;
					} else if (currNeigh[1][2].c == '-') { // right -> go EAST
						currentDirection = Direction.EAST;
						dirx = 1;
						diry = 0;
					} else {
						System.err.println("FAIL");
					}
				} else if (midChar.c == '[' || midChar.c == ']') {
					// signal name
					System.out.println("Found signal!");
					String sigName = processSignal(array, currx, curry, lineColor, true);
					System.out.println(">>>" + sigName + "<<<");
					signalNames.add(sigName);
					lineSignalName = sigName;
					foundEnd = true;
					foundSignal = true;
				} else if (midChar.c == '#') {
					foundEnd = true;
					isContainment = true;
					char mulLeft = currNeigh[1][0].c;
					char mulRight = currNeigh[1][2].c;
					/*
					if (mulLeft == '*' || mulLeft == '1') {
						endMul = mulLeft;
					} else if (mulRight == '*' || mulRight == '1') {
						endMul = mulRight;
					} */
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);

					endColor = currNeigh[0][1].color;
					// found end
				} else if (midChar.c == '-' && midChar.color != 0) { 
					// probably found a class
					System.out.println("Found a class!");
					foundEnd = true;
					endColor = midChar.color;
					
					/*
					char mulLeft = currNeigh[2][0].c;	// we are ON the class, so look one line below for multiplicity
					char mulRight = currNeigh[2][2].c;
					if (mulLeft == '*' || mulLeft == '1') {
						endMul = mulLeft;
					} else if (mulRight == '*' || mulRight == '1') {
						endMul = mulRight;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);
	
				} else if (midChar.c == ' ') {
					// dead end?!
				} else {
					// eh?
				}
			}
			
			else if (currentDirection == Direction.SOUTH) {
				if (midChar.c == '|') {
					// check for label name
					if (currNeigh[1][0].c == '}' || currNeigh[1][2].c == '{') {
						// label
						int labelX;
						if (currNeigh[1][0].c == '}') {
							// label to the left
							labelX = currx - 1;
						} else {
							// label to the right
							labelX = currx + 1;
						}
						System.out.println("Found label");
						String lblName = detectLabel(array, labelX, curry, lineColor);
						System.out.println(">>>" + lblName + "<<<");
						lineName = lblName;
					}
					
					// continue
					dirx = 0;
					diry = 1;
				} else if (midChar.c == ')') {
					// continue
					dirx = 0;
					diry = 1;
				} else if (midChar.c == '(') {
					// continue
					dirx = 0;
					diry = 1;
				} else if (midChar.c == '\'') {
					// change direction
					// new direction could be either west/east, so look at 8-neigh and decide
					if (currNeigh[1][0].c == '-') { // left -> go WEST
						currentDirection = Direction.WEST;
						dirx = -1;
						diry = 0;
					} else if (currNeigh[1][2].c == '-') { // right -> go EAST
						currentDirection = Direction.EAST;
						dirx = 1;
						diry = 0;
					} else {
						System.err.println("FAIL");
					}
				} else if (midChar.c == '[' || midChar.c == ']') {
					// signal name
					System.out.println("Found signal!");
					String sigName = processSignal(array, currx, curry, lineColor, true);
					System.out.println(">>>" + sigName + "<<<");
					signalNames.add(sigName);
					lineSignalName = sigName;
					foundEnd = true;
					foundSignal = true;
				} else if (midChar.c == '#') {
					foundEnd = true;
					isContainment = true;
					char mulLeft = currNeigh[1][0].c;
					char mulRight = currNeigh[1][2].c;
					/*
					if (mulLeft == '*' || mulLeft == '1') {
						endMul = mulLeft;
					} else if (mulRight == '*' || mulRight == '1') {
						endMul = mulRight;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);

					endColor = currNeigh[2][1].color;
					// found end
				} else if (midChar.c == '-' && midChar.color != 0) { 
					// probably found a class
					System.out.println("Found a class!");
					foundEnd = true;
					endColor = midChar.color;
					
					/*
					char mulLeft = currNeigh[0][0].c;	// we are ON the class, so look one line above for multiplicity
					char mulRight = currNeigh[0][2].c;
					if (mulLeft == '*' || mulLeft == '1') {
						endMul = mulLeft;
					} else if (mulRight == '*' || mulRight == '1') {
						endMul = mulRight;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);
	
				} else if (midChar.c == ' ') {
					// dead end?!
				} else {
					// eh?
				}
			}
			
			
			else if (currentDirection == Direction.WEST) {
				if (midChar.c == '-') {
					// continue
					dirx = -1;
					diry = 0;
				} else if (midChar.c == ')') {
					// continue
					dirx = -1;
					diry = 0;
				} else if (midChar.c == '(') {
					// continue
					dirx = -1;
					diry = 0;
				} else if (midChar.c == '\'') {
					// change direction
					// new direction is NORTH 
					currentDirection = Direction.NORTH;
					dirx = 0;
					diry = -1;
				} else if (midChar.c == '.') {
					// change direction
					// new direction is SOUTH
					currentDirection = Direction.SOUTH;
					dirx = 0;
					diry = 1;
				} else if (midChar.c == '}') {
					// label
					System.out.println("Found label");
					String lblName = detectLabel(array, currx, curry, lineColor);
					System.out.println(">>>" + lblName + "<<<");
					dirx = -(lblName.length()+2); // next iteration should skip the label name
					diry = 0;
					lineName = lblName;
	
				} else if (midChar.c == ']') {
					// signal name
					System.out.println("Found signal!");
					String sigName = processSignal(array, currx, curry, lineColor, true);
					System.out.println(">>>" + sigName + "<<<");
					signalNames.add(sigName);
					lineSignalName = sigName;
					foundEnd = true;
					foundSignal = true;
				} else if (midChar.c == '#') {
					foundEnd = true;
					isContainment = true;
					char mulTop = currNeigh[0][1].c;
					char mulBot = currNeigh[2][1].c;
					/*
					if (mulTop == '*' || mulTop == '1') {
						endMul = mulTop;
					} else if (mulBot == '*' || mulBot == '1') {
						endMul = mulBot;
					} */
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);

					endColor = currNeigh[1][0].color;
					// found end
				} else if (midChar.c == '|' && midChar.color != 0) { 
					// probably found a class
					System.out.println("Found a class!");
					foundEnd = true;
					endColor = midChar.color;
					
					/*
					char mulTop = currNeigh[0][2].c;  // we are ON the class, so look one col to the right for multiplicity
					char mulBot = currNeigh[2][2].c;
					if (mulTop == '*' || mulTop == '1') {
						endMul = mulTop;
					} else if (mulBot == '*' || mulBot == '1') {
						endMul = mulBot;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);
	
				} else if (midChar.c == ' ') {
					// dead end?!
				} else {
					// eh?
				}
			}
			
			else if (currentDirection == Direction.EAST) {
				if (midChar.c == '-') {
					// continue
					dirx = 1;
					diry = 0;
				} else if (midChar.c == ')') {
					// continue
					dirx = 1;
					diry = 0;
				} else if (midChar.c == '(') {
					// continue
					dirx = 1;
					diry = 0;
				} else if (midChar.c == '\'') {
					// change direction
					// new direction is NORTH 
					currentDirection = Direction.NORTH;
					dirx = 0;
					diry = -1;
				} else if (midChar.c == '.') {
					// change direction
					// new direction is SOUTH
					currentDirection = Direction.SOUTH;
					dirx = 0;
					diry = 1;
				} else if (midChar.c == '{') {
					// label
					System.out.println("Found label");
					String lblName = detectLabel(array, currx, curry, lineColor);
					System.out.println(">>>" + lblName + "<<<");
					dirx = lblName.length()+2; // next iteration should skip the label name
					diry = 0;
					lineName = lblName;
				} else if (midChar.c == '[') {
					// signal name
					System.out.println("Found signal!");
					String sigName = processSignal(array, currx, curry, lineColor, true);
					System.out.println(">>>" + sigName + "<<<");
					lineSignalName = sigName;
					signalNames.add(sigName);
					foundEnd = true;
					foundSignal = true;
				} else if (midChar.c == '#') {
					foundEnd = true;
					isContainment = true;
					char mulTop = currNeigh[0][1].c;
					char mulBot = currNeigh[2][1].c;
					/*
					if (mulTop == '*' || mulTop == '1') {
						endMul = mulTop;
					} else if (mulBot == '*' || mulBot == '1') {
						endMul = mulBot;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);

					endColor = currNeigh[1][2].color;
					// found end
				} else if (midChar.c == '|' && midChar.color != 0) { 
					// probably found a class
					System.out.println("Found a class!");
					foundEnd = true;
					endColor = midChar.color;
					/*
					char mulTop = currNeigh[0][0].c;  // we are ON the class, so look one col to the left for multiplicity
					char mulBot = currNeigh[2][0].c;
					if (mulTop == '*' || mulTop == '1') {
						endMul = mulTop;
					} else if (mulBot == '*' || mulBot == '1') {
						endMul = mulBot;
					}*/
					endMul = getMultiplicity(currx, curry, currentDirection, true, array);

				} else if (midChar.c == ' ') {
					// dead end?!
				} else {
					// eh?
				}
			}
			
			
			array[curry][currx].color = lineColor;
			if (DEBUG) {
				System.out.println("new direction is now " + currentDirection);
			}
	
		}
		
		
		if (foundEnd) {
			array[curry][currx].color = endColor;
			// now found the line
			AscEdge myEdge = new AscEdge(currx, curry, startx, starty, lineColor, endColor, startColor, lineName, lineSignalName); // currx and startx swapped because we start from the arrow and follow the line backwards
			if (foundSignal) {
				myEdge.isSignalEdge = true;
			}
			if (isContainment) {
				myEdge.isContainment = true;
			}
			if (isInheritance) {
				myEdge.isInheritance = true;
			}
			
			myEdge.startMultiplicity = "" + endMul;
			myEdge.endMultiplicity = "" + startMul;
			
			edges.add(myEdge);
			lineNames.add(lineName);
		}
		
		
		System.out.println("End found at " + currx + "," + curry);
		
	}

	/**
		 * Detects a label at the given coordinates
		 * @param array	input array
		 * @param x	x coordinate of the label (either { or })
		 * @param y y coordinte of the label (either { or })
		 * @param color	color of the line the label is attached to
		 * @return	text of the label (excluding { and }) if found, "" otherwise
		 */
		public String detectLabel(AscChar[][] array, int x, int y, int color) {
			final char currentChar = array[y][x].c;
			String labelName = "";
			if (currentChar == '{') {
				// commence detection to the right
				StringBuilder sb = new StringBuilder();
				int count = 255;
				int currx = x+1;
				char currChar;
				while(currx < array[0].length && count-- > 0) {
					currChar = array[y][currx].c;
					array[y][currx].color = color;
					currx++;
					if (currChar == '}') {
						break;
					}
					sb.append(currChar);
				}
				labelName = sb.toString();
			} else if (currentChar == '}') {
				// commence detection to the left
				StringBuilder sb = new StringBuilder();
				int count = 255;
				int currx = x-1;
				char currChar;
				while(currx < array[0].length && count-- > 0) {
					currChar = array[y][currx].c;
					array[y][currx].color = color;
					currx--;
					if (currChar == '{') {
						break;
					}
					sb.append(currChar);
				}
				sb.reverse();
				labelName = sb.toString();
			} else {
				System.err.println("Could not detect label @" + x + "," + y);
			}
			return labelName;
		}

	/**
	 * Detects a signal and processes it
	 * If the signal does not exist, it will be created
	 * If the signal already exists, it will be updated
	 * Calls createOrUpdateSignal
	 * @param array	input array
	 * @param x		x coordinate of the signal (either [ or ])
	 * @param y		y coordinate of the signal (either [ or ])
	 * @param color	color of the edge
	 * @param incoming	is this an incoming edge?
	 * @return	name of the signal
	 */
	public String processSignal(AscChar[][] array, int x, int y, int color, boolean incoming) {
		final char currentChar = array[y][x].c;
		String signalName = "";
		if (currentChar == '[') {
			// commence detection to the right
			StringBuilder sb = new StringBuilder();
			int count = 255;
			int currx = x+1;
			char currChar;
			while(currx < array[0].length && count-- > 0) {
				currChar = array[y][currx].c;
				array[y][currx].color = color;
				currx++;
				if (currChar == ']') {
					break;
				}
				sb.append(currChar);
			}
			signalName = sb.toString();
		} else if (currentChar == ']') {
			// commence detection to the left
			StringBuilder sb = new StringBuilder();
			int count = 255;
			int currx = x-1;
			char currChar;
			while(currx < array[0].length && count-- > 0) {
				currChar = array[y][currx].c;
				array[y][currx].color = color;
				currx--;
				if (currChar == '[') {
					break;
				}
				sb.append(currChar);
			}
			sb.reverse();
			signalName = sb.toString();
		} else {
			System.err.println("Could not detect signal @" + x + "," + y);
			return null;
		}
	
		createOrUpdateSignal(signalName, color, incoming);
		
		return signalName;
	}

	/**
	 * Creates a signal if it doesn't exist yet
	 * Updates the signal if it exists
	 * modifies SIGNALS
	 * @param signalName	name of the signal
	 * @param col			color of the edge
	 * @param incoming		is it an incoming edge?
	 */
	public void createOrUpdateSignal(String signalName, int col, boolean incoming) {
		for (AscSig s : signals) {
			if (s.signalName.equalsIgnoreCase(signalName)) {
				// update signal
				if (incoming) {
					s.incomingColors.add(col);
				} else {
					s.outgoingColors.add(col);
				}
				return;
			}
		}
		// create signal
		AscSig newSignal = new AscSig(signalName);
		if(incoming) {
			newSignal.incomingColors.add(col);
		} else {
			newSignal.outgoingColors.add(col);
		}
		signals.add(newSignal);
		
	}

}
