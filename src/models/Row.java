package models;

import java.util.HashMap;
import java.util.Map;

public class Row {
    private int id;
    private Map<String, Object> data = new HashMap<>();

    public Row(int id) {
        this.id = id;
    }

    public Row(int id, Map<String, Object> data) {
        this.id = id;
        this.data = data;
    }

    public Row() {}

    public void put(String columnName, Object value) {
        data.put(columnName, value);
    }

    public Object get(String columnName) {
        return data.get(columnName);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public void updateData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Row{");
        data.forEach((key, value) -> sb.append(key).append("=").append(value).append(", "));
        if (!data.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove the trailing ", "
        }
        sb.append("}");
        return sb.toString();
    }
}
