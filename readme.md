# B+ Tree Disk-Based Storage System

A prototype implementation of a B+ tree data structure with persistent disk storage, written in Java. This project demonstrates how to build a simple database storage engine that maintains data on disk using page-based architecture.

## 📋 Overview

This project implements a B+ tree index structure that stores data persistently on disk. Unlike in-memory data structures, this implementation uses a page-based storage system where each node (leaf or internal) is stored as a fixed-size page on disk. The system supports CRUD operations (Create, Read, Update) with data persisted in binary files.

## ✨ Features

- **B+ Tree Implementation**: Full B+ tree with internal and leaf nodes
- **Disk-Based Storage**: All data persisted to disk in binary format
- **Page-Based Architecture**: Fixed-size pages (configurable, default 4KB)
- **CRUD Operations**: 
  - Insert rows with automatic key indexing
  - Search by integer key
  - Update existing rows
  - Delete operations (key lookup implemented)
- **Type-Safe Schema**: Strongly typed columns with validation
- **Range Query Support**: Linked leaf nodes enable efficient range scans
- **Automatic Node Splitting**: Handles overflow by splitting nodes
- **Metadata Management**: Tracks root node and total pages

## 🏗️ Architecture

### Core Components

```
├── interfaces/
│   └── Node.java              # Node interface for leaf and internal nodes
├── models/
│   ├── Column.java            # Column definition with type and size
│   └── Row.java               # Row representation with key-value data
├── storage/
│   └── Table.java             # Main storage engine managing B+ tree operations
└── tree/
    ├── LeafNode.java          # Leaf nodes storing actual data
    ├── InternalNode.java      # Internal nodes for indexing
    └── MetadataNode.java      # Metadata page (page 0)
```

### Storage Layout

```
┌─────────────────────────────────────┐
│  Page 0: Metadata                   │
│  - Total pages                      │
│  - Root node number                 │
├─────────────────────────────────────┤
│  Page 1: Node (Leaf/Internal)       │
├─────────────────────────────────────┤
│  Page 2: Node (Leaf/Internal)       │
├─────────────────────────────────────┤
│  ...                                │
└─────────────────────────────────────┘
```

## 🔧 How It Works

### 1. **Page Structure**

Each page has a fixed size (default 4KB) with a header containing:
- Node number (4 bytes)
- Parent node number (4 bytes)
- Number of keys (4 bytes)
- Is leaf node flag (1 byte)
- Next pointer (4 bytes, leaf nodes only)

### 2. **Leaf Nodes**

- Store actual row data
- Contain keys and corresponding values (rows)
- Linked together for efficient range queries
- Calculate maximum keys based on row size and page size

### 3. **Internal Nodes**

- Store index keys and pointers to child nodes
- Route searches to appropriate leaf nodes
- Maintain sorted order for efficient traversal

### 4. **Insert Operation**

```
1. Find appropriate leaf node
2. If leaf has space → insert directly
3. If leaf is full → split into two nodes
4. Update parent with new key
5. If parent is full → recursively split upwards
6. Create new root if needed
```

## 🚀 Usage

### Define Schema and Create Table

```java
import models.Column;
import models.Row;
import storage.Table;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

// Define columns
List<Column<?>> columns = new ArrayList<>();
columns.add(new Column<Integer>("id", Integer.class));
columns.add(new Column<String>("name", String.class, 256));
columns.add(new Column<String>("email", String.class, 512));
columns.add(new Column<Boolean>("active", Boolean.class));
columns.add(new Column<ZonedDateTime>("created_at", ZonedDateTime.class));

// Create table with 4KB page size
Table table = new Table("users", columns, 4 * 1024);
```

### Insert Data

```java
Row row = new Row(1);
row.put("id", 1);
row.put("name", "John Doe");
row.put("email", "john@example.com");
row.put("active", true);
row.put("created_at", ZonedDateTime.now());

table.insert(row);
```

### Search Data

```java
Row result = table.search(1);
if (result != null) {
    System.out.println(result);
} else {
    System.out.println("Row not found");
}
```

### Update Data

```java
Map<String, Object> updates = new HashMap<>();
updates.put("name", "Jane Doe");
updates.put("email", "jane@example.com");

boolean success = table.update(1, updates);
```

### Delete Data

```java
boolean deleted = table.delete(1);
// Note: Currently finds the key but doesn't remove data or rebalance
```

### Print Tree Structure

```java
table.printTree();  // Visualize B+ tree structure
table.print();      // Print all rows
```

## 💾 Supported Data Types

- `Integer` (4 bytes)
- `Long` (8 bytes)
- `Short` (2 bytes)
- `Byte` (1 byte)
- `Float` (4 bytes)
- `Double` (8 bytes)
- `Boolean` (1 byte)
- `String` (variable, must specify max size)
- `ZonedDateTime` (8 bytes, stored as epoch seconds)

## 📊 Performance Characteristics

- **Search**: O(log n) - logarithmic time complexity
- **Insert**: O(log n) - includes potential node splits
- **Update**: O(log n) - search + in-place update
- **Delete**: O(log n) - currently only key lookup, full removal not implemented
- **Range Query**: O(log n + k) - where k is result set size
- **Space**: Fixed page size reduces memory fragmentation

## 🛠️ Technical Details

### Page Size Calculation

The system automatically calculates the maximum number of keys per node based on:
- Page size (configurable)
- Row size (sum of all column sizes)
- Header overhead

**Leaf Node Formula:**
```
maxKeys = (pageSize - headerSize - valueSize) / (keySize + valueSize)
```

**Internal Node Formula:**
```
maxKeys = (pageSize - headerSize - valueSize) / (keySize + valueSize)
```

### Binary Storage Format

- All data serialized using Java's `ByteBuffer`
- Fixed-size pages for O(1) page access
- Sequential writes for modified nodes
- Metadata cached in memory, synced on changes

### Concurrency

Methods marked as `synchronized` ensure thread safety for:
- Metadata updates
- Node writes
- Page allocation

## 🚧 Limitations & Future Improvements

### Current Limitations
- Delete operation finds keys but doesn't remove data or rebalance tree
- No transaction support
- No write-ahead logging (WAL)
- No buffer pool for caching pages
- Fixed internal node keys (hardcoded to 5)
- Single-threaded write operations

### Potential Improvements
- [ ] Implement full delete with node merging/rebalancing
- [ ] Add buffer pool for page caching
- [ ] Implement write-ahead logging for crash recovery
- [ ] Add transaction support with ACID guarantees
- [ ] Support secondary indexes
- [ ] Implement range queries API
- [ ] Add compression for pages
- [ ] B-link tree variant for better concurrency
- [ ] Bulk loading optimization
- [ ] Variable-length records

## 📁 File Structure

```
db/
├── src/
│   ├── Main.java                    # Example usage and testing
│   ├── interfaces/
│   │   └── Node.java                # Common node interface
│   ├── models/
│   │   ├── Column.java              # Column schema definition
│   │   └── Row.java                 # Row data structure
│   ├── storage/
│   │   └── Table.java               # Main storage engine
│   └── tree/
│       ├── InternalNode.java        # Internal node implementation
│       ├── LeafNode.java            # Leaf node implementation
│       └── MetadataNode.java        # Metadata management
├── mockData.csv                     # Sample data for testing
├── mockSmall.csv                    # Small sample dataset
└── users.bin                        # Binary storage file (generated)
```

## 🎯 Use Cases

This prototype is suitable for:
- **Learning**: Understanding B+ tree internals and disk-based storage
- **Prototyping**: Building custom database systems
- **Research**: Experimenting with index structures
- **Education**: Teaching data structures and file systems

## 🔍 Example: Loading CSV Data

```java
public static void insertMockData(Table table) {
    try (BufferedReader br = new BufferedReader(new FileReader("./mockData.csv"))) {
        String line;
        br.readLine(); // Skip header
        
        while ((line = br.readLine()) != null) {
            String[] fields = line.split(",");
            
            Row row = new Row(Integer.parseInt(fields[0]));
            row.put("id", Integer.parseInt(fields[0]));
            row.put("name", fields[1]);
            row.put("email", fields[2]);
            row.put("active", Boolean.parseBoolean(fields[3]));
            row.put("created_at", ZonedDateTime.now());
            
            table.insert(row);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

## 📤 Sample Output

Running the program with 40 records produces the following output:

```
Inserted row: 1
Inserted row: 2
...
Inserted row: 40

Table: users
Total pages: 25
Row{name=Shellie, active=false, created_at=2025-11-30T15:49:17Z[UTC], id=1, email=ssomers0@patch.com}
Row{name=Erminie, active=true, created_at=2025-11-30T15:49:17Z[UTC], id=2, email=egrout1@amazon.de}
...
Row{name=Catlee, active=true, created_at=2025-11-30T15:49:17Z[UTC], id=40, email=cgeake13@google.co.jp}

--------------B+ Tree-------------
[9 17 25 33 ] 
[3 5 7 ] [11 13 15 ] [19 21 23 ] [27 29 31 ] [35 37 ] 
[1 2 ] [3 4 ] [5 6 ] [7 8 ] [9 10 ] [11 12 ] [13 14 ] [15 16 ] [17 18 ] [19 20 ] [21 22 ] [23 24 ] [25 26 ] [27 28 ] [29 30 ] [31 32 ] [33 34 ] [35 36 ] [37 38 39 40 ] 
----------------------------------
```

The tree structure shows:
- **Level 1 (Root)**: Internal node with keys [9, 17, 25, 33] splitting the tree into 5 branches
- **Level 2**: Internal nodes managing ranges of keys
- **Level 3 (Leaves)**: Leaf nodes containing actual data (2-4 rows per page)

## 📖 Further Reading

- [B+ Tree - Wikipedia](https://en.wikipedia.org/wiki/B%2B_tree)
- [Database Internals by Alex Petrov](https://www.databass.dev/)
- [CMU Database Systems Course](https://15445.courses.cs.cmu.edu/)

## 📝 License

This is a prototype project for educational purposes.

## 🤝 Contributing

This is a prototype project. Feel free to fork and experiment with improvements!

---

**Note**: This is a prototype implementation for learning purposes. For production use, consider established database systems like PostgreSQL, MySQL, or embedded databases like SQLite.
