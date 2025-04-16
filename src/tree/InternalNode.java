package tree;

import interfaces.Node;
import models.Column;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class InternalNode implements Node {
    private final int maxKeys;
    private final int pageSize;
    private int nodeNo;
    private Boolean isLeafNode;
    private int parent;
    private List<Integer> keys;
    private List<Integer> values;

    public InternalNode(int pageNo, int pageSize, int maxRows) {
        this.nodeNo = pageNo;
        this.pageSize = pageSize;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.isLeafNode = false;
        this.maxKeys = maxRows;
        this.parent = -1;
    }

    public boolean isLeafNode() {
        return isLeafNode;
    }

    public void setIsLeafNode(boolean leafNode) {
        isLeafNode = leafNode;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public List<Integer> getKeys() {
        return keys;
    }

    public void setKeys(List<Integer> keys) {
        this.keys = keys;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public boolean hasSpace() {
        return keys.size() < this.maxKeys;
    }

    @Override
    public int getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    private byte[] getByteArray(Integer value, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        return buffer.putInt(value).array();
    }

    public byte[] pack(List<Column<?>> columns) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);
        buffer.putInt(this.nodeNo); // 4 bytes node number
        buffer.putInt(this.parent); // 4 bytes parent node number
        buffer.putInt(this.keys.size()); // 4 bytes no of keys
        buffer.put((byte) ((boolean) this.isLeafNode ? 1 : 0));  // 1 byte is lead node

        // add all the keys
        for (Integer key : this.keys) {
            int size = Integer.BYTES;
            byte[] data = getByteArray(key, size);
            buffer.put(data);
        }

        // add all the values
        for (Integer value : this.values) {
            int size = Integer.BYTES;
            byte[] data = getByteArray(value, size);
            buffer.put(data);
        }
        return buffer.array();
    }

    public void unpack(List<Column<?>> columns, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        this.nodeNo = buffer.getInt(); // 4 bytes node number
        this.parent = buffer.getInt(); // 4 bytes parent node number
        int keyCount = buffer.getInt(); // 4 bytes for number of keys
        this.isLeafNode = buffer.get() != 0; // 1 bytes is leaf node
        keys.clear();
        values.clear();

        // all the keys
        for (int i = 0; i < keyCount; i++) {
            Integer key = buffer.getInt();
            keys.add(key);
        }

        // all the values
        for (int i = 0; i < keyCount + 1; i++) {
            Integer value = buffer.getInt();
            values.add(value);
        }
    }
}


