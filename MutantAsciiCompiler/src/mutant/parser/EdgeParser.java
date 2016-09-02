package mutant.parser;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscClass;
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

	public static boolean DEBUG = true;
	private static final int MULTIPLICITY_STRING_LENGTH = 8;
	private final String MULTIPLICITY_REGEX = "\\s*[0-9]+(\\.\\.)([0-9]+|\\*)" + "(\\s*.*)|((\\+|\\|).*)"; // should match multiplicities in the form of '(whitespace)* x..y'   where x = number, y = number or *.   Second part should match greedy whitespace followed by any characters or ( + or | (class vertices and edges) followed by any characters)  
	//private final String MULTIPLICITY_SEARCH_REGEX = "[0-9]+(\\.\\.)([0-9]+|\\*)";	// regex for searching for multiplicities
	
	private final String MULTIPLICITY_SEARCH_REGEX = "([0-9]+(\\.\\.)([0-9]+|\\*))|(1)|(\\*)";	// regex for searching for multiplicities

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
	 * @param array array with the edges. gets recolored to reflect the new edge color
	 * @return		combined edge
	 * @throws Exception if edges could not be combined
	 */
	public static AscEdge combineEdges(AscEdge e1, AscEdge e2, AscChar[][] array) throws Exception {
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
		resEdge.startRolename = startEdge.startRolename;
		resEdge.endRolename = endEdge.endRolename;
		
		if (array != null) {
			Util.recolorArray(array, resEdge.lineColor, startEdge.lineColor, endEdge.lineColor);
		}
		
		return resEdge;
	}

	/**
	 * Creates new edges from signal edges (abbreviated edges)
	 * This will modify the list of edges
	 * @throws Exception if edges could not be connected
	 */
	public void connectSignalEdges(AscChar[][] array) throws Exception {
		System.out.println("now connecting signal edges");
		for (AscSig s : signals) {
			System.out.println("signal: " + s.signalName + " in:" + s.incomingColors + " out:" + s.outgoingColors);
		}
		for (AscSig s : signals) {
			if (s.incomingColors.size() > 1 && s.outgoingColors.size() > 1) {
				// error case, signal has multiple incoming and outgoing edges
				throw new Exception("Signal may only have 1 incoming or outgoing edge");
			} else if (s.incomingColors.size() == 1 && s.outgoingColors.size() == 1) {
				// signal has exactly one incoming and one outgoing edge
				// --> create new edge that connects both edges and delete old edges
				AscEdge e1 = Util.getEdgeForColor(s.incomingColors.get(0), edges);
				AscEdge e2 = Util.getEdgeForColor(s.outgoingColors.get(0), edges);
				AscEdge result = combineEdges(e1, e2, array);
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
					AscEdge result = combineEdges(outgoing, incoming, array);
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
					AscEdge result = combineEdges(outgoing, incoming, array);
					edges.add(result);
					edges.remove(outgoing);
				}
				edges.remove(incoming);
			} else if (s.incomingColors.size() == 0 && s.outgoingColors.size() == 2) {
				// bidirectional edge
				AscEdge edge1 = Util.getEdgeForColor(s.outgoingColors.get(0), edges);
				AscEdge edge2 = Util.getEdgeForColor(s.outgoingColors.get(1), edges);
				
				int tmpCol = edge2.startColor;
				edge2.startColor = edge2.endColor;
				edge2.endColor = tmpCol;
				edge2.endMultiplicity = edge2.startMultiplicity;
				
				System.out.println("bidi!!! e1: sc:" + edge1.startColor + " ec:" + edge1.endColor);
				System.out.println("bidi!!! e2: sc:" + edge2.startColor + " ec:" + edge2.endColor);

				AscEdge result = combineEdges(edge1, edge2, array);
				edges.add(result);
				edges.remove(edge1);
				edges.remove(edge2);
				makeReverseEdge(result, false, false);	// TODO: check if we can always assume false, false here
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
		boolean isAggregation = false;
		
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
		
		String startMul = "";
		startMul = getMultiplicity(startx, starty, currentDirection, false, array, lineColor);
		
		System.err.println("figured out multiplicity: " + startMul);
		
		followLine(startx, starty, currentDirection, array, lineColor, startColor, isContainment, isInheritance, isAggregation, startMul);
		
	}
	
	/**
	 * Add rolenames to all edges
	 * @param array	input array
	 */
	public void getRolenamesForAllEdges(AscChar[][] array) {
		for (AscEdge e : edges) {
			computeRolenamesForEdge(e, array);
		}
	}
	
	/**
	 * Extract rolenames and add them to the edge.
	 * Rolenames have to appear in an area 1 character around the edge. 
	 * The position along the edge determines the rolename's association:
	 * If a rolename is further to the source, it will be the source rolename. If a rolename is further to the target, it will be the target rolename.
	 * @param edge	edge for which the rolename is computed. THIS WILL BE MODIFIED.
	 * @param array	input array
	 */
	private void computeRolenamesForEdge(AscEdge edge, AscChar[][] array) {
		class Pair<A, B> {
			A a;
			B b;
			public Pair(A a, B b) {
				super();
				this.a = a;
				this.b = b;
			}
			
			
		}
		System.out.println("============== edge parser ============== for edge " + edge);
		
		// first, get the uncolored array. this contains regexes and rolenames
		String unprocessedArray = Util.getColorFromArrayAsString(0, array);
		// now we need to find all regexes and remove them
		Pattern regex = Pattern.compile(MULTIPLICITY_SEARCH_REGEX);
		Matcher m = regex.matcher(unprocessedArray);
		while(m.find()) {
			System.out.println("found" + m.group());
			String str = m.group();
			String repl = str.replaceAll(".", " ");
			String tmp1 = unprocessedArray.substring(0, m.start());
			String tmp2 = unprocessedArray.substring(m.end(), unprocessedArray.length());
			String newUnprocessedArray = tmp1 + repl + tmp2;
			unprocessedArray = newUnprocessedArray;
		}
		m = null;
		
		
//		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
//		System.out.println(unprocessedArray);
//		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
		
		// now find all other texts and extract them with their position
		ArrayList<StringPosition> otherLabels = new ArrayList<StringPosition>();
		int line = 0;
		Scanner sc = new Scanner(unprocessedArray);
		while (sc.hasNextLine()) {
			String currLine = sc.nextLine();
			Pattern wordRegex = Pattern.compile("\\w+");
			Matcher match = wordRegex.matcher(currLine);
			while (match.find()) {
				System.out.println("found " + match.group());
				String str = match.group();
				otherLabels.add(new StringPosition(match.start(), line, str));
			}
			line++;
		}
		sc.close();
		
		for (StringPosition sp : otherLabels) {
			System.out.println(sp);
		}
		
		
		// now get the edge array and dilate it
		int[][] arr = Util.getBinaryArrayFromString(' ', Util.getColorFromArrayAsString(edge.lineColor, array));
		//Util.printArray(arr);
		int[][] dilated = Util.dilate(arr);
		//System.out.println(" .... dilated:");
		//Util.printArray(dilated);
		
		Pair<String, Integer> fromRolename = new Pair<String, Integer>(null, Integer.MAX_VALUE);
		Pair<String, Integer> toRolename = new Pair<String, Integer>(null, Integer.MAX_VALUE);
		
		for (StringPosition sp : otherLabels) {
			if ((dilated[sp.y][sp.x] == 1) || (dilated[sp.y][sp.x+sp.string.length()] == 1)) {
				// string is inside edge sphere of influence
				// now check distance to edge ends
				int distanceToStart1 = Util.getDistance(edge.startx, edge.starty, sp.x, sp.y, dilated);
				int distanceToStart2 = Util.getDistance(edge.startx, edge.starty, sp.x+sp.string.length(), sp.y, dilated);
				int distanceToEnd1 = Util.getDistance(edge.endx, edge.endy, sp.x, sp.y, dilated);
				int distanceToEnd2 = Util.getDistance(edge.endx, edge.endy, sp.x+sp.string.length(), sp.y, dilated);
				int distanceToStart = Math.min(distanceToStart1, distanceToStart2);
				int distanceToEnd = Math.min(distanceToEnd1, distanceToEnd2);
				
				System.out.println("distances: " + sp.string + " to start: " + distanceToStart + "  to end: " + distanceToEnd);
				
				if (distanceToStart < distanceToEnd) {
					// start rolename
					if (distanceToStart < fromRolename.b) {
						fromRolename = new Pair<String, Integer>(sp.string, distanceToStart);
					}
				} else {
					// end rolename
					if (distanceToEnd <= toRolename.b) {
						toRolename = new Pair<String, Integer>(sp.string, distanceToEnd);
					}
				}
			}
		}

		// TODO: color start and end rolenames in the array

		
		if (fromRolename.a != null) {
			edge.startRolename  = fromRolename.a;
		}
		if (toRolename.a != null) {
			edge.endRolename = toRolename.a;
		}
		
	}
	
	private class StringPosition {
		final int x;
		final int y;
		final String string;
		
		public StringPosition(int x, int y, String string) {
			super();
			this.x = x;
			this.y = y;
			this.string = string;
		}
		
		@Override
		public String toString() {
			return string + " @ " + x + "," + y;
		}
	}
	
	/**
	 * Get the multiplicity
	 * @param startx		x coordinate of the arrowhead
	 * @param starty		y coordinate of the arrowhead
	 * @param currentDirection	current direction of the edge
	 * @param isOutgoingEdge	set if the edge is an outgoing edge rather than an incoming one. This causes the implementation to look for multiplicities on the other side. 
	 * @param array	input array
	 * @param color	currently not used
	 * @return detected multiplicity as a String
	 */
	public String getMultiplicity(int startx, int starty, Direction currentDirection, boolean isOutgoingEdge, AscChar[][] array, int color) {
		System.err.println("get multiplicity");
		String startMul = "";
		AscChar[][] mulNeigh = Util.subArray(startx-MULTIPLICITY_STRING_LENGTH, starty-1, startx+MULTIPLICITY_STRING_LENGTH, starty+1, array);
		
		int offset = 0;
		if (isOutgoingEdge) {
			offset = 1;
		}
		
		Pattern multiplicitySearchRegex = Pattern.compile(MULTIPLICITY_SEARCH_REGEX);
		
		// if isOutgoingEdge is set, need to reverse W/E 
		if (currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH) {
			// multiplicity is left or right
			int yoffset = 0;
			if (isOutgoingEdge) {
				if (currentDirection == Direction.NORTH) {
					yoffset = 1;
				} else {
					yoffset = -1;
				}
			}
			// TODO: Investigate the problem: Sometimes offset seems to work, sometimes values without offset are correct. This is a very hacky workaround.
			String mulLeft1 = Util.extractString(MULTIPLICITY_STRING_LENGTH, 1+yoffset, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			String mulRight1 = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 1+yoffset, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			String mulLeft2 = Util.extractString(MULTIPLICITY_STRING_LENGTH, 1, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			String mulRight2 = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 1, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			System.err.println("\n\n\nNS_mulleft:~" + mulLeft1 + "~right~" + mulRight1 + "~");
			
			Matcher ml1 = multiplicitySearchRegex.matcher(mulLeft1);
			Matcher ml2 = multiplicitySearchRegex.matcher(mulLeft2);
			Matcher mr1 = multiplicitySearchRegex.matcher(mulRight1);
			Matcher mr2 = multiplicitySearchRegex.matcher(mulRight2);
			if (ml1.find()) {
				startMul = ml1.group();
			} else if (mr1.find()) {
				startMul = mr1.group();
			} else if (ml2.find()) {
				startMul = ml2.group();
			} else if (mr2.find()) {
				startMul = mr2.group();
			} else {
				System.out.println("NS couldn't find anything");
				Util.printArray(mulNeigh);
				System.out.println(startx + " " + starty + " " + currentDirection + " " + isOutgoingEdge);
			}
		} else if ((!isOutgoingEdge && currentDirection == Direction.EAST) | (isOutgoingEdge && currentDirection == Direction.WEST)) {
			// +-----------+ mul
			// | Class     |<-------
			// +-----------+ mul
			String mulTop = Util.extractString(MULTIPLICITY_STRING_LENGTH, 0, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			String mulBot = Util.extractString(MULTIPLICITY_STRING_LENGTH, 2, MULTIPLICITY_STRING_LENGTH, mulNeigh, false);
			mulTop = mulTop.replace('+', ' ');
			mulBot = mulBot.replace('+', ' ');
			mulTop = mulTop.replace('|', ' ');
			mulBot = mulBot.replace('|', ' ');
			System.err.println("\n\n\nE_multop:~" + mulTop + "~bot~" + mulBot + "~");
			
			Matcher mt = multiplicitySearchRegex.matcher(mulTop);
			Matcher mb = multiplicitySearchRegex.matcher(mulBot);
			
			if (mt.find()) {
				startMul = mt.group();
			} else if (mb.find()) {
				startMul = mb.group();
			}
			
		} else if ((!isOutgoingEdge && currentDirection == Direction.WEST) | (isOutgoingEdge && currentDirection == Direction.EAST)) {
			//           mul+----------+
			//------------->| Class    |
			//          mul +----------+
			String mulTop = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 0, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			String mulBot = Util.extractString(MULTIPLICITY_STRING_LENGTH+1, 2, MULTIPLICITY_STRING_LENGTH, mulNeigh, true);
			mulTop = mulTop.replace('+', ' ');
			mulBot = mulBot.replace('+', ' ');
			mulTop = mulTop.replace('|', ' ');
			mulBot = mulBot.replace('|', ' ');

			System.err.println("\n\n\nW_multop:~" + mulTop + "~bot~" + mulBot + "~");
			Matcher mt = multiplicitySearchRegex.matcher(mulTop);
			Matcher mb = multiplicitySearchRegex.matcher(mulBot);
			
			if (mt.find()) {
				startMul = mt.group();
			} else if (mb.find()) {
				startMul = mb.group();
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
	public AscEdge followLine(int startx, int starty, Direction startDir, AscChar[][] array, int lineColor, int _startColor, boolean _isContainment, boolean _isInheritance, boolean _isAggregation, String _startMul) {
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
		boolean isAggregation = _isAggregation; // is the edge an aggregation edge (i.e. one that looks like @--->)
		
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);

					endColor = currNeigh[0][1].color;
					// found end
				} else if (midChar.c == '@') {
					// found end: aggregation
					foundEnd = true;
					isAggregation = true;
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
					endColor = currNeigh[0][1].color;
					
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
	
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);

					endColor = currNeigh[2][1].color;
					// found end
				} else if (midChar.c == '@') {
					// found end: aggregation
					foundEnd = true;
					isAggregation = true;
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);         
					endColor = currNeigh[2][1].color;
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
	
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);

					endColor = currNeigh[1][0].color;
					// found end
				}else if (midChar.c == '@') {
					// found end: aggregation
					foundEnd = true;
					isAggregation = true;
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
					endColor = currNeigh[1][0].color;
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
	
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);

					endColor = currNeigh[1][2].color;
					// found end
				} else if (midChar.c == '@') {
					// found end: aggregation
					foundEnd = true;
					isAggregation = true;
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);
					endColor = currNeigh[1][2].color;
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
					endMul = getMultiplicity(currx, curry, currentDirection, true, array, lineColor);

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
		
		AscEdge myEdge = null;
		
		if (foundEnd) {
			array[curry][currx].color = endColor;
			// now found the line
			myEdge = new AscEdge(currx, curry, startx, starty, lineColor, endColor, startColor, lineName, lineSignalName); // currx and startx swapped because we start from the arrow and follow the line backwards
			if (foundSignal) {
				myEdge.isSignalEdge = true;
			}
			if (isContainment) {
				myEdge.isContainment = true;
			}
			if (isInheritance) {
				myEdge.isInheritance = true;
			}
			if (isAggregation) {
				myEdge.isAggregation = true;
				System.err.println("edge is aggregation");
			}
			
			myEdge.startMultiplicity = "" + endMul;
			myEdge.endMultiplicity = "" + startMul;
			
			edges.add(myEdge);
			lineNames.add(lineName);
		}
		
		
		System.out.println("End found at " + currx + "," + curry);
		
		return myEdge;
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
		System.out.println("process signal");
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
		System.out.println("create or update signal " + signalName);
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

	/**
	 * Create a reverse/opposite edge from a provided edge and add it to the edge list
	 * @param edge	Edge that should get an opposite edge. Usually, this is one part of a bidirectional edge.
	 * @param reverseIsContainment	if set, the reverse edge will be a containment
	 * @param reverseIsAggregation	if set, the reverse edge will be an aggregation
	 */
	public void makeReverseEdge(AscEdge edge, boolean reverseIsContainment, boolean reverseIsAggregation) {
		
		System.err.println("make reverse edge. this edge has #=" + edge.isContainment + " @=" + edge.isAggregation + "    rev#=" + reverseIsContainment + " rev@=" + reverseIsAggregation);
		
		//AscEdge reverseEdge = new AscEdge(startx, starty, endx, endy, lineColor, startColor, endColor, label, signalName)
		AscEdge reverseEdge = new AscEdge(edge.endx, edge.endy, edge.startx, edge.starty, edge.lineColor, edge.endColor, edge.startColor, edge.label, edge.signalName);
		reverseEdge.startMultiplicity = edge.endMultiplicity;
		reverseEdge.endMultiplicity = edge.startMultiplicity;
		reverseEdge.oppositeEdge = edge;
		reverseEdge.isContainment = reverseIsContainment;
		reverseEdge.isAggregation = reverseIsAggregation;
		edge.oppositeEdge = reverseEdge;
		edges.add(reverseEdge);
	}
}
