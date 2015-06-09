package mutant.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;

import mutant.ascii.representation.AscChar;
import mutant.ascii.representation.AscClass;
import mutant.generator.EcoreGenerator;
import mutant.main.MutantModelInfo.MutantType;
import mutant.parser.AsciiParser;
import mutant.parser.EdgeParser;
import mutant.parser.ModelElementParser;
import mutant.util.Coords;
import mutant.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class MutantCompiler {
	
	private final static String modelDirectory = "mutant";
	public static boolean DEBUG = false;
	
	private static String basePath;
	private static String modelPath;
	
	public static void main(String[] args) {
		boolean hasMetamodels = false;
		if (args.length == 0) {
			System.out.println("MUTANT compiler. Arguments: MutantCompiler.jar COMPILE Java-project-base-path <Ecore-models-base-path> <DEBUG> ");
		}
		
		// Base path: Path where all source files are in, will be traversed recursively
		basePath = args[0];
		if (basePath.endsWith(File.separator)) {
			basePath = basePath.substring(0, basePath.lastIndexOf(File.separator) - 1);
		}
		
		// Model path: Path where all ecore metamodels are in, will not be traversed.
		if (args.length > 1) {
			if (args[1].equalsIgnoreCase("debug")) {
				DEBUG = true;
			} else {
				hasMetamodels = true;
				modelPath = args[1];
				if (modelPath.endsWith(File.separator)) {
					modelPath = modelPath.substring(0, modelPath.lastIndexOf(File.separator));
				}
			}
			
			System.out.println(args[1]);
		}
		
		if ((args.length > 2) && (args[2].equalsIgnoreCase("debug"))) {
			DEBUG = true;
		}
		
		
		System.out.println("model path:" + modelPath);
		System.out.println("base path:" + basePath);
		if (hasMetamodels) {
			System.out.println("Loading all metamodels");
			EcoreGenerator.loadAllMetaModelsFromPath(new File(modelPath));
			System.out.println("Done loading all metamodels");
		}
		
		traverseDirectory(new File(basePath), "java");
	}
	
	public static void traverseDirectory(File start, String fileExtension) {
		if (start.isDirectory()) {
			if (DEBUG) {
				System.out.println("MUTANT: directory" + start.toString());
			}
			// parse the files in the directory and all the directories recursively
			File[] files = start.listFiles();
			for (File f : files) {
				traverseDirectory(f, fileExtension);
			}
		} else {
			// parse the file
			String filename = start.getPath();
			String fileExt = filename.substring(filename.lastIndexOf(".") + 1);
			System.out.println(filename + " " + fileExt);
			if (fileExt.equalsIgnoreCase(fileExtension)) {
				Map<String, String> mutantMethods = parseJavadocMutantFile(start);
				String javaFileName = start.getName().substring(0, start.getName().lastIndexOf("."));
				compileMutant(javaFileName, mutantMethods);
			}
		}
	}
	
	private static Map<String, String> parseJavadocMutantFile(File sourceFile) {
		System.out.println("MUTANT: Trying to parse " + sourceFile.getPath());
		Map<String, String> methodNameToMutantContentsMap = null;
		try {
			Scanner sc = new Scanner(sourceFile);
			methodNameToMutantContentsMap = new HashMap<String, String>(); 
			StringBuilder sb = new StringBuilder();

			String currentMutantModel = null;
			
			boolean inMutantModel = false;
			boolean inJavadocComment = false;
			boolean foundMutantModel = false;
			String currentMethodName = null;
			while (sc.hasNextLine()) {
				String currLine = sc.nextLine();
				if (DEBUG) {
					System.out.println("line: " + currLine);
				}
				// are we in a comment
				if (currLine.trim().startsWith("/**")) {
					inJavadocComment = true;
				}
				
				if (inJavadocComment) {
					if (currLine.contains("@InputModel")) {
						inMutantModel = true;
					}
					if (currLine.trim().contains("@ModelEnd")) {
						inMutantModel = false;
						foundMutantModel = true;
						currentMutantModel = sb.toString();
						sb = new StringBuilder();
					}
					if (inMutantModel) {
						sb.append(currLine);
						sb.append("\n");
					}
				} else if (currLine.trim().isEmpty()) {
					// do nothing
				} else {
					if (!foundMutantModel) {
						continue;	// if we haven't found a MUTANT model, there's no use trying to parse the method
					}
					// now look for the method head
					String[] tokens = currLine.split("\\s+");
					int genericTypeDepth = 0; // generic types are the only occurence of spaces we need to handle specially
					boolean foundMethodReturnType = false;
					boolean foundMethod = false;
					for (String t : tokens) {
						System.out.println(":" + t + ":");
						if (t.contains("<")) { // handle generic types
							genericTypeDepth++;
						}
						if (t.contains(">")) { // handle generic types
							genericTypeDepth--;
						}

						
						if (t.trim().equalsIgnoreCase("class")) { // not a method
							break;
						}
						
						if (t.trim().startsWith("@")) { // not a method
							break;
						}
						
						if (t.contains("=")) { // not a method
							break;
						}
						
						if (genericTypeDepth == 0) { // not in generic type
							if (!foundMethodReturnType) { // iterate tokens until we find the return type
								if ((t.trim().equalsIgnoreCase("private")) || (t.trim().equalsIgnoreCase("public")) || (t.trim().equalsIgnoreCase("protected")) 
										|| (t.trim().equalsIgnoreCase("abstract")) || (t.trim().equalsIgnoreCase("static")) || (t.trim().equalsIgnoreCase("final"))
										|| (t.trim().equalsIgnoreCase("synchronized")) || (t.trim().equalsIgnoreCase("native")) || (t.trim().equalsIgnoreCase("strictfp"))) {
									// method modifiers
									// do nothing
									System.out.println("found method modifier");
								} else if (t.trim().equalsIgnoreCase("void")) {
									// return type
									foundMethodReturnType = true;
								} else if (t.trim().isEmpty()){
									// do nothing
								} else {
									// this should be the return type
									foundMethodReturnType = true;
								}
							} else { // in the last iteration, we found the return type. Now this token should be the method name
								System.out.println("this token should be the method name:" + t + "<");
								if ((t.trim().indexOf('(')) == -1) {
									// no parens, so method name is already okay
									currentMethodName = t.trim().toLowerCase();
								} else {
									// trim parens from method name.
									currentMethodName = t.trim().substring(0, t.trim().indexOf('(')).toLowerCase();
								}
								foundMethod = true;
								methodNameToMutantContentsMap.put(currentMethodName, currentMutantModel);
								foundMutantModel = false;
								break; // no need to parse more tokens
							}
						}
						
					}
				}
								
				if (currLine.trim().contains("*/")) {
					inJavadocComment = false;
					if (inMutantModel) {
						foundMutantModel = true;
					}
					inMutantModel = false;
					currentMutantModel = sb.toString();
					int commentEnd = currentMutantModel.lastIndexOf("*/");
					if (commentEnd != -1) {
						currentMutantModel = currentMutantModel.substring(0, commentEnd);
					}
					sb = new StringBuilder();
				}

				
			}
			
			sc.close();
			
			if (methodNameToMutantContentsMap.isEmpty()) {
				System.out.println("No MUTANT models in file");
			} else {
				System.out.println("The following MUTANT models have been found");
				for (Entry<String, String> e : methodNameToMutantContentsMap.entrySet()) {
					System.out.println("\n");
					System.out.println("Method name:" + e.getKey() + "<");
					System.out.println("MUTANT model:\n" + e.getValue() + "<");
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return methodNameToMutantContentsMap;
	}
	
	private static MutantModelInfo getMutantModelInfo(String mutantContents) {
		int inputModelIndex = mutantContents.indexOf("@InputModel");
		int equalSignIndex = mutantContents.indexOf("=", inputModelIndex);
		String modelDefSubstr = mutantContents.substring(inputModelIndex, equalSignIndex);
		String[] tokens = modelDefSubstr.split("\\s+");
		
		MutantModelInfo info = null;
		if (tokens.length == 3) { // concrete syntax: class
			info = new MutantModelInfo(MutantModelInfo.MutantType.CLASS, null, tokens[1], tokens[2]);
		} else if (tokens.length == 4) { // abstract syntax
			
			String nsUri = tokens[1];
			int beginIdx = nsUri.indexOf('"');
			int endIdx = nsUri.lastIndexOf('"');
			nsUri = nsUri.substring(beginIdx+1, endIdx);
			info = new MutantModelInfo(MutantModelInfo.MutantType.ABSTRACT, nsUri, tokens[2], tokens[3]);
		}
		
		return info;
	}
	
	
	
	private static void compileMutant(String filename, Map<String, String> methodNameToMutantContentsMap) {
		try {
			for (Entry<String, String> e : methodNameToMutantContentsMap.entrySet()) {
				System.out.println("---\n" + e.getKey());
				MutantModelInfo info = getMutantModelInfo(e.getValue());
				
				
				// build model filename
				String modelFilename = filename + "_" + e.getKey().toLowerCase() + "_" + info.rootName.toLowerCase() ;
				String mutantContents = e.getValue();
				
				int beginModelIndex = mutantContents.indexOf('=');
				if (beginModelIndex != -1) {
					mutantContents = mutantContents.substring(beginModelIndex + 1);
				}
				
				// parse mutant model
				EdgeParser ep = new EdgeParser();
				
				AscChar[][] inputArray = AsciiParser.buildArrayFromString(mutantContents);
				
				// First, get classes
				List<AscClass> classes = null;
				if (info.mutantType == MutantType.CLASS) {
					classes = ModelElementParser.getClassesConcreteSyntax(inputArray);
				} else if (info.mutantType == MutantType.ABSTRACT) {
					classes = ModelElementParser.getModelElementsAbstractSyntax(inputArray);
				}
				
				// Then, reserve labels so they will not be parsed by the arrowhead finder
				AsciiParser.reserveColorAllLabelsAndSignals(inputArray);
				
				
				//Util.printArrayWithColor(inputArray);
				
				
				// Detect arrowheads
				List<Coords> arrowHeads = AsciiParser.getArrowHeadLocations(inputArray);
	
				Util.printArray(inputArray);
				
				// Follow the lines from arrowheads
				for(Coords p : arrowHeads) {
					System.out.println("arrowhead @" + p.toString());
					ep.followLineFromArrowhead(p.x, p.y, inputArray, Util.getNextColor());	// XXX: does this still work? if not, swap x and y
				}
				
				// Detect and follow signals, which might be dangling
				AsciiParser.detectAndFollowSignals(inputArray, ep);
				
				// connect signal edges and desugar multi-edges to multiple edges
				ep.connectSignalEdges();
	
				// generate ecore
				if (info.mutantType == MutantType.CLASS) {
					URI modelUri = URI.createURI("file:///" + basePath + File.separator + modelDirectory + File.separator + modelFilename + ".ecore");
					System.out.println("MUTANT will now generate CLASS " + modelUri);					
					EcoreGenerator.generateEcoreClassModel(info.rootName, classes, ep.getEdges(), modelUri);
				} else if (info.mutantType == MutantType.ABSTRACT) {
					URI modelUri = URI.createURI("file:///" + basePath + File.separator + modelDirectory + File.separator + modelFilename + ".xmi");
					System.out.println("MUTANT will now generate ABSTRACT " + modelUri);
					EcoreGenerator.generateEcoreAbstractModel(info, classes, ep.getEdges(), modelUri);
				}
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.err.println("Error processing " + filename);
		}
	}
	
}
