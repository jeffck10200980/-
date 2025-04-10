import java.sql.*;

public class DBConnect {
    // MySQL 連線資訊
    private static final String URL = "jdbc:mysql://localhost:3306/程式設計寵物美容院";
    private static final String USER = "root";
    private static final String PASSWORD = "ji3cj04au4";

    static {
        try {
            // 載入 MySQL 驅動
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("⚠️ 無法載入 MySQL 驅動程式", e);
        }
    }

    /**
     * 取得資料庫連線
     *
     * @return Connection 資料庫連線物件
     * @throws SQLException 如果連線失敗，拋出 SQLException
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 執行 SELECT 查詢
     *
     * @param query  SQL 查詢語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return ResultSet 查詢結果
     */
    public static ResultSet selectQuery(String query, Object... params) {
        try {
            // 連線
            Connection conn = getConnection(); // Use the new getConnection() method
            System.out.println("成功連線MySQL！");

            // 下查詢語法
            PreparedStatement stmt = conn.prepareStatement(query);

            // 執行查詢並回傳
            return stmt.executeQuery(); // ⚠️ 注意：ResultSet 需要手動關閉
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 查詢失敗：" + e.getMessage(), e);
        }
    }

    /**
     * 執行 INSERT / UPDATE / DELETE 操作
     *
     * @param query  SQL 語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return 受影響的行數
     */
    public static int executeUpdate(String query, Object... params) {
        try (Connection conn = getConnection(); // Use the new getConnection() method
                PreparedStatement stmt = conn.prepareStatement(query)) {

            // 設定參數
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("⚠️ 資料更新失敗：" + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {

        // 測試 SELECT 查詢
        try (ResultSet rs = selectQuery("SELECT * FROM customers")) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getString("CustomersID") + ", Name: " + rs.getString("CustomersName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 測試 INSERT
        int insertResult = executeUpdate("INSERT INTO order (idOrders, ProductName) VALUES (?, ?)", "102",
                "Keyboard");
        System.out.println("🔹 插入成功，影響行數：" + insertResult);

        // 測試 UPDATE
        int updateResult = executeUpdate("UPDATE order_v1 SET ProductName = ? WHERE idOrders = ?", "Gaming Keyboard",
                "102");
        System.out.println("🔹 更新成功，影響行數：" + updateResult);

        // 測試 DELETE
        int deleteResult = executeUpdate("DELETE FROM order_v1 WHERE idOrders = ?",
                "102");
        System.out.println("🔹 刪除成功，影響行數：" + deleteResult);
    }
}
