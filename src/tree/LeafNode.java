package tree;

import interfaces.Node;
import models.Column;
import models.Row;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeafNode implements Node {
    private final int maxKeys;
    private final int pageSize;
    private int nodeNo;
    private int next;

    private Boolean isLeafNode;
    private int parent;
    private List<Integer> keys;
    private List<Row> values;

    public LeafNode(int nodeNo, int pageSize, int maxRows) {
        this.nodeNo = nodeNo;
        this.pageSize = pageSize;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.isLeafNode = true;
        this.maxKeys = maxRows;
        this.next = -1;
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

    public List<Row> getValues() {
        return values;
    }

    public void setValues(List<Row> values) {
        this.values = values;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    private byte[] getByteArray(Object value, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        switch (value) {
            case Integer i -> buffer.putInt(i);
            case Long l -> buffer.putLong(l);
            case Short i -> buffer.putShort(i);
            case Byte b -> buffer.put(b);
            case Double v -> buffer.putDouble(v);
            case Float v -> buffer.putFloat(v);
            case String s -> {
                byte[] stringBytes = s.getBytes();
                if (stringBytes.length >= size) {
                    buffer.put(stringBytes, 0, size); // Truncate or fit exactly
                } else {
                    buffer.put(stringBytes);
                    buffer.position(size);
                }
            }
            case Boolean b -> buffer.put((byte) ((boolean) value ? 1 : 0));
            case ZonedDateTime zonedDateTime -> {
                long timestamp = zonedDateTime.toEpochSecond();
                buffer.putLong(timestamp);
            }
            case null, default ->
                    throw new IllegalArgumentException("Unsupported number type or size: " + value.getClass() + " with size " + size);
        }

        // Adjust the buffer size if necessary
        if (buffer.position() < size) {
            byte[] padding = new byte[size - buffer.position()];
            buffer.put(padding);
        }
        return buffer.array();
    }

    public byte[] pack(List<Column<?>> columns) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);
        buffer.putInt(nodeNo); // 4byte node number
        buffer.putInt(parent); // 4byte parent node number
        buffer.putInt(this.keys.size()); // 4byte number of keys
        buffer.put((byte) ((boolean) this.isLeafNode ? 1 : 0)); // 1byte
        buffer.putInt(next); // pointer to next leaf 4 byte


        // add all the keys
        for (Integer key : this.keys) {
            int size = Integer.BYTES;
            byte[] data = getByteArray(key, size);
            buffer.put(data);
        }

        // add all the values
        for (Row row : this.values) {
            for (Column<?> col : columns) {
                String colName = col.getName();
                int size = col.getSize();
                Object value = row.get(colName);
                byte[] data = getByteArray(value, size);
                buffer.put(data);
            }
        }
        return buffer.array();
    }

    private Object getObject(ByteBuffer buffer, Class<?> colType, int size) {
        if (colType == Integer.class) {
            return buffer.getInt();
        } else if (colType == Long.class) {
            return buffer.getLong();
        } else if (colType == Short.class) {
            return buffer.getShort();
        } else if (colType == Byte.class) {
            return buffer.get();
        } else if (colType == Double.class) {
            return buffer.getDouble();
        } else if (colType == Float.class) {
            return buffer.getFloat();
        } else if (colType == String.class) {
            byte[] stringBytes = new byte[size];
            buffer.get(stringBytes);
            return new String(stringBytes, StandardCharsets.UTF_8).trim();
        } else if (colType == Boolean.class) {
            return buffer.get() != 0;
        } else if (colType == ZonedDateTime.class) {
            long timestamp = buffer.getLong();
            return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"));
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + colType);
        }
    }

    public void unpack(List<Column<?>> columns, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        this.nodeNo = buffer.getInt(); // node number 4byte
        this.parent = buffer.getInt(); // parent node number 4byte
        int keyCount = buffer.getInt(); // number of keys 4byte
        this.isLeafNode = buffer.get() != 0; // is leaf node 1 byte
        this.next = buffer.getInt(); // next leaf node number 4 byte
        keys.clear();
        values.clear();

        // all the keys
        for (int i = 0; i < keyCount; i++) {
            Integer key = buffer.getInt();
            keys.add(key);
        }

        // all the values
        for (int i = 0; i < keyCount; i++) {
            Row row = new Row();
            row.setId(keys.get(i));
            for (Column<?> col : columns) {
                String colName = col.getName();
                Class<?> colType = col.getType();
                int size = col.getSize();
                Object value = getObject(buffer, colType, size);
                row.put(colName, value);
            }
            values.add(row);
        }
    }
}

