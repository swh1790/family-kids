import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库初始化工具：直接通过 JDBC 执行 db/schema.sql 与 db/data.sql。
 * 用法：
 *   java -cp <postgresql-jar> DbInit.java <jdbcUrl> <user> <password> <file1> [file2 ...]
 * 示例：
 *   java -cp postgresql-42.7.4.jar DbInit.java ^
 *        "jdbc:postgresql://localhost:5432/ontobricks" ontobricks devpass ^
 *        ..\..\src\main\resources\db\schema.sql ..\..\src\main\resources\db\data.sql
 */
public class DbInit {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("参数不足：需要 jdbcUrl user password sqlFile...");
            System.exit(1);
        }
        String url = args[0];
        String user = args[1];
        String password = args[2];

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            try (Statement st = conn.createStatement()) {
                st.execute("SET client_encoding TO 'UTF8'");
            }
            for (int i = 3; i < args.length; i++) {
                Path p = Path.of(args[i]);
                String sql = Files.readString(p, StandardCharsets.UTF_8);
                long start = System.currentTimeMillis();
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                }
                System.out.println("[OK] " + p.getFileName() + " (" + (System.currentTimeMillis() - start) + "ms)");
            }
            // 打印校验结果
            try (Statement st = conn.createStatement()) {
                for (String table : new String[]{"child", "pet", "pet_level", "task_category",
                        "task_template", "task_instance", "habit_item", "point_record",
                        "reward_item", "reward_claim", "pet_log"}) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM family_kids." + table)) {
                        rs.next();
                        System.out.printf("  %-16s %d 行%n", table, rs.getInt(1));
                    }
                }
            }
        }
        System.out.println("数据库初始化完成。");
    }
}
