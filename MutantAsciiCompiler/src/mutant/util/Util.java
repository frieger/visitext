package mutant.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscClass;
import mutant.ascii.representation.AscEdge;

/**
 * 
 * @author Felix Rieger
 *
 */
public class Util {
	
	private static int nextColor = 1;
	private static int instanceName = 1;
	public static boolean DEBUG = false;


	/**
	 * Gets the 8-neighborhood of a coordinate in the input array, i.e. a 3x3 subarray centered on the (x,y) coordinate given
	 * @param x		x coordinate
	 * @param y		y coordinate
	 * @param array	input array
	 * @return	3x3 subarray centered on (x,y). If (x,y) is at the array boundaries, result will be padded.
	 */
	public static AscChar[][] get8Neigh(int x, int y, AscChar[][] array) {
		if (DEBUG) {
			System.out.println("requested x: " + x + "requested y: " + y + "  array size:" + array.length + " " + array[0].length);
		}
		
		final int ymax = array.length-1;
		final int xmax = array[0].length-1;
		
		AscChar[][] result = new AscChar[3][3];
		if (x != -1 && y != -1) {
			result[1][1] = array[y][x];
		} else if (x == -1 && y == -1) {
			result[1][1] = new AscChar(' ');
		} else if (x == -1) {
			result[1][1] = new AscChar(' ');
		} else if (y == -1) {
			result[1][1] = new AscChar(' ');
		}
		if (x==0) {
			// left border, pad result array
			result[0][0] = new AscChar(' ');
			result[1][0] = new AscChar(' ');
			result[2][0] = new AscChar(' ');
		} else {
			if (y != 0) {
				result[0][0] = array[y-1][x-1];	//! y may be 0
			}
			result[1][0] = array[y][x-1];
			if (y != ymax) {
				result[2][0] = array[y+1][x-1]; //! y max be max
			}
		}
		if (y==0) {
			// top border, pad result array
			result[0][0] = new AscChar(' ');
			result[0][1] = new AscChar(' ');
			result[0][2] = new AscChar(' ');
		} else {
			if (x != 0) {
				result[0][0] = array[y-1][x-1]; // ! x may be 0
			}
			result[0][1] = array[y-1][x];
			if (x != xmax) {
				result[0][2] = array[y-1][x+1]; // !x may be max
			}
		}
		
		if (x==xmax) {
			// right border, pad result array
			result[0][2] = new AscChar(' ');
			result[1][2] = new AscChar(' ');
			result[2][2] = new AscChar(' ');
		} else {
			if (y != 0) {
				result[0][2] = array[y-1][x+1];	//! y may be 0
			}
			result[1][2] = array[y][x+1];
			if (y != ymax) {
				result[2][2] = array[y+1][x+1]; //! y may be max
			}
		}
		if (y==ymax) {
			//bottom border, pad result array
			result[2][0] = new AscChar(' ');
			result[2][1] = new AscChar(' ');
			result[2][2] = new AscChar(' ');
		} else {
			if (x != 0) {
				result[2][0] = array[y+1][x-1];	// ! x may be 0
			}
			result[2][1] = array[y+1][x];
			if (x != xmax) {
				result[2][2] = array[y+1][x+1];
			}
		}
		
		return result;
	}
	
	
	/**
	 * Extracts sub-array
	 * @param x1 top left coordinates
	 * @param y1 top left coordinates
	 * @param x2 bottom right coordinates
	 * @param y2 bottom right coordinates
	 * @param origin input array
	 * @return subarray -- indices out of bounds are padded with space ' '
	 */
	public static AscChar[][] subArray(int x1, int y1, int x2, int y2, AscChar[][] origin) {
		System.out.println("subarray: " + x1 + ", " + y1 + " -- " + x2 + "," + y2 + "   origin:" + origin.length + "," + origin[0].length);
		AscChar[][] array = new AscChar[y2-y1+1][x2-x1+1];
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if ((y1+y < origin.length) &&
						(x1+x < origin[0].length) &&
						(y1+y >= 0) &&
						(x1+x >= 0)) {
					//System.out.println("!");
					array[y][x] = origin[y1+y][x1+x];
				} else {
					// pad array
					array[y][x] = new AscChar(' ');
				}
			}
		}
		return array;
	}

	/**
	 * Extracts sub-array
	 * @param x1 top left coordinates
	 * @param y1 top left coordinates
	 * @param x2 bottom right coordinates
	 * @param y2 bottom right coordinates
	 * @param origin input array
	 * @param padValue value used for padding
	 * @return subarray -- indices out of bounds are padded with space ' '
	 */
	public static int[][] subArray(int x1, int y1, int x2, int y2, int[][] origin, int padValue) {
		int[][] array = new int[y2-y1+1][x2-x1+1];
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if ((y1+y < origin.length) &&
						(x1+x < origin[0].length) &&
						(y1+y >= 0) &&
						(x1+x >= 0)) {
					//System.out.println("!");
					array[y][x] = origin[y1+y][x1+x];
				} else {
					// pad array
					array[y][x] = padValue;
				}
			}
		}
		return array;
	}
	
	/**
	 * Get the next free color
	 * @return
	 */
	public static int getNextColor() {
		return nextColor++;
	}
	
	
	/**
	 * Get the next free generic instance name (for abstract syntax)
	 * @return
	 */
	public static String getNextGenericInstanceName() {
		return "i_" + instanceName++;
	}

	/**
	 * Print array
	 * @param array
	 */
	public static void printArray(AscChar[][] array) {
		for (int yc = 0; yc < array.length; yc++) {
			for (int xc = 0; xc < array[0].length; xc++) {
				System.out.print(array[yc][xc]);
			}
			System.out.println("");
		}
	}

	/**
	 * Print array and colors
	 * @param array
	 */
	public static void printArrayWithColor(AscChar[][] array) {
		for (int yc = 0; yc < array.length; yc++) {
			for (int xc = 0; xc < array[0].length; xc++) {
				System.out.print("" + array[yc][xc].c + "" + array[yc][xc].color);
			}
			System.out.println("");
		}
	}

	/**
	 * Returns the class with the specified color from the list
	 * @param color	color of the class to find
	 * @param classes	list of classes
	 * @return	first class with the specified color, or null if no class with the specified color exists
	 */
	public static AscClass getClassForColor(int color, List<AscClass> classes) {
		for (AscClass c : classes) {
			if (c.classColor == color) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Returns the edge with the specified color from the list
	 * @param color	color of the edge to find
	 * @param edges	list of edges
	 * @return	first edge with the specified color, or null of no edge with the specified color exists.
	 */
	public static AscEdge getEdgeForColor(int color, List<AscEdge> edges) {
		for (AscEdge e : edges) {
			if (e.lineColor == color) {
				return e;
			}
		}
		return null;
	}
	
	/**
	 * Extracts a string from an AscChar array. Strings are always extracted horizontally.
	 * @param startx x position of the array where the string begins or ends, depending on the reverse flag
	 * @param starty y position of the array where the string begins and ends
	 * @param length length of string to extract.
	 * @param array	array from which the string is to be extracted
	 * @param extractFromLeft if this flag is set, extraction is done from x-length to x. If this flag is cleared, extraction is done from x to x+length
	 * @return
	 */
	public static String extractString(int startx, int starty, int length, AscChar[][] array, boolean extractFromLeft) {
		String tmp = "";
		
		if (extractFromLeft) {
			for(int i = 0; i < length; i++) {
				if ((startx-i) < 0 || (startx-i) > array[0].length) {
					break;
				}
				tmp += array[starty][startx-length + i];
			}
		} else {
			for (int i = 0; i < length; i++) {
				if ((startx+i) < 0 || (startx+1) > array[0].length) {
					break;
				}
				tmp += array[starty][startx + i];
			}
		}
		
		return tmp;
	}


	/**
	 * Extract a String from an array. This string is empty except where the array's color matches the color parameter
	 * @param color color to extract
	 * @param array array
	 * @return a string that has the dimensions of the array, that is a copy of the array where the array's color matches the color parameter, and empty otherwise
	 */
	public static String getColorFromArrayAsString(int color, AscChar[][] array) {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if (array[y][x].color == color) {
					sb.append(array[y][x].c);
				} else {
					sb.append(' ');
				}
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * Create a binary array from a string
	 * @param emptyChar character to use as the "empty"/whitespace character. All other characters are converted to a set pixel
	 * @param str 
	 * @return
	 */
	public static int[][] getBinaryArrayFromString(char emptyChar, String str) {
		int lines = 0;
		int maxLength = 0;
		Scanner sc = new Scanner(str);
		while(sc.hasNextLine()) {
			lines++;
			String lineString = sc.nextLine();
			if (maxLength < lineString.length()) {
				maxLength = lineString.length();
			}
		}
		sc.close();
		
		int[][] array = new int[lines][maxLength];

		sc = new Scanner(str);
		int currLine = 0;
		while(sc.hasNextLine()) {
			String lineString = sc.nextLine();
			int currChar = 0;
			for (char c : lineString.toCharArray()) {
				if (c != emptyChar) {
					array[currLine][currChar] = 1;
				} else {
					array[currLine][currChar] = 0;
				}
				currChar++;
			}
			currLine++;
		}
		sc.close();
		

		
		return array;
		
	}
	
	/**
	 * print an array
	 * @param array
	 */
	public static void printArray(int[][] array) {
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				System.out.print(array[y][x]);
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * print an array, cutting off after a number of characters
	 * @param array
	 * @param length printed value is cut off after this many characters
	 */
	public static void printArray(int[][] array, int length) {
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				String str = array[y][x] + "                                                   ";
				
				System.out.print(str.substring(0, length));
				System.out.print(" ");
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * "dilate" operator
	 * @param array 0 denotes empty pixel, 1 denotes set pixel
	 * @return
	 */
	public static int[][] dilate(int[][] array) {
		
		int[][] tmpArr = new int[array.length][array[0].length];
		
		for (int y = 0; y < tmpArr.length; y++) {
			for (int x = 0; x < tmpArr[0].length; x++) {
				int[][] neigh = subArray(x-1,y-1,x+1,y+1,array,0);
				if (arraySum(neigh) > 0) {
					// at least one set bit in the array -> dilate
					tmpArr[y][x] = 1;
				} else {
					tmpArr[y][x] = 0;
				}
			}
		}
		
		return tmpArr;
	}
	
	/**
	 * compute the sum of values in an array
	 * @param array
	 * @return
	 */
	public static int arraySum(int[][] array) {
		int sum = 0;
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				sum += array[y][x];
			}
		}
		return sum;
	}
	
	
	/**
	 * Get the distance from one point to another
	 * @param fromx
	 * @param fromy
	 * @param tox
	 * @param toy
	 * @param array		1 for vertices, 0 otherwise
	 * @return
	 */
	public static int getDistance(int fromx, int fromy, int tox, int toy, int[][] array) {
		// TODO: use caching
		class Vertex {
			int x;
			int y;
			
			public Vertex(int x, int y) {
				super();
				this.x = x;
				this.y = y;
			}
			
			public boolean adjacentTo(Vertex v) {
				if (x == v.x) {
					return (y == v.y + 1 || y == v.y - 1);
				} else if (y == v.y) {
					return (x == v.x + 1 || x == v.x - 1);
				} else {
					return false;
				}
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + x;
				result = prime * result + y;
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Vertex other = (Vertex) obj;
				if (x != other.x)
					return false;
				if (y != other.y)
					return false;
				return true;
			}
			
			
		}
		
		// build the list of vertices
		ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
		
		for (int y = 0; y < array.length; y++) {
			for (int x = 0; x < array[0].length; x++) {
				if (array[y][x] == 1) {
					vertexList.add(new Vertex(x, y));
				}
			}
		}		
		
		
		int[][] distances = new int[vertexList.size()][vertexList.size()];
		
		// initialize distances to infinity
		for (int vertexFrom = 0; vertexFrom < distances.length; vertexFrom++) {
			for (int vertexTo = 0; vertexTo < distances[0].length; vertexTo++) {
				distances[vertexFrom][vertexTo] = Integer.MAX_VALUE / 2 - 1;
				if (vertexTo == vertexFrom) {
					distances[vertexFrom][vertexTo] = 0;
				}
			}
		}

		// edge distances
		for (int vertexFrom = 0; vertexFrom < distances.length; vertexFrom++) {
			for (int vertexTo = 0; vertexTo < distances[0].length; vertexTo++) {
				Vertex vFrom = vertexList.get(vertexFrom);
				Vertex vTo = vertexList.get(vertexTo);
				if (vFrom.adjacentTo(vTo)) {
					distances[vertexFrom][vertexTo] = 1;
				}
			}
		}

		// compute distance
		for (int i = 0; i < distances.length; i++) {
			for (int u = 0; u < distances.length; u++) {
				for (int p = 0; p < distances.length; p++) {
					if (distances[i][u] > distances[i][p] + distances[p][u]) {
						distances[i][u] = distances[i][p] + distances[p][u];
					}
				}
			}
		}
		
		Vertex fromVertex = new Vertex(fromx, fromy);
		Vertex toVertex = new Vertex(tox, toy);
		int fromVertexIndex = vertexList.indexOf(fromVertex);
		int toVertexIndex = vertexList.indexOf(toVertex);
		
		if (fromVertexIndex == -1 || toVertexIndex == -1) {
			return Integer.MAX_VALUE;
		}
		
		return distances[fromVertexIndex][toVertexIndex];
		
	}
	
	public static void recolorArray(AscChar[][] array, int newColor, int ... colors) {
		for (int y = 0; y < array.length; y++) {
			for (int x = 0 ; x < array[0].length; x++) {
				for (int col : colors) {
					if (array[y][x].color == col) {
						// recolor
						array[y][x].color = newColor;
					}
				}
			}
		}
	}
}
