package edu.lu.uni.serval.jdt.visitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import edu.lu.uni.serval.entity.EntityType;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.jdt.tree.TreeContext;
import edu.lu.uni.serval.tbar.utils.SuspiciousCodeParser;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AbstractJdtVisitor extends ASTVisitor {

    protected TreeContext context = new TreeContext();

    private Deque<ITree> trees = new ArrayDeque<>();

    public AbstractJdtVisitor() {
        super(true);
    }

    public TreeContext getTreeContext() {
        return context;
    }

    protected void pushNode(ASTNode n, String label, String herculesType) {
        int type = n.getNodeType();  //Hercules' kind
        String typeName = n.getClass().getSimpleName();
//        push(type, typeName, label, n.getStartPosition(), n.getLength());

        String name = SuspiciousCodeParser.readSuspiciousCode(n);
        push(type, typeName, label, herculesType, name, n.getStartPosition(), n.getLength());
    }


    protected void pushFakeNode(EntityType n, int startPosition, int length) {
        int type = -n.ordinal(); // Fake types have negative types (but does it matter ?)
        String typeName = n.name();
//        push(type, typeName, "", startPosition, length);
        push(type, typeName, "", null, "", startPosition, length);
    }

    protected void push(int type, String typeName, String label, String herculesType, String name, int startPosition, int length) {
        ITree t = context.createTree(type, label, typeName, herculesType, name);
        t.setPos(startPosition);
        t.setLength(length);

        if (trees.isEmpty())
            context.setRoot(t);
        else {
            ITree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }

        trees.push(t);
    }

//    protected void push(int type, String typeName, String label, int startPosition, int length) {
//        ITree t = context.createTree(type, label, typeName);
//        t.setPos(startPosition);
//        t.setLength(length);
//
//        if (trees.isEmpty())
//            context.setRoot(t);
//        else {
//            ITree parent = trees.peek();
//            t.setParentAndUpdateChildren(parent);
//        }
//
//        trees.push(t);
//    }

    protected ITree getCurrentParent() {
        return trees.peek();
    }

    protected void popNode() {
        trees.pop();
    }
}
