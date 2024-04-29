package org.example.hercules;

import edu.lu.uni.serval.jdt.tree.ITree;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import edu.lu.uni.serval.jdt.tree.Tree;
import edu.lu.uni.serval.jdt.tree.TreeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.lang.Character;
import java.lang.IllegalArgumentException;

public class PatchExtend {
	static List<Integer> ignored_types = new ArrayList<Integer>(){{add(83);add(7);}};
	//static List<Integer> ignored_types = new ArrayList<Integer>();
	static String _ignored_words[] = new String[] {"++", "this", "native", "?", "...", "{", "^", "abstract", "interface", "static", "+", "%=", "[", "implements", "}", "extends", "--", "!", "if", "==", "&", "||", "default", "|=", ">>>=", "enum", "->", "throws", "void", "transient", "finally", "synchronized", "!=", ".", ">=", "while", "]", "return", "package", "continue", "throw", "import", "/=", "<<=", "final", "goto", "const", "&&", "class", "<", "catch", "public", "else", "-=", "~", "for", "-", "new", ">", "switch", "+=", "instanceof", "super", "::", ":", "/", ",", "(", "strictfp", "protected", "<=", "|", "<<", ">>=", "&=", "do", ";", "*", "try", "volatile", "=", "%", "*=", "private", ")", "^=", "break", "assert", "case",};
	//static String _ignored_words[] = new String[] {};
	static List<String> ignored_words = Arrays.asList(_ignored_words);
	static List<String> left_right_indent = Arrays.asList(new String[] { "<<", "*=", "=", "|=", "/=", ":", "==", "+=", "&&", "<<=", "~", "/", "<=", "%=", "||", ">>=", "+", "->", "&", "-", ">", ">>>=", "?", "<", "^=", "&=", "^", "-=", "|", "!=", ">=", "*", "%"});
	static List<String> right_indent = Arrays.asList(new String[]{"--", ";", "++", ",", "!", "]", "}"}); 
	static List<String> can_with_token = Arrays.asList(new String[] {"--", "<<", "*=", "=", "::", "|=", ")", "/=", ":", "==", "+=", "&&", "<<=", "(", "~", "/", "<=", "%=", "]", ";", "||", "}", ">>=", "++", "+", "->", "&", "-", ",", ">", ">>>=", "?", "<", "[", "^=", "&=", "^", "!", ".", "-=", "|", "!=", "...", ">=", "*", "{", "%"});

	public static String getSrc(ITree node){
		return ((Tree)node).getName();
	}

	public static List<ITree> getLeaves(ITree node){
		List<ITree> leaves = new ArrayList<ITree>();
		for(ITree candidate : node.preOrder()){
			if(candidate.isLeaf()){
				leaves.add(candidate);
			}
		}
		return leaves;
	}

	public static List<String> getIdentifiers(ITree node){
		List<String> identifiers = new ArrayList<String>();
		String code;
		for(ITree leaf : getLeaves(node)){
			if(!ignored_types.contains(leaf.getType())){
				code = getSrc(leaf);
				if(!ignored_words.contains(code)){
					identifiers.add(code);
				}
			}
		}
		return identifiers;
	}

	public static boolean fullMatch(List<String> match, List<String> matcher, int offset){
		// match the first argument
		for(int i = 0; i < matcher.size(); ++i){
			if(i + offset >= match.size())
				return false;
			if(!matcher.get(i).equals(match.get(i + offset)))
				return false;
		}
		return true;
	}

	public static List<Integer> findAll(List<String> obj, String toFind){
		List<Integer> res = new ArrayList<Integer>();
		for(int i = 0; i < obj.size(); ++i){
			if(obj.get(i).equals(toFind)){
				res.add(i);
			}
		}
		return res;
	}

	public static void matchRemove(List<String> big, List<String> small){
		int pos = -1;
		// find the position of last matched sequence
		for(Integer i : findAll(big, small.get(0))){
			int unpacked = i.intValue();
			if(fullMatch(big, small, unpacked)){
				pos = unpacked;
			}
		}
		if(pos < 0)
			return;
		big.subList(pos, pos + small.size()).clear();
	}

	public static String compressSpace(String input){
		return input.replaceAll("\\s+", " ");
	}
	
	public static String getPatchType(String p){
		int sign1 = p.indexOf("$");
		int sign2 = p.indexOf(":");
		if(sign2 == -1 || sign2 > sign1)
			return p.substring(0, sign1);
		return p.substring(0, sign2);
	}

	public static String getPatchHead(String p){
		return p.substring(0, p.indexOf("$"));
	}

	public static String getPatchContent(String p){
		return p.substring(p.indexOf("$") + 1);
	}

	public static List<String> tokenize(String p){
		List<String> tokens = new ArrayList<String>();
		String[] _tokens = p.split("\\s+");
		for(String token : _tokens){
			if(ignored_words.contains(token))
				tokens.add(token);
			else
				tokens.addAll(_tokenize(token));
		}
		return tokens;
	}
	public static List<String> _tokenize(String p){
		List<String> tokens = new ArrayList<String>();
		String cache = "";
		for(char ch : p.toCharArray()){
			if(Character.isWhitespace(ch)){
				if(!cache.isEmpty()){
					tokens.add(cache);
					cache = "";
				}
				continue;
			}
			if(can_with_token.contains(""+ch)){
				if(!cache.isEmpty()){
					tokens.add(cache);
					cache = "";
				}
				tokens.add(""+ch);
				continue;
			}
			cache += ch;
		}
		if(!cache.isEmpty())
			tokens.add(cache);
		return tokens;
	}

	public static String replaceTokens(String p, String from, String to){
		try{
			return _replaceTokens(p, from, to);
		}catch(IllegalArgumentException e){
			//String[] tokens = p.split("\\s+");
			List<String> tokens = tokenize(p);
			String res = "";
			for(String token : tokens){
				if(token.equals(from)){
					res += to;
				}else{
					res += token;
				}
				res += " ";
			}
			return res.trim();
		}
	}

	public static String _replaceTokens(String p, String from, String to) throws IllegalArgumentException{
		String cache = "";
		int scanner = -1;
		String scanned = "";
		int checker = 0;
		boolean shouldBeUpdated = true;
		for(char ch : p.toCharArray()){
			scanner += 1;
			if(Character.isWhitespace(ch) || can_with_token.contains(""+ch)){
				if(!cache.isEmpty()){
					if(!p.substring(checker, scanner).equals(cache))
						throw new IllegalArgumentException("Cache should be " + p.substring(checker, scanner) + " but get " + cache);
					if(!cache.equals(from))
						scanned += cache;
					else
						scanned += to;
					shouldBeUpdated = true;
					cache = "";
				}
				scanned += ch;
				continue;
			}else if(shouldBeUpdated){
				checker = scanner;
				shouldBeUpdated = false;
			}
			cache += ch;
		}
		if(!cache.isEmpty()){
			if(!p.substring(checker, scanner).equals(cache))
				throw new IllegalArgumentException("Cache should be " + p.substring(checker, scanner) + " but get " + cache);
			if(!(scanner == p.length()))
				throw new IllegalArgumentException("Scanner checking failed: want " + p.length() + " but get " + scanner);
			if(!cache.equals(from))
				scanned += cache;
			else
				scanned += to;
			cache = "";
		}
		return scanned;
	}
	
	public static int findObj(List<String> l, String toFind, int from){
		for(int i = from; i < l.size(); ++i){
			if(l.get(i).equals(toFind)){
				return i;
			}
		}
		return -1;
	}

	public static HashMap<Integer, Integer> positionMap(List<String> a, List<String> b){
		HashMap<Integer, Integer> res  = new HashMap<Integer, Integer>();
		int pos = 0;
		int pos1 = 0;
		for(int i = 0; i < a.size(); ++i){
			pos1 = findObj(b, a.get(i), pos);
			if(pos1 != -1){
				res.put(i, pos1);
				pos = pos1 + 1;
			}
		}
		// TODO implement diff
		return res;
	}

	public static int abs(int v){
		if(v < 0)
			return -v;
		return v;
	}

	public static int getNearest(int pos, Set<Integer> s){
		int res = -1;
		for(Integer i : s.toArray(new Integer[0])){
			if(res == -1 || (abs(i.intValue() - pos) < abs(res - pos)))
				res = i.intValue();
		}
		return res;
	}

	public static List<Integer> betweenBound(int a, int b){
		List<Integer> res = new ArrayList<Integer>();
		for(int i = a + 1; i < b; ++i){
			res.add(i);
		}
		return res;
	}

	public static int locateSequence(List<String> full, List<String> part){
		for(int i = 0; i < full.size(); ++i){
			if(fullMatch(full, part, i))
				return i;
		}
		return -1;
	}
	
	public static int nearestOffset(int pos, List<String> astIdentifiers, String patchToken, String codeToken){
		int nearestPatchToken = findNearest(pos, astIdentifiers, patchToken);
		int nearestCodeForPatch = findNearest(nearestPatchToken, astIdentifiers, codeToken);
		return nearestPatchToken - nearestCodeForPatch;
	}

	public static int findNearest(int pos, List<String> astIdentifiers, String pattern){
		return getNearest(pos, new HashSet<Integer>(findAll(astIdentifiers, pattern)));
	}
/*
 * node1: node of source code
 * node2: node of sibling
 * patchNode1: node of patch/patched code
 * patch: input patch needs to be used in sibling
 */
	public static String patchExtend_strategy_1(ITree node1, ITree node2, ITree patchNode1, String patch){
		// return same patch when it gets same source code
		return patch;
	}

	public static String patchExtend_strategy_2(ITree node1, ITree node2, ITree patchNode1, String patch, ITree srcNode1, ITree srcNode2){
		// Double mapping
		// workaround for replacing
		// a better way is AST mapping
		List<String> patchIdentifiers = getIdentifiers(patchNode1);
		List<String> codeIdentifiers = getIdentifiers(node1);
		List<String> matchIdentifiers = getIdentifiers(node2);
		List<String> astIdentifiers = getIdentifiers(srcNode1);
		matchRemove(astIdentifiers, codeIdentifiers);
		List<String> astMatchIdentifiers = getIdentifiers(srcNode2);
		matchRemove(astMatchIdentifiers, matchIdentifiers);
		List<Integer> activePos = new ArrayList<Integer>();
		// backwards: cannot locate adding identifiers in replacing
		List<Integer> diffPos = new ArrayList<Integer>();
		String id = "";
		for(int i = 0; i < codeIdentifiers.size(); ++i){
			id = codeIdentifiers.get(i);
			if(patchIdentifiers.contains(id))
				activePos.add(i);
			else
				diffPos.add(i);
		}
		HashMap<Integer, Integer> mapping = positionMap(codeIdentifiers, matchIdentifiers);
		String ret = getPatchContent(patch);
		for(Integer _pos : activePos){
			int pos = _pos.intValue();
			if(mapping.containsKey(pos))
				continue;
			int nearest = getNearest(pos, mapping.keySet());
			if(nearest == -1)
				continue;
			int nearest_mapped = mapping.get(nearest);
			if(!(nearest_mapped + pos - nearest < matchIdentifiers.size()))
				continue;
			ret = replaceTokens(ret, codeIdentifiers.get(pos), matchIdentifiers.get(nearest_mapped + pos - nearest));
		}
		HashMap<Integer, Integer> mapping_patch = positionMap(codeIdentifiers, patchIdentifiers);
		int pre = -1;
		Integer[] keysPos = mapping_patch.keySet().toArray(new Integer[0]);
		Arrays.sort(keysPos);
		try{
			for(Integer packed : keysPos){
				int now = packed.intValue();
				if(pre + 1 == now){
					pre = now;
					continue;
				}
				List<Integer> insideCode = betweenBound(pre, now);
				List<Integer> insideCode1 = betweenBound(mapping.getOrDefault(pre, -1), mapping.get(now));
				if(insideCode.size() == insideCode1.size()){
					boolean pass = true;
					for(int i = 0; i < insideCode.size(); ++i){
						if(codeIdentifiers.get(insideCode.get(i).intValue()).equals(matchIdentifiers.get(insideCode1.get(i).intValue())))
							continue;
						pass = false;
						break;
					}
					if(pass){
						pre = now;
						continue;
					}
				}
				List<Integer> insidePatch = betweenBound(mapping_patch.getOrDefault(pre, -1), mapping_patch.get(now));
				// We skip complex situation that multiple inside positions in these lists there for its low probability to appear
				int fDiffP = -1;
				int fDiffC = -1;
				for(int i = 0; i < insideCode.size(); ++i){
					if(codeIdentifiers.get(insideCode.get(i).intValue()).equals(matchIdentifiers.get(insideCode1.get(i).intValue())))
						continue;
					fDiffC = i;
					break;
				}
				for(int i = 0; i < insideCode.size(); ++i){
					if(codeIdentifiers.get(insideCode.get(i).intValue()).equals(patchIdentifiers.get(insidePatch.get(i).intValue())))
						continue;
					fDiffP = i;
					break;
				}
				if(fDiffP == -1 || fDiffC == -1){
					pre = now;
					continue;
				}
				String codeId = codeIdentifiers.get(insideCode.get(fDiffP).intValue());
				String patchId = patchIdentifiers.get(insidePatch.get(fDiffP).intValue());
				String codeId1 = matchIdentifiers.get(insideCode1.get(fDiffC).intValue());
				// findAll and get nearest is better than indexOf
				//int offset = astIdentifiers.indexOf(patchId) - astIdentifiers.indexOf(codeId);
				int posInAST = locateSequence(astIdentifiers, codeIdentifiers);
				int offset = nearestOffset(posInAST, astIdentifiers, patchId, codeId);
				ret = replaceTokens(ret, patchId, astMatchIdentifiers.get(findNearest(posInAST, astMatchIdentifiers, codeId1) + offset));
				System.out.println("# PatchExtend.java:361 backup approach is not used; please check the correctness of result carefully. Inspect: " + ret);
				pre = now;
			}
		}catch(Exception e){
			// when mapping error occurs
			// backup approach
			boolean pass = true;
			for(Integer i : diffPos){
				if(codeIdentifiers.get(i.intValue()).equals(matchIdentifiers.get(mapping.getOrDefault(i.intValue(), 0))))
					continue;
				pass = false;
			}
			if(!pass){
				List<Integer> extraPos = new ArrayList<Integer>();
				for(int i = 0; i < patchIdentifiers.size(); ++i){
					id = patchIdentifiers.get(i);
					if(!codeIdentifiers.contains(id))
						extraPos.add(i);
				}
				String codeToken, matchToken, patchToken;
				for(int i = 0; i < extraPos.size(); ++i){
					try{
						codeToken = codeIdentifiers.get(diffPos.get(i).intValue());
						int nearTokenPos = getNearest(diffPos.get(i), mapping.keySet());
						if(nearTokenPos == -1)
							matchToken = matchIdentifiers.get(diffPos.get(i).intValue());
						matchToken = matchIdentifiers.get(mapping.get(nearTokenPos) + diffPos.get(i) - nearTokenPos);
						if(!matchToken.equals(matchIdentifiers.get(diffPos.get(i).intValue())))
							System.out.println("# PatchExtend.java:385 matchToken gets different values; please check the correctness carefully. Inspect: " + matchToken + " and " + matchIdentifiers.get(diffPos.get(i).intValue()));
						if(codeToken.equals(matchToken))
							continue;
						patchToken = patchIdentifiers.get(extraPos.get(i).intValue());
						// a better way is to have one extra step checking if patchToken and codeToken have same parent
						//int offset = astIdentifiers.indexOf(patchToken) - astIdentifiers.indexOf(codeToken);
						int posInAST = locateSequence(astIdentifiers, codeIdentifiers);
						int offset = nearestOffset(posInAST, astIdentifiers, patchToken, codeToken);
						ret = replaceTokens(ret, patchToken, astMatchIdentifiers.get(findNearest(posInAST, astMatchIdentifiers, matchToken) + offset));
					}catch(Exception unknownException){
						// low-probability exception situation
						continue;
					}
				}
			}
		}
		return getPatchHead(patch) + "$" + ret;
	}

	public static String patchExtend_strategy_3(ITree node1, ITree node2, ITree patchNode1, String patch){
		// double mapping for insert and wrap
		// that no code is modified
		List<String> patchIdentifiers = getIdentifiers(patchNode1);
		List<String> codeIdentifiers = getIdentifiers(node1);
		matchRemove(patchIdentifiers, codeIdentifiers);
		List<String> matchIdentifiers = getIdentifiers(node2);
		List<Integer> activePos = new ArrayList<Integer>();
		String id = "";
		for(int i = 0; i < codeIdentifiers.size(); ++i){
			id = codeIdentifiers.get(i);
			if(patchIdentifiers.contains(id)){
				activePos.add(i);
			}
		}
		HashMap<Integer, Integer> mapping = positionMap(codeIdentifiers, matchIdentifiers);
		String ret = getPatchContent(patch);
		for(Integer _pos : activePos){
			int pos = _pos.intValue();
			if(mapping.containsKey(pos)){
				continue;
			}
			// backwards: nearest position may be wrong in extreme situation
			int nearest = getNearest(pos, mapping.keySet());
			int nearest_mapped = mapping.get(nearest);
			ret = replaceTokens(ret, codeIdentifiers.get(pos), matchIdentifiers.get(nearest_mapped + pos - nearest));
		}
		return getPatchHead(patch) + "$" + ret;
	}

	public static String patchExtend_strategy_4(ITree node1, ITree node2, ITree patchNode1, String patch, ITree srcNode1, ITree srcNode2){
		String ret = getPatchContent(patch);
		List<String> patchIdentifiers = tokenize(ret);
		List<String> retPatch = new ArrayList();
		List<String> codeIdentifiers = tokenize(getSrc(node1));
		List<String> matchIdentifiers = tokenize(getSrc(node2));
		HashMap<Integer, Integer> mapping = positionMap(codeIdentifiers, matchIdentifiers);
		HashMap<Integer, Integer> mapping_patch = positionMap(codeIdentifiers, patchIdentifiers);
		Integer[] keysPos = mapping.keySet().toArray(new Integer[0]);
		Arrays.sort(keysPos);
		int pre = 0;
		int npre = 0;
		int ppre = 0;
		for(Integer _pos : keysPos){
			int pos = _pos.intValue();
			int npos = mapping.get(pos);
			int ppos;
			try{
				ppos = mapping_patch.getOrDefault(pos, findObj(patchIdentifiers, codeIdentifiers.get(pos), ppre));
				if(ppos == -1)
					ppos = patchIdentifiers.size();
			}catch(Exception e){
				ppos = patchIdentifiers.size();
			}
			Iterator iter0 = codeIdentifiers.subList(pre, pos).iterator();
			Iterator iter = matchIdentifiers.subList(npre, npos).iterator();
			String matchToken = iter0.hasNext() ? iter0.next().toString() : "";
			for(int i = ppre; i < ppos; ++i){
				System.out.println(patchIdentifiers.size());
				System.out.println(ppos);
				System.out.println(ppre);
				String token = patchIdentifiers.get(i);
				if(matchToken.equals(token)){
					if(iter0.hasNext())
						matchToken = iter0.next().toString();
					if(iter.hasNext())
						retPatch.add(iter.next().toString());
				}else{
					retPatch.add(token);
				}
			}
			while(iter.hasNext())
				retPatch.add(iter.next().toString());
			pre = pos;
			npre = npos;
			ppre = ppos;
		}
		if(pre < codeIdentifiers.size()){
			int pos = codeIdentifiers.size();
			int npos = matchIdentifiers.size();
			int ppos = patchIdentifiers.size();
			Iterator iter0 = codeIdentifiers.subList(pre, pos).iterator();
			Iterator iter = matchIdentifiers.subList(npre, npos).iterator();
			String matchToken = iter0.hasNext() ? iter0.next().toString() : "";
			for(int i = ppre; i < ppos; ++i){
				String token = patchIdentifiers.get(i);
				if(matchToken.equals(token)){
					if(iter0.hasNext())
						matchToken = iter0.next().toString();
					if(iter.hasNext())
						retPatch.add(iter.next().toString());
				}else{
					retPatch.add(token);
				}
			}
			while(iter.hasNext())
				retPatch.add(iter.next().toString());
		}
		return getPatchHead(patch) + "$" + String.join(" ", retPatch);
	}

	static class Edit{
		public List<Integer> from;
		public List<Integer> to;
		public int p;
		public Edit(){
			this.from = new ArrayList<Integer>();
			this.to = new ArrayList<Integer>();
			this.p = 0;
		}
	}
	public static int shrink_pos(List<String> full, List<String> part, int pos){
		int npos = 0;
		Iterator<String> iter = part.iterator();
		String match = iter.next();
		for(String s : full.subList(0, pos)){
			if(match.equals(s)){
				npos++;
				match = iter.next();
			}
		}
		return npos;
	}
	public static void inspectMapping(HashMap<Integer, Integer> mapping, List<String> f, List<String> s){
		System.out.println("\n\nInspect: " + mapping);
		for(Integer i : mapping.keySet())
			System.out.print(i + ":\"" + f.get(i) + "\" -> " + mapping.get(i) + ":\"" + s.get(mapping.get(i)) + "\" ");
		System.out.println("\n\n");
	}
	public static int getMaxFromSet(Set<Integer> set){
		int max = Integer.MIN_VALUE;
		for (int number : set) {
		    if (number > max) {
			max = number;
		    }
		}
		return max;
	}
	public static String patchExtend_strategy_5(ITree node1, ITree node2, ITree patchNode1, String patch, ITree srcNode1, ITree srcNode2) throws IllegalStateException{
		String ret = getPatchContent(patch);
		//String ret = getPatchContent(patchExtend_strategy_2(node1, node2, patchNode1, patch, srcNode1, srcNode2));
		List<String> patchIdentifiers = tokenize(ret);
		List<String> codeIdentifiers = tokenize(getSrc(node1));
		List<String> matchIdentifiers = tokenize(getSrc(node2));
		List<String> retPatch = tokenize(getSrc(node2));
		List<String> _patchIdentifiers = getIdentifiers(patchNode1);
		List<String> _codeIdentifiers = getIdentifiers(node1);
		List<String> _matchIdentifiers = getIdentifiers(node2);
		List<String> astIdentifiers = getIdentifiers(srcNode1);
		matchRemove(astIdentifiers, _codeIdentifiers);
		List<String> astMatchIdentifiers = getIdentifiers(srcNode2);
		matchRemove(astMatchIdentifiers, _matchIdentifiers);
		HashMap<Integer, Integer> mapping = positionMap(codeIdentifiers, matchIdentifiers);
		HashMap<Integer, Integer> _mapping = positionMap(_codeIdentifiers, _matchIdentifiers);
		HashMap<Integer, Integer> mapping_patch = positionMap(codeIdentifiers, patchIdentifiers);
		List<Edit> edits = new ArrayList<Edit>();
		Integer[] keysPos = mapping_patch.keySet().toArray(new Integer[0]);
		Arrays.sort(keysPos);
		int pre = 0;
		int ppre = 0;
		//System.out.println(patch);
		//inspectMapping(mapping, codeIdentifiers, matchIdentifiers);
		//inspectMapping(mapping_patch, codeIdentifiers, patchIdentifiers);
		int maxMappingKey = getMaxFromSet(mapping.keySet());
		for(Integer _pos : keysPos){
			int pos = _pos.intValue();
			int ppos = mapping_patch.get(pos);
			Edit edit = new Edit();
			for(int i = pre; i < pos; ++i)
				edit.from.add(i);
			for(int i = ppre; i < ppos; ++i)
				edit.to.add(i);
			//pre = pos + 1;
			//ppre = ppos + 1;
			pre = pos;
			ppre = ppos;
			if(mapping.containsKey(pos)){
				edit.p = mapping.get(pos);
			}else{
				int loop = pos;
				// not limited loop may increment until overflow
				if(loop > maxMappingKey)
					throw new IllegalStateException("No key is in mapping");
				while(!mapping.containsKey(loop))
					loop++;
				if(loop == pos)
					edit.p = mapping.get(getNearest(pos, mapping.keySet()));
				else
					edit.p = mapping.get(loop);
			}
			edits.add(edit);
		}
		Edit _edit = new Edit();
		for(int i = pre; i < codeIdentifiers.size(); ++i)
			_edit.from.add(i);
		for(int i = ppre; i < patchIdentifiers.size(); ++i)
			_edit.to.add(i);
		edits.add(_edit);
		int pointer = 0;
		int _offset = 0;
		//System.out.println(patch);
		//System.out.println(mapping_patch);
		for(Edit edit : edits){
		//System.out.println(edit.from);
		//System.out.println(edit.to);
			if(pointer < edit.p)
				pointer = edit.p;
			if(edit.from.size() == 0 && edit.to.size() != 0){
				for(int i = 0; i < edit.to.size(); ++i){
					retPatch.add(pointer + _offset, patchIdentifiers.get(edit.to.get(i)));
					_offset ++;
				}
			}
			Iterator<Integer> iter = edit.to.iterator();
			for(Integer i : edit.from){
				int pos = i.intValue();
				int mpos = mapping.getOrDefault(pos, -1);
				if(mpos < 0){
					if(!iter.hasNext()){
						retPatch.set(pointer + _offset, "");
						pointer++;
						continue;
					}
					String pat = patchIdentifiers.get(iter.next().intValue());
					int posInAST = locateSequence(astIdentifiers, _codeIdentifiers);
					pos = shrink_pos(codeIdentifiers, _codeIdentifiers, pos);
					int offset = nearestOffset(posInAST, astIdentifiers, pat, _codeIdentifiers.get(pos));
					int nearTokenPos = getNearest(pos, _mapping.keySet());
					String matchToken;
					if(nearTokenPos == -1)
						matchToken = _matchIdentifiers.get(pos);
					matchToken = _matchIdentifiers.get(_mapping.get(nearTokenPos) + pos - nearTokenPos);
					//String old = retPatch.get(pointer + _offset);
					retPatch.set(pointer + _offset, astMatchIdentifiers.get(findNearest(posInAST, astMatchIdentifiers, matchToken) + offset));
					//System.out.println(old + " is set to " + astMatchIdentifiers.get(findNearest(posInAST, astMatchIdentifiers, matchToken) + offset)); 
					pointer++;
					continue;
				}
				String toSet = iter.hasNext() ? patchIdentifiers.get(iter.next().intValue()) :  "";
				//String old = retPatch.get(mpos + _offset);
				retPatch.set(mpos + _offset, toSet);
				//System.out.println(old + " is set 634 to " + toSet);
				pointer = mpos + 1;
			}
			while(iter.hasNext()){
				retPatch.add(pointer + _offset, patchIdentifiers.get(iter.next().intValue()));
				_offset ++;
			}
		}
		return getPatchHead(patch) + "$" + String.join(" ", retPatch);
	}

    public static String patchExtend(ITree node1, ITree node2, ITree patchNode1, String patch){
	    ITree root = node1;
	    while(!root.isRoot()){
		    root = root.getParent();
	    }
	    ITree root1 = node2;
	    while(!root1.isRoot()){
		    root1 = root1.getParent();
	    }
	    return patchExtend(node1, node2, patchNode1, patch, root, root1);
    }

    public static String patchExtend(ITree node1, ITree node2, ITree patchNode1, String patch, ITree srcNode1, ITree srcNode2){
	if(compressSpace(getSrc(node1)).equals(compressSpace(getSrc(node2)))){
		return patchExtend_strategy_1(node1, node2, patchNode1, patch);
	}
	if(getPatchType(patch).equals("replace")){
		//return patchExtend_strategy_2(node1, node2, patchNode1, patch, srcNode1, srcNode2);
		try{
			return patchExtend_strategy_5(node1, node2, patchNode1, patch, srcNode1, srcNode2);
		}catch(IllegalStateException e){
			return patchExtend_strategy_2(node1, node2, patchNode1, patch, srcNode1, srcNode2);
		}
	}
	return patchExtend_strategy_3(node1, node2, patchNode1, patch);
    }
}
