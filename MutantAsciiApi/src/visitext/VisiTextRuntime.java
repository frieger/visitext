package visitext;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.emf.ecore.EPackage;

public class VisiTextRuntime {
	public static EPackage getPackage(String packageName) {
		System.out.println("visitext runtime get package");
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callerMethod = ste[2];
		
		String className = callerMethod.getClassName();
		String methodName = callerMethod.getMethodName();
		
		
		String javaSourceFileName = className.replace('.', '/') + ".java";
		System.out.println("java source file name: " + javaSourceFileName);
		
		Object o = new Object();
		InputStream in = o.getClass().getClassLoader().getResourceAsStream(className);
		
		char c;
		try {
			while((c = (char) in.read()) != -1) {
				System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//InputStream in = getClassLoader().getResourceAs
		
		//String modelName = className + "_" + methodName.toLowerCase() + "_" + packageName;
		
		//EPackage retVal = (EPackage) loadMutantResource(modelName).getContents().get(0);
		//return retVal;
		
		return null;
	}
}
