import models.Column;
import models.Row;
import storage.Table;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void insertMockData(Table table) {
        try (BufferedReader br = new BufferedReader(new FileReader("./mockData.csv"))) {
            String line;
            // skip first line
            br.readLine();
            while ((line = br.readLine()) != null) {
                // Split the line by commas (you can adjust this if your CSV uses a different delimiter)
                String[] fields = line.split(",");

                // Assuming CSV has columns in this order: id, name, active, email, created_at
                int id = Integer.parseInt(fields[0]);
                String name = fields[1];
                String email = fields[2];
                boolean active = Boolean.parseBoolean(fields[3]);
                ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("UTC"));

                // Create a new Row object and populate it with data
                Row row = new Row(id);
                row.put("id", id);
                row.put("name", name);
                row.put("active", active);
                row.put("email", email);
                row.put("created_at", createdAt);

                table.insert(row);
//                table.printTree();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        List<Column<?>> columns = new ArrayList<>();
        columns.add(new Column<Integer>("id", Integer.class));
        columns.add(new Column<String>("name", String.class, 256));
        columns.add(new Column<String>("email", String.class, 512));
        columns.add(new Column<Boolean>("active", Boolean.class));
        columns.add(new Column<ZonedDateTime>("created_at", ZonedDateTime.class));

        Table table = new Table("users", columns, 4 * 1024);
        insertMockData(table);
//        table.print();
        table.printTree();
//        int id = 999;
//        Row r = table.search(id);
//        if (r == null) {
//            System.out.println("Row: " + id + " not found");
//        } else {
//            System.out.println(r);
//        }
//        table.update(id, new HashMap<String, Object>() {
//            {
//                put("name", "shubham Dogra");
//            }
//        });
//
//        r = table.search(id);
//        if (r == null) {
//            System.out.println("Row: " + id + " not found");
//        } else {
//            System.out.println(r);
//        }
    }
}