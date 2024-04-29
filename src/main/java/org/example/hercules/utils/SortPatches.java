package org.example.hercules.utils;

import org.example.hercules.PatchFile;
import org.example.hercules.Patch;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;

import java.util.List;

public class SortPatches {

	private static int CORE_POOL_SIZE = 5;
	public static String DELIMITER = "\n";
	public static boolean DEBUG = true;
	public static boolean USE_CUSTOM_LOADER = false;
	public static String COMING_JAR = "./lib/coming.jar";
	public static String JFXRT_JAR = "./lib/jfxrt.jar";
	public static String ODS_ROOT = "./";
	static class PatchWithLine{
		public String patchCode;
		public int bugLine;
		public String patch;
		public int line;
		public PatchWithLine(String patch, int line){
			this.line = line;
			this.patch = patch;
			this.patchCode = patch;
			this.bugLine = line;
		}
	}
	static class DelegatePatch{
		public List<Patch> content;
		public PatchFile patchFile;
		public int id;
		public String path;
		public String patchedSrc;
		public double score;
		public DelegatePatch(int id, PatchFile patchFile, String path){
			this.id = id;
			this.patchFile = patchFile;
			this.path = path;
			this.content = new ArrayList<Patch>();
		}
		public PatchWithLine[] getPatches(){
			PatchWithLine patches[] = new PatchWithLine[this.content.size()];
			for(int i = 0; i < this.content.size(); ++i)
				patches[i] = new PatchWithLine(this.content.get(i).getPatchStr(), this.content.get(i).getBuggyLine().get(0).intValue());
			return patches;
		}
		public void makeSource() throws IOException{
			try{
				this.patchedSrc = applyPatches(this.path, this.getPatches());
			}catch(Exception e){
				if(DEBUG){
					System.err.println("Exception inspect:");
					System.err.println("method: makeSource()");
					System.err.println("DelegatePatch ID: " + this.id);
					System.err.println("patchFile:");
					PatchFile pf = this.patchFile;
					System.err.println("GroupId: "+pf.getGroupId());
					System.err.println("PatchId: "+pf.getPatchId());
				}
				throw e;
			}
		}
		public String makeAndGetSource() throws IOException{
			this.makeSource();
			return this.patchedSrc;
		}
	}
	public static String applyPatches(String srcFilePath, PatchWithLine[] patches) throws IOException{
		List<Object> codeLines = PatchProcessor.convertStringListToObjectList(readFileWithDelimiter(srcFilePath, DELIMITER));
		for(PatchWithLine patch : patches)
			try{
				//codeLines = PatchProcessor.applyPatch(codeLines, patch.patch.trim(), patch.line);
				codeLines = PatchProcessor.applyPatch(codeLines, patch.patch, patch.line);
			}catch (Exception e){
				if(DEBUG){
					System.err.println("Exception inspect:");
					System.err.println("method: applyPatches(String, PatchWithLine[])");
					System.err.println("srcFilePath: " + srcFilePath);
					System.err.println("patch: " + patch.patch);
					System.err.println("for line: " + patch.line);
					//continue;
				}
				throw e;
			}
		return PatchProcessor.toFixedLineSrc(codeLines);
	}
	private static void tryAdd(HashMap<String, List<DelegatePatch>> fileMap, DelegatePatch patchCandidate){
		String fileName = patchCandidate.path;
		if(fileMap.containsKey(fileName)){
			fileMap.get(fileName).add(patchCandidate);
			return;
		}
		List<DelegatePatch> patchList = new ArrayList<DelegatePatch>();
		patchList.add(patchCandidate);
		fileMap.put(fileName, patchList);
	}
	public static String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[10];
		while ((reader.read(buf)) != -1) {
			fileData.append(new String(buf));
			buf = new char[1024];
		}

		reader.close();

		return  fileData.toString();
	}
	public static List<String> readFileWithDelimiter(String filePath, String delimiter) throws IOException{
		List<String> linesWithDelimiters = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;

			while ((line = reader.readLine()) != null) {
				linesWithDelimiters.add(line + "\n"); // Add each line with delimiter
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return linesWithDelimiters;
	}
	public static void fillFileMap(HashMap<String, List<DelegatePatch>> fileMap, List<PatchFile> patchFiles) throws IllegalStateException{
		int counter = -1;
		String fileName;
		for(PatchFile patchFile : patchFiles){
			counter ++;
			//HashMap<String, DelegatePatch> filePatches = new HashMap<String, DelegatePatch>();
			for(Patch patch : patchFile.getPatch()){
				fileName = patch.getBuggyFilePath();
				boolean shouldAdd = true;
				if(fileMap.containsKey(fileName)){
					List<DelegatePatch> list = fileMap.get(fileName);
					if(list.get(list.size() - 1).id == counter)
						shouldAdd = false;
				}
				if(shouldAdd)
					tryAdd(fileMap, new DelegatePatch(counter, patchFile, fileName));
				List<DelegatePatch> list = fileMap.get(fileName);
				DelegatePatch delegatePatch = list.get(list.size() - 1);
				if(delegatePatch.id != counter)
					throw new IllegalStateException("Id and counter does not match");
				delegatePatch.content.add(patch);
			}
		}
	}

	public static List<Double> getProba(String src, List<String> patches, int timeoutForEach){
		try{
			/*
			URLClassLoader ucl = new URLClassLoader(new URL[] {coming.toURI().toURL()});
			*/
			Class odsapi;
			File coming = new File(COMING_JAR);
			if(USE_CUSTOM_LOADER){
				CustomClassLoader ucl = new CustomClassLoader(new URL[] {coming.toURI().toURL()}, null);
				odsapi = ucl.loadClass("fr.inria.coming.utils.ODSAPI_boost");
			}else{
				File jfxrt = new File(JFXRT_JAR);
				URLClassLoader ucl = new URLClassLoader(new URL[] {coming.toURI().toURL(), jfxrt.toURI().toURL()}, null);
				odsapi = ucl.loadClass("fr.inria.coming.utils.ODSAPI_boost");
			}
			//Object api = odsapi.newInstance();
			Constructor api_c = odsapi.getConstructor(int.class);
			Object api = api_c.newInstance(CORE_POOL_SIZE);
			if(!ODS_ROOT.equals("./")){
				Method setRoot = odsapi.getDeclaredMethod("setRoot", String.class);
				setRoot.invoke(api, ODS_ROOT);
			}
			Method api_f = odsapi.getDeclaredMethod("getProba", String.class, List.class, int.class);
			return (List<Double>)(api_f.invoke(api, src, patches, timeoutForEach));
			/*
			ODSAPI_boost api = new ODSAPI_boost(CORE_POOL_SIZE);
			return api.getProba(src, patches, timeoutForEach);
			*/
		}catch (Exception e){
			e.printStackTrace();
			List<Double> ret =  new ArrayList<Double>();
			for(int i = 0; i < patches.size(); ++i)
				ret.add(-1.0);
			return ret;
		}
	}
	public static List<Double> getProba(String src, List<String> patches){
		return getProba(src, patches, 8);
	}
	public static void sortPatches(List<PatchFile> patchFiles) throws IOException, IllegalStateException{
		HashMap<String, List<DelegatePatch>> fileMap = new HashMap<String, List<DelegatePatch>>();
		Integer[] zeroes = new Integer[patchFiles.size()];
		Arrays.fill(zeroes, 0);
		List<Integer> fileNum = new ArrayList<Integer>(Arrays.asList(zeroes));
		fillFileMap(fileMap, patchFiles);
		//System.out.println(Arrays.asList(fileMap.get("/CatenaD4jProjects/Math_35/src/main/java/org/apache/commons/math3/genetics/ListPopulation.java").get(0).getPatches()));
		for(String file : fileMap.keySet()){
			List<String> patchedSources = new ArrayList<String>();
			String sourceCode = readFileToString(file);
			List<DelegatePatch> list = fileMap.get(file);
			for(DelegatePatch patch : list)
				patchedSources.add(patch.makeAndGetSource());
			List<Double> scores = getProba(sourceCode, patchedSources);
			if(list.size() != scores.size())
				throw new IllegalStateException("Size of map and scores does not match");
			for(int i = 0; i < scores.size(); ++i){
				double score = scores.get(i);
				if(score < 0)
					score = -1.0;
				list.get(i).score = score;
				PatchFile patchFile = list.get(i).patchFile;
				patchFile.setOdsScore(patchFile.getOdsScore() + score);
				Integer num = fileNum.get(list.get(i).id);
				fileNum.set(list.get(i).id, num + 1);
			}
		}
		for(int i = 0; i < patchFiles.size(); ++i){
			PatchFile patchFile = patchFiles.get(i);
			patchFile.setOdsScore(patchFile.getOdsScore() / fileNum.get(i));
		}
		patchFiles.sort((a, b) -> Double.compare(b.getOdsScore(), a.getOdsScore()));
		//System.out.println(fileMap.get("/CatenaD4jProjects/Math_35/src/main/java/org/apache/commons/math3/genetics/ListPopulation.java").get(0).patchedSrc);
	}
}
