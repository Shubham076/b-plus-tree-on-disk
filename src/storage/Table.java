package storage;

import interfaces.Node;
import models.Column;
import models.Row;
import tree.InternalNode;
import tree.LeafNode;
import tree.MetadataNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Table {
    private static final int LEAF_PAGE_HEADER_SIZE = 17;
    private static final int INTERNAL_PAGE_HEADER_SIZE = 13;
    private String table;
    private List<Column<?>> columns;
    private HashSet<Node> updatedNodes;
    private MetadataNode metadata;
    private Node root;
    private RandomAccessFile raf;
    private final int pageSize;
    private final int maxLeafNodeKeys;
    private final int maxInternalNodeKeys;

    public Table(String name, List<Column<?>> columns, int pageSize) throws Exception {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Columns can't be empty.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Table name is required.");
        }

        this.table = name;
        this.columns = columns;
        this.pageSize = pageSize;
        this.updatedNodes = new HashSet<>();
        metadata = new MetadataNode(pageSize, name);
        this.maxLeafNodeKeys = this.getMaxKeys();
        this.maxInternalNodeKeys = 5;
        this.initializeTable();
    }

    private boolean isTablePresent() {
        Path path = Path.of(String.format("%s.bin", this.table));
        return Files.exists(path);
    }

    private void initializeTable() throws Exception {
        String path = String.format("%s.bin", this.table);
        if (this.isTablePresent()) {
            FileInputStream fis = new FileInputStream(path);
            byte[] data = new byte[pageSize];
            fis.read(data);
            metadata.unpack(data);
        } else {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(metadata.pack());
        }
        this.raf = new RandomAccessFile(path, "rw");
        // set the root node at startup
        int nodeNo = this.metadata.getRootNodeNumber();
        if (nodeNo == 0) {
            root = new LeafNode(this.metadata.getNextPage(), this.pageSize, this.maxLeafNodeKeys);
            this.metadata.setRootNodeNumber(root.getNodeNo());
            this.saveMetadata();
            this.updatedNodes.add(root);
        } else {
            root = this.getNode(nodeNo);
        }
    }

    synchronized private void saveMetadata() throws Exception {
        raf.seek(0);
        raf.write(this.metadata.pack());
    }

    synchronized public void saveData() throws Exception {
        for (Node n: this.updatedNodes) {
            this.writeNode(n);
        }
        this.updatedNodes.clear();
        System.out.println();
    }

    synchronized private void writeNode(Node node) throws Exception {
        long offset = (long) node.getNodeNo() *  this.pageSize;
        raf.seek(offset);
        raf.write(node.pack(this.columns));
    }
    /*
        assumtion: key is of type integer 4 bytes
        size of value is 4 bytes
        let's say we can store N keys
        space for N keys = 4 * N bytes

        For N + 1 values, each value requiring 4 bytes:
        Space for values= 4 × ( N + 1)

        Total Space = 4N + 4N + 4 => 8N + 4
        4096 - 13 (header size) - 4 => 8N
 */

    private int getMaxKeys() throws Exception {
        int availableSpace = this.pageSize - LEAF_PAGE_HEADER_SIZE;
        int keySize = Integer.BYTES;
        int valueSize = 0;

        for (Column<?> col : columns) {
            valueSize += col.getSize();
        }

        int maxKeys = (availableSpace - valueSize) / (keySize + valueSize);
        if (maxKeys == 0) {
            throw new Exception("Row is too big either reduce size/ number of cols or increase leaf node page size");
        }
        return maxKeys;
    }
    /*
            assumtion: key and value are of tye integer 4 bytes
            let's say we can store N keys
            space for N keys = 4 * N bytes

            For N + 1 values, each value requiring 4 bytes:
            Space for values= 4 × ( N + 1)

            Total Space = 4N + 4N + 4 => 8N + 4
            4096 - 13 (header size) - 4 => 8N
     */
    private int getMaxInternalNodeKeys() throws Exception {
        int availableSpace = this.pageSize - INTERNAL_PAGE_HEADER_SIZE;
        int keySize = Integer.BYTES;
        int valueSize = Integer.BYTES;

        // Calculate the maximum number of keys (N)
        int maxKeys = (availableSpace - valueSize) / (keySize + valueSize);

        if (maxKeys == 0) {
            throw new Exception("increase page size for internal nodes");
        }
        return maxKeys;
    }

    synchronized public void print() throws Exception {
        Path path = Paths.get(table + ".bin");
        if (Files.exists(path)) {
            if (!this.updatedNodes.isEmpty()) {
                this.saveData();
            }
            System.out.println("Table: " + this.table);
            System.out.println("Total pages: " + this.metadata.getTotalPages());
            for (int i = 1; i <= metadata.getTotalPages(); i++) {
                if (this.isLeafNode(i)) {
                    LeafNode node = this.getLeafNode(i);
//                    System.out.println("LeafNode: " + node.getNodeNo() + ", " + node.getKeys() + ", Parent: " + node.getParent());
                    for (Row row: node.getValues()) {
                        System.out.println(row);
                    }
                }
            }
        } else {
            System.out.println("No data file found.");
        }
    }

    public void printTree() throws Exception {
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(this.root.getNodeNo());
        System.out.println("--------------B+ Tree-------------");
        if (!this.updatedNodes.isEmpty()) {
            this.saveData();
        }
        while (!queue.isEmpty()) {
            int s = queue.size();
            for (int i = 0; i < s; i++) {
                int no = queue.remove();
                Node node = this.getNode(no);
                if (!node.getKeys().isEmpty()) {
                    System.out.print("[");
                    for (int key : node.getKeys()) {
                        System.out.print(key + " ");
                    }
                    System.out.print("] ");
                }
                if (!this.isLeafNode(no)) {
                    queue.addAll(((InternalNode) node).getValues());
                }
            }
            System.out.println();
        }
        System.out.println("----------------------------------");
    }

    private void validateRow(Row row, boolean update) throws Exception {
        // check for unknown columns
        for (String key: row.getData().keySet()) {
            boolean contains = this.columns.stream().anyMatch(c -> c.getName().equals(key));
            if (!contains) {
                throw new Exception("Unknown column: " + key);
            }
        }

        // check for required columns
        for (Column<?> column: this.columns) {
            if (row.containsKey(column.getName())) {
                Object value = row.get(column.getName());
                column.validate(value);
            } else {
                if (!update) {
                    throw new Exception(String.format("column: %s is required", column.getName()));
                }
            }
        }
    }

    public synchronized boolean isLeafNode(int no) throws Exception {
        // isLeafNode stored at 13th index
        raf.seek((long) no * this.pageSize + 12);
        byte[] data = new byte[1];
        int bytesRead = raf.read(data);
        if (bytesRead == -1) {
            throw new Exception("not able to check whether it is internal or leaf node: " + no);
        }
        return data[0] != 0;
    }

    synchronized private Node getNode(int no) throws Exception {
        Node node;
        // first check in updated nodes to get the latest state
        for (Node n: this.updatedNodes) {
            if (n.getNodeNo() == no) {
                return n;
            }
        }

        // fectch from disk
        if (this.isLeafNode(no)) {
            node = this.getLeafNode(no);
        } else {
            node = this.getInternalNode(no);
        }
        return node;
    }

    synchronized private InternalNode getInternalNode(int no) throws Exception {
        raf.seek((long) no * this.pageSize);
        byte[] data = new byte[this.pageSize];
        int bytesRead = raf.read(data);
        if (bytesRead == -1) {
            throw new Exception("data not present for internal node: " + no);
        }
        InternalNode node = new InternalNode(no, this.pageSize, this.maxInternalNodeKeys);
        node.unpack(null, data);
        return node;
    }

    synchronized private LeafNode getLeafNode(int no) throws Exception {
        raf.seek((long) no * this.pageSize);
        byte[] data = new byte[this.pageSize];
        int bytesRead = raf.read(data);
        if (bytesRead == -1) {
            throw new Exception("data not present for leaf node: " + no);
        }
        LeafNode node = new LeafNode(no, this.pageSize, this.maxLeafNodeKeys);
        node.unpack(this.columns, data);
        return node;
    }

    private Node findLeafNode(Node node, int searchKey) throws Exception {
        if (node.isLeafNode()) {
            return node;
        }
        // typecast to internal node
        InternalNode internalNode = (InternalNode) node;
        List<Integer> keys = node.getKeys();
        for (int i = 0; i < keys.size(); i++) {
            if (searchKey < keys.get(i)) {
                int nodeNo = internalNode.getValues().get(i);
                Node child = this.getNode(nodeNo);
                return findLeafNode(child, searchKey);
            }
        }
        List<Integer> values = internalNode.getValues();
        Node child = this.getNode(values.getLast());
        return findLeafNode(child, searchKey);
    }

    private void insertIntoLeaf(LeafNode node, int key, Row row) {
        int pos = 0;
        List<Integer> keys = node.getKeys();
        while (pos < keys.size() && keys.get(pos) < key) {
            pos++;
        }
        keys.add(pos, key);
        node.getValues().add(row);
        updatedNodes.add(node);
    }

    private void insertIntoParent(Node parent, int key, int val) {
        int pos = 0;
        List<Integer> keys = parent.getKeys();
        List<Integer> values = ((InternalNode) parent).getValues();
        while (pos < keys.size() && keys.get(pos) < key) {
            pos++;
        }
        keys.add(pos, key);

        if (values.isEmpty() || pos + 1 == values.size()) {
            // Append at the end
            values.add(val);
        } else {
            // Insert at the next position
            values.add(pos + 1, val);
        }
        this.updatedNodes.add(parent);
    }

    // Split a full internal node and insert the new key and child pointer
    private void splitParentAndInsert(Node node, Node rightChild, int key) throws Exception {
        insertIntoParent(node, key, rightChild.getNodeNo());
        InternalNode left = (InternalNode) node;
        InternalNode right = new InternalNode(this.metadata.getNextPage(), this.pageSize, this.maxInternalNodeKeys);
        int mid = (this.maxInternalNodeKeys + 1) / 2;
        int midKey = left.getKeys().get(mid);
        right.getKeys().addAll(left.getKeys().subList(mid + 1, left.getKeys().size()));
        right.getValues().addAll(left.getValues().subList(mid + 1, left.getValues().size()));

        node.getKeys().subList(mid, left.getKeys().size()).clear();
        ((InternalNode) node).getValues().subList(mid + 1, ((InternalNode) node).getValues().size()).clear();

        // update the parent
        right.setParent(left.getParent());
        // update the parent pointers of children
        for (Integer childNodeNo : right.getValues()) {
            Node child = this.getNode(childNodeNo);
            this.updatedNodes.add(child);
            child.setParent(right.getNodeNo());
        }

        this.updatedNodes.add(left);
        this.updatedNodes.add(right);
        updateParent(left, right, midKey);
    }

    private void updateParent(Node left, Node right, int key) throws Exception {
        if (left.getNodeNo() == root.getNodeNo()) {
            int no = this.metadata.getNextPage();
            InternalNode newRoot = new InternalNode(no, this.pageSize, this.maxInternalNodeKeys);
            newRoot.getKeys().add(key);
            newRoot.getValues().addAll(Arrays.asList(left.getNodeNo(), right.getNodeNo()));
            newRoot.setParent(-1);
            root = newRoot;
            left.setParent(no);
            right.setParent(no);

            this.metadata.setRootNodeNumber(no);
            // save metadata root is updated
            this.saveMetadata();
            this.updatedNodes.addAll(Arrays.asList(root, left, right));
            return;
        }

        int parentNodeNo = left.getParent();
        Node parent = this.getNode(parentNodeNo);
        if (parent.hasSpace()) {
            insertIntoParent(parent, key, right.getNodeNo());
        } else {
            splitParentAndInsert(parent, right, key);
        }

        // update the root node
        if (parent.getNodeNo() == this.root.getNodeNo()) {
            this.root = parent;
        }
    }

    private void splitLeafAndInsert(Node node, int key, Row row) throws Exception {
        int midIndex = (this.maxLeafNodeKeys + 1) / 2;
        LeafNode leafNode = (LeafNode) node;
        this.insertIntoLeaf(leafNode , key, row);
        LeafNode newLeafNode = new LeafNode(this.metadata.getNextPage(), this.pageSize, this.maxLeafNodeKeys);

        newLeafNode.getKeys().addAll(leafNode.getKeys().subList(midIndex, leafNode.getKeys().size()));
        newLeafNode.getValues().addAll(leafNode.getValues().subList(midIndex, leafNode.getValues().size()));

        leafNode.getKeys().subList(midIndex, leafNode.getKeys().size()).clear();
        leafNode.getValues().subList(midIndex, leafNode.getValues().size()).clear();

        // attaching next pointers for range queries
        newLeafNode.setNext(leafNode.getNext());
        leafNode.setNext(newLeafNode.getNodeNo());

        // Set the parent for the new leaf
        newLeafNode.setParent(leafNode.getParent());

        // set the updated nodes
        this.updatedNodes.add(leafNode);
        this.updatedNodes.add(newLeafNode);

        // update parent node
        updateParent(leafNode, newLeafNode, newLeafNode.getKeys().getFirst());
    }

    public void insert(Row row) throws Exception {
        validateRow(row, false);
        Node node = findLeafNode(this.root, row.getId());
        if (node.getKeys().contains(row.getId())) {
            System.out.printf("failed to save row: %s reason: duplicate id\n", row.getId());
            return;
        }
        if (node instanceof InternalNode) {
            throw new Exception("internal node returned by find");
        }
        if (node.hasSpace()) {
            insertIntoLeaf((LeafNode)node, row.getId(), row);
        } else {
            splitLeafAndInsert(node, row.getId(), row);
        }

        if (this.updatedNodes.size() >= 5) {
            this.saveData();
        }
        System.out.println("Inserted row: " + row.getId());
    }

    public Row search(int id) throws Exception {
        Node node = findLeafNode(this.root, id);
        if (node instanceof InternalNode) {
            throw new Exception("internal node returned by find");
        }
        LeafNode leafNode = (LeafNode) node;
        int idx = -1;
        for (int i = 0; i < node.getKeys().size(); i++) {
            if (node.getKeys().get(i) == id) {
                idx = i;
            }
        }
        if (idx == -1) {
            return null;
        } else {
            return leafNode.getValues().get(idx);
        }
    }

    public boolean update(int id, Map<String, Object> updates) throws Exception {
        updates.put("id", id);
        Row row = new Row(id, updates);
        validateRow(row, true);
        Node node = findLeafNode(this.root, id);
        if (node instanceof InternalNode) {
            throw new Exception("internal node returned by find");
        }
        LeafNode leafNode = (LeafNode) node;
        int idx = -1;
        for (int i = 0; i < node.getKeys().size(); i++) {
            if (node.getKeys().get(i) == id) {
                idx = i;
            }
        }
        if (idx == -1) {
            return false;
        } else {
            Row oldRow = leafNode.getValues().get(idx);
            oldRow.getData().putAll(updates);
            this.writeNode(node);
            return true;
        }
    }

    public boolean delete(int id) throws Exception {
        Node node = findLeafNode(this.root, id);
        if (node instanceof InternalNode) {
            throw new Exception("internal node returned by find");
        }
        int keyIndex = node.getKeys().indexOf(id);
        if (keyIndex == -1) {
            return false;
        }

        return true;
    }
}