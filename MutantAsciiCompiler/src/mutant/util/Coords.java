package mutant.util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class Coords {
	public final int x;
	public final int y;
	public final int lineNumberFromSourceFile;
	
	
	
	public Coords(int x, int y, int lineNumerFromSourceFile) {
		super();
		this.x = x;
		this.y = y;
		this.lineNumberFromSourceFile = lineNumerFromSourceFile;
	}

	public Coords(int x, int y) {
		this.x = x;
		this.y = y;
		lineNumberFromSourceFile = -1;
	}
	
	@Override
	public String toString() {
		if (lineNumberFromSourceFile == -1) {
			return "(" + x + "," + y + ")";
		} else {
			return "(" + x + "," + y + ")  on line " + lineNumberFromSourceFile;
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
		Coords other = (Coords) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	
	
}