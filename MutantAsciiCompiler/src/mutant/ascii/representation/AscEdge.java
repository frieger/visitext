package mutant.ascii.representation;

/**
 * 
 * @author Felix Rieger
 *
 */
public class AscEdge {
	/**
	 * Information about edges in the ASCII representation
	 */
	public int startx;		// start point
	public int starty;
	public int endx;		// end point
	public int endy;
	public String label;	// edge label
	public String signalName; // name of the signal (abbreviated edge) this edge is connected to
	public boolean isSignalEdge = false;
	public boolean isInheritance = false;
	public boolean isContainment = false;	// containment: #
	public boolean isAggregation = false;	// aggregation: @
	public int lineColor;		// color of the edge
	public int startColor;		// color of the start element
	public int endColor;		// color of the end element
	
	public String startMultiplicity = "";
	public String endMultiplicity = "";
	
	public AscEdge oppositeEdge = null;
	
	
	public AscEdge(int startx, int starty, int endx, int endy, int lineColor, int startColor, int endColor, String label, String signalName) {
		this.startx = startx;
		this.starty = starty;
		this.endx = endx;
		this.endy = endy;
		this.label = label;
		this.signalName = signalName;
		this.lineColor = lineColor;
		this.startColor = startColor;
		this.endColor = endColor;
	}
}