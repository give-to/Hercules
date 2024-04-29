package org.example.hercules;

import distance.EditBasedDist;
import tree.LblTree;

import java.util.Arrays;
import java.util.Enumeration;

public class HerculesEditDist extends EditBasedDist {

    // call initKeyroots to init the following arrays!
    int[] kr1;  // LR_keyroots(t1)={k | there is no k'>k such that l(k)=l(k')}
    int[] kr2;  // LR_keyroots(t2)
    int[] l1;   // l(t1) = post-order number of left-most leaf
    //   descendant of i-th node in post-order in t1
    int[] l2;   // l(t2)
    String[] lbl1; // label of i-th node in postorder of t1
    String[] lbl2; // label of i-th node in postorder of t2

    double[][] treedist; // intermediate treedist results
    double[][] forestdist; // intermediate forest dist results

    public HerculesEditDist(boolean normalized) {
        this(1, 1, 1, normalized);
    }

    public HerculesEditDist(double ins, double del, double update, boolean normalized) {
        super(ins, del, update, normalized);
    }

    // inits kr (keyroots), l (left-most leaves), lbl (labels) with t (tree)
    private static void init(int[] kr, int[] l, String[] lbl, LblTree t) {
        int i = 1;
        for (Enumeration e = t.postorderEnumeration(); e.hasMoreElements(); ) {
            LblTree n = (LblTree) e.nextElement();
            // add postorder number to node
            n.setTmpData(new Integer(i));
            // label
            lbl[i] = n.getLabel();
            // left-most leaf
            l[i] = ((Integer) ((LblTree) n.getFirstLeaf()).getTmpData()).intValue();
            i++;
        }
        boolean[] visited = new boolean[l.length];
        Arrays.fill(visited, false);
        int k = kr.length - 1;
        for (i = l.length - 1; i >= 0; i--) {
            if (!visited[l[i]]) {
                kr[k] = i;
                visited[l[i]] = true;
                k--;
            }
        }
        t.clearTmpData();
    }

    @Override
    public double nonNormalizedTreeDist(LblTree t1, LblTree t2) {

        // System.out.print(t1.getTreeID() + "|" + t2.getTreeID() + "|");

        int nc1 = t1.getNodeCount() + 1;
        kr1 = new int[t1.getLeafCount() + 1];
        l1 = new int[nc1];
        lbl1 = new String[nc1];

        int nc2 = t2.getNodeCount() + 1;
        kr2 = new int[t2.getLeafCount() + 1];
        l2 = new int[nc2];
        lbl2 = new String[nc2];

        init(kr1, l1, lbl1, t1);
        init(kr2, l2, lbl2, t2);

        treedist = new double[nc1][nc2];
        forestdist = new double[nc1][nc2];

        for (int i = 1; i < kr1.length; i++) {
            for (int j = 1; j < kr2.length; j++) {
                treeEditDist(kr1[i], kr2[j]);
                //printMatrix(forestdist);
            }
        }
        //printMatrix(treedist);
        // System.out.println(treedist[nc1 - 1][nc2 - 1]);
        return treedist[nc1 - 1][nc2 - 1];
    }

    private void treeEditDist(int i, int j) {
        forestdist[l1[i] - 1][l2[j] - 1] = 0;
        for (int i1 = l1[i]; i1 <= i; i1++) {
            forestdist[i1][l2[j] - 1] = forestdist[i1 - 1][l2[j] - 1] + this.getDel();
            for (int j1 = l2[j]; j1 <= j; j1++) {
                forestdist[l1[i] - 1][j1] = forestdist[l1[i] - 1][j1 - 1] + this.getIns();
                double wDel = this.getDel();
                double wIns = this.getIns();

                if ((l1[i1] == l1[i]) && (l2[j1] == l2[j])) {
                    double u = 0;
//                    if (!lbl1[i1].equals(lbl2[j1])) {
//                        u = this.getUpdate();
//                    }
                    if(!HerculesASTNodeSimilarityUtil.nodeSimilarity(lbl1[i1],lbl2[j1])){
                        u = this.getUpdate();
                    }
                    forestdist[i1][j1] = Math.min(Math.min(forestdist[i1 - 1][j1] + wDel, forestdist[i1][j1 - 1] + wIns), forestdist[i1 - 1][j1 - 1] + u);
                    treedist[i1][j1] = forestdist[i1][j1];
                } else {
                    forestdist[i1][j1] = Math.min(Math.min(forestdist[i1 - 1][j1] + wDel,forestdist[i1][j1 - 1] + wIns), forestdist[l1[i1] - 1][l2[j1] - 1] + treedist[i1][j1]);
                }
            }
        }
    }

}
