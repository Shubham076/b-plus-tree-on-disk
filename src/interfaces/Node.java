package interfaces;

import models.Column;
import models.Row;

import java.io.IOException;
import java.util.List;

public interface Node {
    public boolean hasSpace();
    public int getNodeNo();
    public void setNodeNo(int n);
    public int getParent();
    public void setParent(int parent);
    public List<Integer> getKeys();
    public void setKeys(List<Integer> keys);
    public boolean isLeafNode();
    public void setIsLeafNode(boolean value);
    public byte[] pack(List<Column<?>> columns) throws IOException;
    public void unpack(List<Column<?>> columns, byte[] data);
}
