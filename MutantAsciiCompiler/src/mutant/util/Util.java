package mutant.util;

import java.util.ArrayList;
import java.util.List;

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

	/*
	public static List<AscChar[]> getAreaAroundPoint(int x, int y, AscChar[][] array) {
		List<AscChar[]> res = new ArrayList<AscChar[]>();
		
		AscChar[] resN = new AscChar[10];
		for(int yy = 0; yy < 10; yy++)
		
		return res;
	} */
	
	
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
		
		//int incr;
		/*if (extractFromLeft) {
			incr = -1;
		} else {
			incr = 1;
		}
		
		for (int i = 0; i < length; i++) {
			if ((startx+i*incr) < 0 || (startx+i*incr) > array[0].length) {
				break;
			}
			tmp += array[starty][startx+i*incr];
		}
		
		return tmp;*/
		
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


}
