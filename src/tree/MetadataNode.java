package tree;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class MetadataNode {
    private int totalPages;
    private int rootNodeNumber;
    private final String table;
    private final int pageSize;
    private RandomAccessFile raf;

    public MetadataNode(int pageSize, String table) throws Exception {
        this.pageSize = pageSize;
        this.table = table;
        rootNodeNumber = 0;
    }

    private void initializeFilePointer() throws Exception {
        String path = String.format("%s.bin", this.table);
        this.raf = new RandomAccessFile(path, "rw");
    }

    public int getRootNodeNumber() {
        return rootNodeNumber;
    }

    public void setRootNodeNumber(int root) {
        this.rootNodeNumber = root;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    private byte[] getByteArray(Integer value, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        return buffer.putInt(value).array();
    }

    public byte[] pack() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(pageSize);
        buffer.putInt(this.totalPages);
        buffer.putInt(this.rootNodeNumber);
        byte[] data = buffer.array();
        if (data.length > pageSize) {
            throw new Exception("[metadata] buffer size greater than allocated page");
        }
        return buffer.array();
    }

    public void unpack(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        this.totalPages = buffer.getInt();
        this.rootNodeNumber = buffer.getInt();
    }

    public synchronized int getNextPage() throws Exception {
        if (this.raf == null) {
            this.initializeFilePointer();
        }
        this.totalPages += 1;
        raf.seek(0);
        raf.write(pack());
        return this.totalPages;
    }
}


