package visitext.main;

public class VisiTextCompilationInfo {
	public final String fileName;
	public final String methodName;
	
	public final VisiTextModelInfo modelInfo;
	
	public final double compileTime;
	public final int numberOfClasses;
	public final int numberOfAttributes;
	public final int numberOfOperations;
	public final int numberOfAssociations;
	public final int numberOfGeneralizations;
	public VisiTextCompilationInfo(String fileName, String methodName,
			VisiTextModelInfo modelInfo, double compileTime, int numberOfClasses,
			int numberOfAttributes, int numberOfOperations,
			int numberOfAssociations, int numberOfGeneralizations) {
		super();
		this.fileName = fileName;
		this.methodName = methodName;
		this.modelInfo = modelInfo;
		this.compileTime = compileTime;
		this.numberOfClasses = numberOfClasses;
		this.numberOfAttributes = numberOfAttributes;
		this.numberOfOperations = numberOfOperations;
		this.numberOfAssociations = numberOfAssociations;
		this.numberOfGeneralizations = numberOfGeneralizations;
	}
	
	public static String getCsvHeader() {
		return "filename ; methodname ; modeltype ; compiletime ; numberOfClasses ; numberOfAttributes ; numberOfOperations ; numberOfAssociations ; numberOfGeneralizations\n"; 
	}
	
	private static String SEP = " ; ";
	public String getCsvContents() {
		return fileName + SEP + 
				methodName + SEP + 
				modelInfo.mutantType.name() + SEP +
				compileTime + SEP +
				numberOfClasses + SEP + 
				numberOfAttributes + SEP + 
				numberOfOperations + SEP +
				numberOfAssociations + SEP +
				numberOfGeneralizations + "\n";
	}
}
