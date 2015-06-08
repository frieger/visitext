package mutant.parser;

import java.util.ArrayList;
import java.util.List;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscClass;
import mutant.util.Coords;
import mutant.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class ModelElementParser {

	/**
	 * Get model elements in abstract-syntax notation
	 * @param array
	 * @return
	 */
	public static List<AscClass> getModelElementsAbstractSyntax(AscChar[][] array) {
		// TODO: Test & example
		ArrayList<Coords> coords = new ArrayList<Coords>();
		
		for (int i = 0; i < array.length; i++) {
			for (int u = 0; u < array[0].length; u++) {
				if (array[i][u].c == '+') {
					System.out.println("found + at " + u + ", " + i);
					coords.add(new Coords(u, i));
				}
			}
		}
		
		if (coords.size() % 4 != 0) {
			// every class has 4 corners, so there should be a multiple of 4 '+'
			System.err.println("Wrong syntax, missing or extra '+");
		}
		
		ArrayList<AscClass> classes = new ArrayList<AscClass>();
		
		int counter = 1000;
		findClasses:
			while (!coords.isEmpty() && counter-- > 0) {
				Coords topLeftCoord = coords.get(0);	// should be the upper left coordinate
				
				Coords topRightCoord = coords.get(1);
				if (topRightCoord.y != topLeftCoord.y) {
					// this shouldn't happen, as we scan x, then y
					// if this happens, something is seriously wrong, so abort
					System.err.println("Wrong syntax, incorrectly placed '+'");
					System.err.println("Was testing " + topLeftCoord + " and " + topRightCoord);
					break findClasses;
				}
				
				Coords botLeftCoord = null;
				Coords botRightCoord = null;
				int botLeftCoordIdx = 0;
				int botRightCoordIdx = 0;
				// now, coords(0) and coords(1) are on the same line. We know that they are sorted accordingly,
				// so now we need to look for coordinates on a later line that match their x-coordinates
				for (int i = 2; i < coords.size(); i++) {
					Coords testCoord = coords.get(i);
					if (testCoord.x == topLeftCoord.x && botLeftCoord == null) {
						botLeftCoord = testCoord;
						botLeftCoordIdx = i;
					}
					if (botLeftCoord != null && testCoord.y != botLeftCoord.y) {
						// couldn't find bottom right coordinate on the same line as bottom left coordinate
						// --> malformed syntax, so abort
						System.err.println("malformed syntax, couldn't find bottom right '+' on the same line as bottom left '+', was testing " + testCoord);
						
						break findClasses;
					}
					if (testCoord.x == topRightCoord.x) {
						botRightCoord = testCoord;
						botRightCoordIdx = i;
						// now we are done
						break;
					}
				}
				coords.remove(botRightCoordIdx);
				coords.remove(botLeftCoordIdx);
				coords.remove(1);
				coords.remove(0);
				System.out.println("class vertices:" + topLeftCoord + topRightCoord + botLeftCoord + botRightCoord);
				
				Util.printArray(Util.subArray(topLeftCoord.x, topLeftCoord.y, botRightCoord.x, botRightCoord.y, array));
				AscClass newClass = new AscClass(topLeftCoord.x, topLeftCoord.y, botRightCoord.x, botRightCoord.y, array);
				classes.add(newClass);
			}
			
		
		
		
		
		for (AscClass myClass : classes) {
			myClass.classColor = Util.getNextColor();
			// color the original array
			for (int x = myClass.x1; x <= myClass.x2; x++) {
				for (int y = myClass.y1; y <= myClass.y2; y++) {
					array[y][x].color = myClass.classColor;
				}
			}
			
			
			AscChar[][] cla = myClass.img;
			
			// get the class name and type
			String foo = "";
			for (int x = 1; x < cla[1].length-1; x++) {
				foo += cla[1][x].c;
			}
			System.out.println(foo.trim());
	
			if (foo.indexOf(':') != -1) {
				// correct syntax found
				// name : Type
				String myClassName = foo.substring(0, foo.indexOf(':')).trim();
				if (myClassName.isEmpty()) {
					System.err.println("Wrong syntax in class definition for class at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), missing NAME for class. Expected name : Type" );
					System.err.println("ERROR RECOVERY: select generic instance name.");
					myClassName = Util.getNextGenericInstanceName();
				}
				String myClassType = foo.substring(foo.indexOf(':') + 1).trim();
				System.out.println(">" + myClassName + "<");
				System.out.println(">" + myClassType + "<");
				myClass.classType = myClassType;
				myClass.instanceName = myClassName;
			} else {
				System.err.println("Wrong syntax in class definition for class at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), expected name : Type" );
			}
			
			
			// get attributes
			for (int y = 3; y < cla.length - 1; y++) { // attributes begin on the 4th line of a class
				String tmp = "";
				for (int x = 1; x < cla[y].length - 1; x++) {
					tmp += cla[y][x].c;
				}
				tmp = tmp.trim();
				System.out.println(tmp);
				
				if (tmp.trim().isEmpty()) { // empty line, okay
					
				} else {
					if (tmp.indexOf('=') != -1) {
						// possibly correct syntax
						String attribName = tmp.substring(0, tmp.indexOf('=')).trim();
						if (attribName.isEmpty()) {
							System.err.println("Wrong syntax in attribute definition for class at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), missing NAME for attribute. Expected attributeName = value" );
						}
						String attribVal = tmp.substring(tmp.indexOf('=') + 1).trim();
						System.out.println(">" + attribName + "<");
						System.out.println(">" + attribVal + "<");
						myClass.attributes.put(attribName, attribVal);
					} else {
						System.err.println("Wrong syntax in attribute definition for class at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), expected attributeName = value" );
					}
				}
			}
			
		}
		
		return classes;		
	}

	/**
	 * Get classes from the model in concrete-syntax notation
	 * @param array	input model
	 * @return list of the found classes
	 */
	public static List<AscClass> getClassesConcreteSyntax(AscChar[][] array) {
		
		ArrayList<Coords> coords = new ArrayList<Coords>();
		
		for (int i = 0; i < array.length; i++) {
			for (int u = 0; u < array[0].length; u++) {
				if (array[i][u].c == '+') {
					System.out.println("found + at " + u + ", " + i);
					coords.add(new Coords(u, i));
				}
			}
		}
		
		if (coords.size() % 4 != 0) {
			// every class has 4 corners, so there should be a multiple of 4 '+'
			System.err.println("Wrong syntax, missing or extra '+");
		}
		
		ArrayList<AscClass> classes = new ArrayList<AscClass>();
		
		int counter = 1000;
		findClasses:
			while (!coords.isEmpty() && counter-- > 0) {
				Coords topLeftCoord = coords.get(0);	// should be the upper left coordinate
				
				Coords topRightCoord = coords.get(1);
				if (topRightCoord.y != topLeftCoord.y) {
					// this shouldn't happen, as we scan x, then y
					// if this happens, something is seriously wrong, so abort
					System.err.println("Wrong syntax, incorrectly placed '+'");
					System.err.println("Was testing " + topLeftCoord + " and " + topRightCoord);
					break findClasses;
				}
				
				Coords botLeftCoord = null;
				Coords botRightCoord = null;
				int botLeftCoordIdx = 0;
				int botRightCoordIdx = 0;
				// now, coords(0) and coords(1) are on the same line. We know that they are sorted accordingly,
				// so now we need to look for coordinates on a later line that match their x-coordinates
				for (int i = 2; i < coords.size(); i++) {
					Coords testCoord = coords.get(i);
					if (testCoord.x == topLeftCoord.x && botLeftCoord == null) {
						botLeftCoord = testCoord;
						botLeftCoordIdx = i;
					}
					if (botLeftCoord != null && testCoord.y != botLeftCoord.y) {
						// couldn't find bottom right coordinate on the same line as bottom left coordinate
						// --> malformed syntax, so abort
						System.err.println("malformed syntax, couldn't find bottom right '+' on the same line as bottom left '+', was testing " + testCoord);
						
						break findClasses;
					}
					if (testCoord.x == topRightCoord.x) {
						botRightCoord = testCoord;
						botRightCoordIdx = i;
						// now we are done
						break;
					}
				}
				coords.remove(botRightCoordIdx);
				coords.remove(botLeftCoordIdx);
				coords.remove(1);
				coords.remove(0);
				System.out.println("class vertices:" + topLeftCoord + topRightCoord + botLeftCoord + botRightCoord);
				
				Util.printArray(Util.subArray(topLeftCoord.x, topLeftCoord.y, botRightCoord.x, botRightCoord.y, array));
				AscClass newClass = new AscClass(topLeftCoord.x, topLeftCoord.y, botRightCoord.x, botRightCoord.y, array);
				classes.add(newClass);
			}
			
		// TODO: refactor
		// ^^^^^^^^^^^^^^^^^this does not depend on concrete/abstract syntax, so extract to its own method
		
		
		// vvvvvvvvvvvvvvvvvvvvvvvvvvv abstract/concrete syntax specific code begins HERE
		
		for (AscClass myClass : classes) {
			myClass.classColor = Util.getNextColor();
			// color the original array
			for (int x = myClass.x1; x <= myClass.x2; x++) {
				for (int y = myClass.y1; y <= myClass.y2; y++) {
					array[y][x].color = myClass.classColor;
				}
			}
			
			
			//printArrayWithColor(array);
			AscChar[][] cla = myClass.img;
			
			int attribBeginY = 3; // attributes begin on this line
			
			// get the class name and type
			String foo = "";
			for (int x = 1; x < cla[1].length-1; x++) {
				foo += cla[1][x].c;
			}
			System.out.println(foo.trim());
			
			if (foo.contains("<<")) {
				// found guards, so this might be an abstract class, an interface or an enum
				int beginGuardText = foo.lastIndexOf('<');
				int endGuardText = foo.lastIndexOf('>');
				String guardString = foo.substring(beginGuardText+1, endGuardText-1).trim();
				System.out.println("||||" + guardString + "|||");
				if (guardString.equalsIgnoreCase("abstract")) {
					myClass.isAbstractClass = true;
				} else if (guardString.equalsIgnoreCase("enum")) {
					myClass.isEnumClass = true;
				} else if (guardString.equalsIgnoreCase("interface")) {
					myClass.isInterfaceClass = true;
				} else {
					System.err.println("Unknown guard string: " + guardString);
				}
				attribBeginY = 4;
				// parse next line
				foo = "";
				for (int x = 1; x < cla[1].length-1; x++) {
					foo += cla[2][x].c;
				}
	
				
			}
	
			if (foo.trim().isEmpty()) {
				System.out.println("Wrong syntax: Classes need to have a name");
			} else {
				String classTypeString = foo.trim();
				System.out.println("Class type: ----" + classTypeString + "----");
				myClass.classType = classTypeString;
			}
			
			
			// get attributes
			for (int y = attribBeginY; y < cla.length - 1; y++) { // attributes begin on the 4th line of a class
				String tmp = "";
				for (int x = 1; x < cla[y].length - 1; x++) {
					tmp += cla[y][x].c;
				}
				tmp = tmp.trim();
				System.out.println(tmp);
				
				if (tmp.trim().isEmpty()) { // empty line, okay
					
				} else {
					if (tmp.indexOf(':') != -1) {
						// possibly correct syntax
						String attribName = tmp.substring(0, tmp.indexOf(':')).trim();
						if (attribName.isEmpty()) {
							System.err.println(tmp + "||||Wrong syntax in attribute definition for class " + myClass.classType + " at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), missing TYPE for attribute. Expected attributeName : type" );
						}
						String attribType = tmp.substring(tmp.indexOf(':') + 1).trim();
						System.out.println(">" + attribName + "<");
						System.out.println(">" + attribType + "<");
						myClass.attributes.put(attribName, attribType);
					} else if (myClass.isEnumClass){
						String attribName = tmp.trim();
						myClass.attributes.put(attribName, null);
					} else {
						System.err.println(tmp + "||Wrong syntax in attribute definition for class " + myClass.classType + " at (" + myClass.x1 + "," + myClass.y1 + "),(" + myClass.x2 + "," + myClass.y2 + "), expected attributeName : type" );
					}
				}
			}
			
		}
		
		return classes;		
	}

}
