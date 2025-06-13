import java.sql.*;
import java.util.*;

public class DBConnect {
    // MySQL 連線資訊，加上 useSSL、編碼與時區設定
    private static final String URL = "jdbc:mysql://localhost:3306/程式設計寵物美容院"
            + "?useSSL=false"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8"
            + "&serverTimezone=Asia/Taipei";
    private static final String USER = "root";
    private static final String PASSWORD = "ji3cj04au4";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("⚠️ 無法載入 MySQL 驅動程式", e);
        }
    }

    /** 取得資料庫連線 */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** SELECT 查詢並回傳 ResultSet（TableDataHandler 會自己關連線） */
    public static ResultSet selectQuery(String sql) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        return stmt.executeQuery();
    }

    /** 查詢單筆資料並以 Map 回傳（適合登入驗證等用途） */
    public static Map<String, String> queryForMap(String sql, Object... params) {
        try (
                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                if (rs.next()) {
                    Map<String, String> result = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        result.put(meta.getColumnLabel(i), rs.getString(i));
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 單筆查詢失敗：" + e.getMessage(), e);
        }
        return null;
    }

    /** 查詢多筆資料並回傳 List<Map> */
    public static List<Map<String, String>> queryForList(String sql, Object... params) {
        List<Map<String, String>> resultList = new ArrayList<>();
        try (
                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getString(i));
                    }
                    resultList.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 多筆查詢失敗：" + e.getMessage(), e);
        }
        return resultList;
    }

    /** 執行 INSERT / UPDATE / DELETE 操作 */
    public static int executeUpdate(String sql, Object... params) {
        try (
                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 資料更新失敗：" + e.getMessage(), e);
        }
    }

    /** 取得某資料表的下一個 ID（MAX + 1） */
    public static int getNextId(String tableName, String columnName) {
        String sql = "SELECT MAX(" + columnName + ") FROM " + tableName;
        try (
                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /** 快速將 JSON 格式的字串解析為 Map（簡單用法） */
    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}