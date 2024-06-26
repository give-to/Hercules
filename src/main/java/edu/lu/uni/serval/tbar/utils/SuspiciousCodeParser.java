package edu.lu.uni.serval.tbar.utils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import edu.lu.uni.serval.AST.ASTGenerator;
import edu.lu.uni.serval.AST.ASTGenerator.TokenType;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.lu.uni.serval.tbar.fixtemplate.FixTemplate;

/**
 * Parse the suspicious code into an AST.
 *
 * @author kui.liu
 *
 */
public class SuspiciousCodeParser {

	private static File javaFile;
	public CompilationUnit unit = null;
	private List<Pair<ITree, String>> suspiciousCode = new ArrayList<>();

	public void parseSuspiciousCode(File javaFile, int suspLineNum) {
		this.javaFile = javaFile;
		unit = new MyUnit().createCompilationUnit(javaFile);

		FixTemplate.unit = unit;
		ITree rootTree = new ASTGenerator().generateTreeForJavaFile(javaFile, TokenType.EXP_JDT);
		identifySuspiciousCodeAst(rootTree, suspLineNum);
//		return unit.getProblems().length;
	}

	private void identifySuspiciousCodeAst(ITree tree, int suspLineNum) {
		List<ITree> children = tree.getChildren();

		for (ITree child : children) {
			int startPosition = child.getPos();
			int endPosition = startPosition + child.getLength();
			int startLine = this.unit.getLineNumber(startPosition);
			int endLine = this.unit.getLineNumber(endPosition);
			if (endLine == -1) endLine = this.unit.getLineNumber(endPosition - 1);
			if (startLine <= suspLineNum && suspLineNum <= endLine) {

				FixTemplate.susCodeStartLine = startLine;
				FixTemplate.susCodeEndLine = endLine;

				if (startLine == suspLineNum || endLine == suspLineNum) {
					if (Checker.isBlock(child.getType())) {
						identifySuspiciousCodeAst(child, suspLineNum);
						continue;
					} else {
						if (!isRequiredAstNode(child)) {
							child = traverseParentNode(child);
							if (child == null) continue;
						}
						Pair<ITree, String> pair = new Pair<ITree, String>(child, readSuspiciousCode(child));
						if (!suspiciousCode.contains(pair)) {
							suspiciousCode.add(pair);
						}
					}
				} else {
					identifySuspiciousCodeAst(child, suspLineNum);
				}
			} else if (startLine > suspLineNum) {
				break;
			}
		}
	}

	private boolean isRequiredAstNode(ITree tree) {
		int astNodeType = tree.getType();
		if (Checker.isStatement(astNodeType)
				|| Checker.isFieldDeclaration(astNodeType)
				|| Checker.isMethodDeclaration(astNodeType)
				|| Checker.isTypeDeclaration(astNodeType)) {
			return true;
		}
		return false;
	}

	private ITree traverseParentNode(ITree tree) {
		ITree parent = tree.getParent();
		if (parent == null) return null;
		if (!isRequiredAstNode(parent)) {
			parent = traverseParentNode(parent);
		}
		return parent;
	}

	private String readSuspiciousCode(ITree suspiciousCodeAstNode) {
		String javaFileContent = FileHelper.readFile(this.javaFile);
		int startPos = suspiciousCodeAstNode.getPos();
		int endPos = startPos + suspiciousCodeAstNode.getLength();
		return javaFileContent.substring(startPos, endPos);
	}

	// get Hercules' node name
	public static String readSuspiciousCode(ASTNode n){
		String javaFileContent = FileHelper.readFile(javaFile);
		int startPos = n.getStartPosition();

		int endPos = startPos + n.getLength();

		if(startPos<0){
			startPos = 0;
		}
		if(startPos >= javaFileContent.length()){
			startPos = javaFileContent.length();
		}
		if(endPos >= javaFileContent.length())
			endPos = javaFileContent.length();
		if(endPos < 0)
			endPos = 0;
//		if(endPos <0) endPos = 0;
//		System.out.println("startPos: " + startPos);
//		System.out.println("endPos: " + endPos);
		if(0 <= startPos && startPos <= javaFileContent.length() && 0 <= endPos && endPos <= javaFileContent.length()&&startPos <= endPos)
			return javaFileContent.substring(startPos, endPos);
		else {
			System.out.println("startPos: " + startPos);
			System.out.println("endPos: " + endPos);
			throw new StringIndexOutOfBoundsException("Get ASTNode Name error");
		}

	}

	public List<Pair<ITree, String>> getSuspiciousCode() {
		return suspiciousCode;
	}

	public static class MyUnit {

		public CompilationUnit createCompilationUnit(File javaFile) {
			char[] javaCode = readFileToCharArray(javaFile);
			ASTParser parser = createASTParser(javaCode);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			CompilationUnit unit = (CompilationUnit) parser.createAST(null);

			return unit;
		}

		private ASTParser createASTParser(char[] javaCode) {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(javaCode);

			return parser;
		}

		private char[] readFileToCharArray(File javaFile) {
			StringBuilder fileData = new StringBuilder();
			BufferedReader br = null;

			char[] buf = new char[10];
			int numRead = 0;
			try {
				FileReader fileReader = new FileReader(javaFile);
				br = new BufferedReader(fileReader);
				while ((numRead = br.read(buf)) != -1) {
					String readData = String.valueOf(buf, 0, numRead);
					fileData.append(readData);
					buf = new char[1024];
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null) {
						br.close();
						br = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (fileData.length() > 0)
				return fileData.toString().toCharArray();
			else return new char[0];
		}
	}

}
