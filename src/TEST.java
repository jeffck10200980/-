import java.sql.ResultSet;

public class TEST {
    public static void main(String[] args) throws Exception {
        ResultSet rs = DBConnect.selectQuery("SELECT * FROM customers"); // 移除 query:
        while (rs.next()) {
            System.out.println(
                    rs.getInt("CustomersID") + " - " + rs.getString("CustomersName")); // 移除 columnlabel:
        }
    }
}
