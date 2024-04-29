package org.example.hercules;

import org.eclipse.jdt.core.dom.ASTNode;
import org.example.hercules.utils.LevenshteinDistance;

public class HerculesASTNodeSimilarityUtil {
    public static boolean nodeSimilarity(String nodeStr1,String nodeStr2){
        String[] elements1 = nodeStr1.split("###");
        int kind1 = 0;
        try{
            kind1 = Integer.parseInt(elements1[0]);
        }catch (Exception e){
            kind1 = -1;
        }
        Class kindClass1 = kind1 > 0 ? ASTNode.nodeClassForType(kind1) : null;
        try{
            String type1 = elements1[1];
        }catch (Exception e){
            System.out.println(e);
        }
        String type1 = elements1[1];
        String name1 = elements1[2];

        String[] elements2 = nodeStr2.split("###");
        int kind2 = 0;
        try{
            kind2 = Integer.parseInt(elements2[0]);
        }catch (Exception e){
            kind2 = -1;
        }
        Class kindClass2 = kind2 > 0 ? ASTNode.nodeClassForType(kind2) : null;
        String type2 = elements2[1];
        String name2 = elements2[2];

        if(kindCompatibility(kindClass1,kindClass2))
            if(typeCompatibility(type1,type2))
                if(ComputeNameSim(name1,name2))
                    return true;
        return false;
    }

    public static boolean kindCompatibility(Class kind1,Class kind2){
        if(kind1 == null && kind2 == null)
            return true;
        if(kind1 != null && kind2 != null){
            if(kind1.isAssignableFrom(kind2)||kind2.isAssignableFrom(kind1))
                return true;
        }
        return false;
    }

    public static boolean typeCompatibility(Class type1,Class type2){
        if(type1.isAssignableFrom(type2)||type2.isAssignableFrom(type1))
            return true;
        return false;
    }

    public static boolean typeCompatibility(String type1,String type2){
        if(type1.equals(type2))
            return true;
        return false;
    }

    public static boolean ComputeNameSim(String name1,String name2){
        double threshold = 0.5;
        int levenshteinDist = LevenshteinDistance.calculateDistance(name1, name2);
        double similarity = 1 - levenshteinDist*1.0/Math.max(name1.length(),name2.length());
        if(similarity > threshold)
            return true;
        return false;
    }
}
