import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * 简易 SQL 查询工具（UTF-8 输出，避免 Windows 控制台乱码）。
 * 用法：java -cp <postgresql-jar> SqlQuery.java <jdbcUrl> <user> <password> <sqlFile|sql>
 */
public class SqlQuery {

    public static void main(String[] args) throws Exception {
        String url = args[0], user = args[1], password = args[2];
        String sql = args[3];
        try {
            if (Files.exists(Path.of(sql))) {          // SQL 含 * 等字符时 Path.of 会抛异常，需兜住
                sql = Files.readString(Path.of(sql), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // 不是文件路径，按 SQL 文本处理
        }
        Print.setOut();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement()) {
            boolean hasResult = st.execute(sql);
            do {
                if (hasResult) {
                    try (ResultSet rs = st.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int n = md.getColumnCount();
                        for (int i = 1; i <= n; i++) {
                            System.out.print(pad(md.getColumnLabel(i), 18));
                        }
                        System.out.println();
                        System.out.println("-".repeat(18 * n));
                        while (rs.next()) {
                            for (int i = 1; i <= n; i++) {
                                System.out.print(pad(String.valueOf(rs.getObject(i)), 18));
                            }
                            System.out.println();
                        }
                    }
                } else if (st.getUpdateCount() >= 0) {
                    System.out.println("受影响行数: " + st.getUpdateCount());
                }
                hasResult = st.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
            } while (hasResult || st.getUpdateCount() >= 0);
        }
    }

    private static String pad(String s, int w) {
        if (s == null) s = "null";
        int len = 0;
        for (char c : s.toCharArray()) {
            len += (c > 0x2E80) ? 2 : 1;   // 中日韩宽字符按 2 宽度计
        }
        StringBuilder sb = new StringBuilder(s);
        for (int i = len; i < w; i++) sb.append(' ');
        return sb.toString();
    }

    static class Print {
        static void setOut() throws Exception {
            System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out),
                    true, "UTF-8"));
        }
    }
}
