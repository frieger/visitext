package mutant.ascii.representation;

import java.util.HashMap;

public class AscMethod {
	private String methodName;
	private HashMap<String, String> methodParams;
	private String methodReturnType;
	public AscMethod(String methodName, HashMap<String, String> methodParams,
			String methodReturnType) {
		super();
		this.methodName = methodName;
		this.methodParams = methodParams;
		this.methodReturnType = methodReturnType;
	}
	public AscMethod(String methodName, HashMap<String, String> methodParams) {
		super();
		this.methodName = methodName;
		this.methodParams = methodParams;
	}
	public AscMethod(String methodName, String methodReturnType) {
		super();
		this.methodName = methodName;
		this.methodReturnType = methodReturnType;
	}
	
	public AscMethod(String methodName) {
		super();
		this.methodName = methodName;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public HashMap<String, String> getMethodParams() {
		return methodParams;
	}
	public void setMethodParams(HashMap<String, String> methodParams) {
		this.methodParams = methodParams;
	}
	public String getMethodReturnType() {
		return methodReturnType;
	}
	public void setMethodReturnType(String methodReturnType) {
		this.methodReturnType = methodReturnType;
	}
	@Override
	public String toString() {
		return "AscMethod [methodName=" + methodName + ", methodParams="
				+ methodParams + ", methodReturnType=" + methodReturnType + "]";
	}
	
	
}
