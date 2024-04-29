package edu.lu.uni.serval.jdt.tree;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Forest or the complete tree of the complete Java code.
 */
public class TreeContext {

    private Map<Integer, String> typeLabels = new HashMap<>();
    private Map<String, Class> typeClasses = new HashMap<>();
    private Map<Integer, Class> kindClasses = new HashMap<>();

    private final Map<String, Object> metadata = new HashMap<>();

//    private final MetadataSerializers serializers = new MetadataSerializers();

    private ITree root;

    @Override
    public String toString() {
        return "";//TreeIoUtils.toLisp(this).toString(); //TODO
    }

    public void setRoot(ITree root) {
        this.root = root;
    }

    public ITree getRoot() {
        return root;
    }

    public String getKindLabel(ITree tree) {
        return getKindLabel(tree.getType());
    }

    public String getKindLabel(int type) {
        String tl = typeLabels.get(type);
        if (tl == null)
            tl = Integer.toString(type);
        return tl;
    }

    protected void registerTypeLabel(int type, String labelStr) {
        if (labelStr == null || labelStr.equals(ITree.NO_LABEL)) // FIXME: here might be buggy.
            return;
        String typeLabel = typeLabels.get(type);
        if (typeLabel == null)
            typeLabels.put(type, labelStr);
        else if (!typeLabel.equals(labelStr))
            throw new RuntimeException(String.format("Redefining type %d: '%s' with '%s'", type, typeLabel, labelStr));
    }

    //Hercules kind table
    protected void registerTypeClass(int kind, Class kindClass) {
        if (kindClass == null) // FIXME: here might be buggy.
            return;
        Class aclass = kindClasses.get(kind);
        if (aclass == null)
            kindClasses.put(kind, kindClass);
        else if (!aclass.equals(kindClass))
            throw new RuntimeException(String.format("Redefining kingClass %d: '%s' with '%s'", kind, aclass, kindClass));
    }

    //Hercules type table
    protected void registerHerculesTypeClass(String type, Class typeClass) {
        if (typeClass == null) // FIXME: here might be buggy.
            return;
        Class aclass = typeClasses.get(type);
        if (aclass == null)
            typeClasses.put(type, typeClass);
        else if (!aclass.equals(typeClass))
            throw new RuntimeException(String.format("Redefining kingClass %d: '%s' with '%s'", type, aclass, typeClass));
    }

    public ITree createTree(int type, String label, String typeLabel) {//TODO: why does it use different label?
        registerTypeLabel(type, typeLabel);
        return new Tree(type, label);

    }

    //Create Hercules Tree
    public ITree createTree(int type, String label, String typeLabel, String herculesType, String name) {//TODO: why does it use different label?
        registerTypeLabel(type, typeLabel);
        if(type > 0){
            registerTypeClass(type, ASTNode.nodeClassForType(type));
        }

        registerHerculesTypeClass(herculesType, null);
        return new Tree(type, herculesType, name, label);

    }

    public ITree createTree(ITree... trees) {
        return new AbstractTree.FakeTree(trees);
    }

    public void validate() {
        root.refresh();
        TreeUtils.postOrderNumbering(root);
    }

    public boolean hasLabelFor(int type) {
        return typeLabels.containsKey(type);
    }

    /**
     * Get a global metadata.
     * There is no way to know if the metadata is really null or does not exists.
     *
     * @param key of metadata
     * @return the metadata or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Get a local metadata, if available. Otherwise get a global metadata.
     * There is no way to know if the metadata is really null or does not exists.
     *
     * @param key of metadata
     * @return the metadata or null if not found
     */
    public Object getMetadata(ITree node, String key) {
        Object metadata;
        if (node == null || (metadata = node.getMetadata(key)) == null)
            return getMetadata(key);
        return metadata;
    }

    /**
     * Store a global metadata.
     *
     * @param key   of the metadata
     * @param value of the metadata
     * @return the previous value of metadata if existed or null
     */
    public Object setMetadata(String key, Object value) {
        return metadata.put(key, value);
    }

    /**
     * Store a local metadata
     *
     * @param key   of the metadata
     * @param value of the metadata
     * @return the previous value of metadata if existed or null
     */
    public Object setMetadata(ITree node, String key, Object value) {
        if (node == null)
            return setMetadata(key, value);
        else {
            Object res = node.setMetadata(key, value);
            if (res == null)
                return getMetadata(key);
            return res;
        }
    }

    /**
     * Get an iterator on global metadata only
     */
    public Iterator<Entry<String, Object>> getMetadata() {
        return metadata.entrySet().iterator();
    }

    /**
     * Get serializers for this tree context
     */
//    public MetadataSerializers getSerializers() {
//        return serializers;
//    }
//
//    public TreeContext export(MetadataSerializers s) {
//        serializers.addAll(s);
//        return this;
//    }
//
//    public TreeContext export(String key, MetadataSerializer s) {
//        serializers.add(key, s);
//        return this;
//    }
    public TreeContext export(String... name) {
//        for (String n : name)
//            serializers.add(n, x -> x.toString());
        return this;
    }

    public TreeContext deriveTree() { // FIXME Should we refactor TreeContext class to allow shared metadata etc ...
        TreeContext newContext = new TreeContext();
        newContext.setRoot(getRoot().deepCopy());
        newContext.typeLabels = typeLabels;
        newContext.metadata.putAll(metadata);
//        newContext.serializers.addAll(serializers);
        return newContext;
    }

    /**
     * Get an iterator on local and global metadata.
     * To only get local metadata, simply use : `node.getMetadata()`
     */
    public Iterator<Entry<String, Object>> getMetadata(final ITree node) {
        if (node == null)
            return getMetadata();
        return new Iterator<Entry<String, Object>>() {
            final Iterator<Entry<String, Object>> localIterator = node.getMetadata();
            final Iterator<Entry<String, Object>> globalIterator = getMetadata();
            final Set<String> seenKeys = new HashSet<>();

            Iterator<Entry<String, Object>> currentIterator = localIterator;
            Entry<String, Object> nextEntry;

            {
                next();
            }

            @Override
            public boolean hasNext() {
                return nextEntry != null;
            }

            @Override
            public Entry<String, Object> next() {
                Entry<String, Object> n = nextEntry;
                if (currentIterator == localIterator) {
                    if (localIterator.hasNext()) {
                        nextEntry = localIterator.next();
                        seenKeys.add(nextEntry.getKey());
                        return n;
                    } else {
                        currentIterator = globalIterator;
                    }
                }
                nextEntry = null;
                while (globalIterator.hasNext()) {
                    Entry<String, Object> e = globalIterator.next();
                    if (!(seenKeys.contains(e.getKey()) || (e.getValue() == null))) {
                        nextEntry = e;
                        seenKeys.add(nextEntry.getKey());
                        break;
                    }
                }
                return n;
            }

            @Override
            public void remove() {
                // TODO Auto-generated method stub

            }
        };
    }

    public static class Marshallers<E> {
        Map<String, E> serializers = new HashMap<>();

        public static final Pattern valid_id = Pattern.compile("[a-zA-Z0-9_]*");

        public void addAll(Marshallers<E> other) {
//            addAll(other.serializers);
        }

//        public void addAll(Map<String, E> serializers) {
//            serializers.forEach((k, s) -> add(k, s));
//        }

        public void add(String name, E serializer) {
            if (!valid_id.matcher(name).matches()) // TODO I definitely don't like this rule, we should think twice
                throw new RuntimeException("Invalid key for serialization");
            serializers.put(name, serializer);
        }

        public void remove(String key) {
            serializers.remove(key);
        }

        public Set<String> exports() {
            return serializers.keySet();
        }
    }

//    public static class MetadataSerializers extends Marshallers<MetadataSerializer> {
//
//        public void serialize(TreeFormatter formatter, String key, Object value) throws Exception {
//            MetadataSerializer s = serializers.get(key);
//            if (s != null)
//                formatter.serializeAttribute(key, s.toString(value));
//        }
//    }
//
//    public static class MetadataUnserializers extends Marshallers<MetadataUnserializer> {
//
//        public void load(ITree tree, String key, String value) throws Exception {
//            MetadataUnserializer s = serializers.get(key);
//            if (s != null) {
//                if (key.equals("pos"))
//                    tree.setPos(Integer.parseInt(value));
//                else if (key.equals("length"))
//                    tree.setLength(Integer.parseInt(value));
////                if (key.equals("line_before"))
////                    tree.setPos(Integer.parseInt(value));
////                else if (key.equals("line_after"))
////                    tree.setLength(Integer.parseInt(value));
//                else
//                    tree.setMetadata(key, s.fromString(value));
//            }
//        }
//    }
}
