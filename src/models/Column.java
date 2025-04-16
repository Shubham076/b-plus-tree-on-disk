package models;
import java.time.ZonedDateTime;

public class Column<T> {
    private String name;
    private Class<T> type;
    private int size;

    public Column(String name, Class<T> type, int size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public Column(String name, Class<T> type) {
        this.name = name;
        this.type = type;
        this.size = getDefaultSize(type);
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    private static int getDefaultSize(Class<?> type) {
        if (type == Integer.class) {
            return 4;
        } else if (type == Long.class) {
            return 8;
        } else if (type == Short.class) {
            return 2;
        } else if (type == Byte.class) {
            return 1;
        } else if (type == Double.class) {
            return 8;
        } else if (type == Float.class) {
            return 4;
        } else if (type == Boolean.class) {
            return 1;
        } else if (type == ZonedDateTime.class) {
            return 8; // Assuming epoch seconds
        } else if (type == String.class) {
            throw new IllegalArgumentException("Size must be defined for String types");
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    public void validate(Object value) throws Exception {
        String colName = this.getName();
        if (!this.type.isInstance(value)) {
            throw new IllegalArgumentException("column " + colName + " is not of type " + this.type.getName());
        }
        if (this.type == String.class) {
            String strVal = (String) value;
            if (strVal.length() > this.size) {
                throw new Exception(String.format("column: %s is greater than %d bytes", this.name, this.size));
            }
        }
    }
}
