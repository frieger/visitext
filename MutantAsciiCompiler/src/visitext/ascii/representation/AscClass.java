package visitext.ascii.representation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import visitext.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class AscClass {
	/**
	 * Information about classes in the Ascii representation
	 */
	public AscChar[][] img;			// subarray of the class image
	public int classColor;
	public int x1;
	public int y1;
	public int x2;
	public int y2;
	
	public String classType;		// name of the class type
	public String instanceName;		// name of the instance (for abstract syntax)
	
	public Map<String, String> attributes = new LinkedHashMap<String, String>();
	public List<AscMethod> methods = new ArrayList<AscMethod>();
	public AscClass(int x1, int y1, int x2, int y2, AscChar[][] array) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.img = Util.subArray(x1, y1, x2, y2, array);
	}
	
	// attributes for CONCRETE SYNTAX
	// XXX: maybe we should refactor this
	// vvvvvvvvvvvvvvvvvvvvvvvvvvv
	public boolean isAbstractClass;
	public boolean isInterfaceClass;
	public boolean isEnumClass;
	// ^^^^^^^^^^^^^^^^^^^^^^^^^^^
	
	@Override
	public String toString() {
		return "{CLASS: " + instanceName + " : " + classType + " (col:" + classColor + ")" + "@(" + x1 + "," + y1 + "),(" + x2 + "," + y2 + ")" + ", attribs:" + attributes + "}";
	}
}