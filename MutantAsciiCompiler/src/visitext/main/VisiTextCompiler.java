package visitext.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.sound.midi.Synthesizer;

import org.eclipse.emf.common.util.URI;

import visitext.ascii.representation.AscChar;
import visitext.ascii.representation.AscClass;
import visitext.ascii.representation.AscEdge;
import visitext.debug.ui.ArrayVisualizer;
import visitext.generator.EcoreGenerator;
import visitext.main.VisiTextModelInfo.MutantType;
import visitext.parser.AsciiParser;
import visitext.parser.EdgeParser;
import visitext.parser.ModelElementParser;
import visitext.util.Coords;
import visitext.util.Util;

/**
 * 
 * @author Felix Rieger
 *
 */
public class VisiTextCompiler {
	
	private final static String modelDirectory = "mutant";
	public static boolean DEBUG = false;
	private static List<VisiTextCompilationInfo> compilationInfos = new ArrayList<VisiTextCompilationInfo>();
	
	private static String basePath;
	private static String modelPath;
	
	public static void main(String[] args) {
		boolean hasMetamodels = false;
		if (args.length == 0) {
			System.out.println("MUTANT compiler. Arguments: VisiTextCompiler.jar COMPILE Java-project-base-path <Ecore-models-base-path> <DEBUG> ");
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
		writeStatistics(new File(basePath + File.separator + "mutant" + File.separator + "compileStatistics-" + System.currentTimeMillis() + ".csv"));
	}
	
	private static void writeStatistics(File statisticsFile) {
		try {
			if (!statisticsFile.exists()) {
				System.out.println("creating " + statisticsFile);
				//statisticsFile.mkdirs();
				statisticsFile.createNewFile();
			}
			PrintWriter pw = new PrintWriter(statisticsFile);
			
			System.out.println("writing to " + statisticsFile);
			//PrintWriter pw = new PrintWriter(System.out);
			pw.print(VisiTextCompilationInfo.getCsvHeader());
			for (VisiTextCompilationInfo mci : compilationInfos) {
				pw.print(mci.getCsvContents());
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
				//Map<String, String> mutantMethods = parseJavadocMutantFile(start);
				String javaFileName = start.getName().substring(0, start.getName().lastIndexOf("."));
				compileMutant(javaFileName, parseJavadocMutantFile(start));
			}
		}
	}
	
	private static class MutantJavadocParseResult {
		public Map<String, String> result;
		public Map<String, Integer> lineNumbers;
		
		public MutantJavadocParseResult(Map<String, String> result,
				Map<String, Integer> lineNumbers) {
			super();
			this.result = result;
			this.lineNumbers = lineNumbers;
		}
		
		
	}
	
	private static MutantJavadocParseResult parseJavadocMutantFile(File sourceFile) {
		System.out.println("MUTANT: Trying to parse " + sourceFile.getPath());
		Map<String, String> methodNameToMutantContentsMap = null;
		Map<String, Integer> methodNameToLineNumberMap = null;
		try {
			Scanner sc = new Scanner(sourceFile);
			methodNameToMutantContentsMap = new HashMap<String, String>(); 
			methodNameToLineNumberMap = new HashMap<String, Integer>(); 

			StringBuilder sb = new StringBuilder();

			String currentMutantModelInputModel = null;
			String currentMutantModelOutputModel = null;
			
			boolean inMutantModel = false;
			boolean inInputModel = false;
			boolean inOutputModel = false;
			boolean inJavadocComment = false;
			boolean foundMutantModel = false;
			boolean mutantModelHasChanged = false;
			String currentMethodName = null;
			int currentLineNumber = 0;
			int startLineNumberInputModel = 0;
			int startLineNumberOutputModel = 0;
			while (sc.hasNextLine()) {
				currentLineNumber++;
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
						System.out.println("--- begin input model");
						inMutantModel = true;
						inInputModel = true;
						inOutputModel = false;
						startLineNumberInputModel = currentLineNumber;
					}
					if (currLine.contains("@OutputModel")) {
						System.out.println("--- begin output model");
						inMutantModel = true;
						inInputModel = false;
						inOutputModel = true;
						mutantModelHasChanged = true;
						startLineNumberOutputModel = currentLineNumber;
					}
					if (currLine.trim().contains("@ModelEnd")) {
						inMutantModel = false;
						foundMutantModel = true;
						if (inInputModel) {
							currentMutantModelInputModel = sb.toString();
						} else if (inOutputModel) {
							currentMutantModelOutputModel = sb.toString();
						}
						inInputModel = false;
						inOutputModel = false;
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
								methodNameToMutantContentsMap.put(currentMethodName, currentMutantModelInputModel);
								methodNameToLineNumberMap.put(currentMethodName, startLineNumberInputModel);
								if (currentMutantModelOutputModel != null) {
									methodNameToMutantContentsMap.put(currentMethodName + "-outputmodel", currentMutantModelOutputModel);
									methodNameToLineNumberMap.put(currentMethodName + "-outputmodel", startLineNumberOutputModel);
								}
								foundMutantModel = false;
								break; // no need to parse more tokens
							}
						}
						
					}
				}
								
				if (mutantModelHasChanged) {
					currentMutantModelInputModel = sb.toString();
					if (currentMutantModelInputModel.contains("@OutputModel")) {
						int idx = currentMutantModelInputModel.indexOf("@OutputModel");
						currentMutantModelInputModel = currentMutantModelInputModel.substring(0, idx);
					}
					System.out.println(currentMutantModelInputModel);
					mutantModelHasChanged = false;
					sb = new StringBuilder();
					sb.append(currLine.trim());
				}
				if (currLine.trim().contains("*/")) {
					inJavadocComment = false;
					if (inMutantModel) {
						foundMutantModel = true;
					}
					String tmpCurrentMutantModel;
					tmpCurrentMutantModel = sb.toString();
					int commentEnd = tmpCurrentMutantModel.lastIndexOf("*/");
					if (commentEnd != -1) {
						tmpCurrentMutantModel = tmpCurrentMutantModel.substring(0, commentEnd);
					}
					sb = new StringBuilder();
					
					if (inInputModel) {
						currentMutantModelInputModel = tmpCurrentMutantModel;
					} else if (inOutputModel) {
						currentMutantModelOutputModel = tmpCurrentMutantModel;
					}
					inInputModel = false;
					inOutputModel = false;
					inMutantModel = false;

				}

				
			}
			
			sc.close();
			
			if (methodNameToMutantContentsMap.isEmpty()) {
				System.out.println("No MUTANT models in file");
			} else {
				System.out.println("The following MUTANT models have been found");
				for (Entry<String, String> e : methodNameToMutantContentsMap.entrySet()) {
					System.out.println("\n");
					System.out.println("Line number:" + methodNameToLineNumberMap.get(e.getKey()));
					System.out.println("Method name:" + e.getKey() + "<");
					System.out.println("MUTANT model:\n" + e.getValue() + "<");
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//return methodNameToMutantContentsMap;
		return new MutantJavadocParseResult(methodNameToMutantContentsMap, methodNameToLineNumberMap);
	}
	
	private static VisiTextModelInfo getVisiTextModelInfo(String mutantContents) {
		int inputModelIndex;
		if (mutantContents.contains("@InputModel")) {
			inputModelIndex = mutantContents.indexOf("@InputModel");
		} else {
			inputModelIndex = mutantContents.indexOf("@OutputModel");
		}
		int equalSignIndex = mutantContents.indexOf("=", inputModelIndex);
		String modelDefSubstr = mutantContents.substring(inputModelIndex, equalSignIndex);
		String[] tokens = modelDefSubstr.split("\\s+");
		
		
		VisiTextModelInfo info = null;
		if (tokens.length == 3) { // concrete syntax: class
			if (tokens[1].equals("EPackage")) { // ecore
				info = new VisiTextModelInfo(VisiTextModelInfo.MutantType.CLASS, null, tokens[1], tokens[2]);
			} else if (tokens[1].equals("Package")) { // uml
				info = new VisiTextModelInfo(VisiTextModelInfo.MutantType.UML, null, tokens[1], tokens[2]);
			}
		} else if (tokens.length == 4) { // abstract syntax
			
			String nsUri = tokens[1];
			int beginIdx = nsUri.indexOf('"');
			int endIdx = nsUri.lastIndexOf('"');
			nsUri = nsUri.substring(beginIdx+1, endIdx);
			info = new VisiTextModelInfo(VisiTextModelInfo.MutantType.ABSTRACT, nsUri, tokens[2], tokens[3]);
		}
		
		return info;
	}
	
	
	
	private static void compileMutant(String filename, MutantJavadocParseResult parseResult) {
		Map<String, String> methodNameToMutantContentsMap = parseResult.result;
		Map<String, Integer> methodNameToLineNumberMap = parseResult.lineNumbers;
		try {
			for (Entry<String, String> e : methodNameToMutantContentsMap.entrySet()) {
				long compileTimeStart = System.nanoTime();
				int lineNumber = methodNameToLineNumberMap.get(e.getKey());
				System.out.println("---\n" + e.getKey());
				VisiTextModelInfo info = getVisiTextModelInfo(e.getValue());
				
				// build model filename
				System.out.println("processing " + filename + "  " + e.getKey() + "   " + info.rootName);
				String modelFilename = filename + "_" + e.getKey().toLowerCase() + "_" + info.rootName.toLowerCase() ;
				String mutantContents = e.getValue();
				
				int beginModelIndex = mutantContents.indexOf('=');
				if (beginModelIndex != -1) {
					mutantContents = mutantContents.substring(beginModelIndex + 1);
				}
				
				// parse mutant model
				EdgeParser ep = new EdgeParser();
				
				AscChar[][] inputArray;
				inputArray = AsciiParser.buildArrayFromString(mutantContents);
				
				// First, get classes
				List<AscClass> classes = null;
				if (info.mutantType == MutantType.CLASS || info.mutantType == MutantType.UML) {
					classes = ModelElementParser.getClassesConcreteSyntax(inputArray, lineNumber);
				} else if (info.mutantType == MutantType.ABSTRACT) {
					classes = ModelElementParser.getModelElementsAbstractSyntax(inputArray, lineNumber);
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
				ep.connectSignalEdges(inputArray);
	
				// Try to find as-yet unconnected edges (bidirectional edges)
				AsciiParser.detectAndFollowBidirectionalEdges(inputArray, ep);
				
				// get rolenames for all edges
				ep.getRolenamesForAllEdges(inputArray);

				// show visualization
				ArrayVisualizer av = new ArrayVisualizer(inputArray, classes, ep, "final-"+filename);
								
				// generate ecore
				if (info.mutantType == MutantType.CLASS) {
					URI modelUri = URI.createURI("file:///" + basePath + File.separator + modelDirectory + File.separator + modelFilename + ".ecore");
					System.out.println("MUTANT will now generate CLASS " + modelUri);					
					EcoreGenerator.generateEcoreClassModel(info.rootName, classes, ep.getEdges(), modelUri);
				} else if (info.mutantType == MutantType.ABSTRACT) {
					URI modelUri = URI.createURI("file:///" + basePath + File.separator + modelDirectory + File.separator + modelFilename + ".xmi");
					System.out.println("MUTANT will now generate ABSTRACT " + modelUri);
					EcoreGenerator.generateEcoreAbstractModel(info, classes, ep.getEdges(), modelUri);
				} else if (info.mutantType == MutantType.UML) {
					URI modelUri = URI.createURI("file:///" + basePath + File.separator + modelDirectory + File.separator + modelFilename + ".uml");
					System.out.println("MUTANT will now generate UML CLASS " + modelUri);					
					EcoreGenerator.generateUmlClassModel(info.rootName, classes, ep.getEdges(), modelUri);
				} 
				long compileTimeEnd = System.nanoTime();
				
				// build mutant compilation info
				int numberOfAttributes = 0;
				int numberOfOperations = 0;
				for (AscClass ac : classes) {
					numberOfAttributes += ac.attributes.size();
					numberOfOperations += ac.methods.size();
				}

				int numberOfAssociations = 0;
				int numberOfGeneralizations = 0;

				for (AscEdge ae : ep.getEdges()) {
					if (ae.isInheritance) {
						numberOfGeneralizations += 1;
					} else {
						if (ae.oppositeEdge != null) {
							numberOfAssociations += 1;
						} else {
							numberOfAssociations += 2;
						}
					}
				}
				
				numberOfAssociations = numberOfAssociations / 2;
				
				
				VisiTextCompilationInfo mci = new VisiTextCompilationInfo(filename, e.getKey(), info, (compileTimeEnd - compileTimeStart) / 1E9, classes.size(), numberOfAttributes, numberOfOperations, numberOfAssociations, numberOfGeneralizations);
				compilationInfos.add(mci);

			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.err.println("Error processing " + filename);
		}
	}
	
}
