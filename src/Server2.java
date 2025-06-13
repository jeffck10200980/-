import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.stream.Collectors;

public class Server2 {
    public static void main(String[] args) throws IOException {
        HttpServer server2 = HttpServer.create(new InetSocketAddress(8000), 0);

        // 登入
        server2.createContext("/login", new LoginHandler());

        // 客戶管理：列表、新增、刪除
        server2.createContext("/api/customers", new CustomerListHandler());
        server2.createContext("/api/customers/create", new CustomerCreateHandler());
        server2.createContext("/api/customers/delete", new CustomerDeleteHandler());

        // 寵物管理：列表、新增、刪除
        server2.createContext("/api/pets", new PetListHandler());
        server2.createContext("/api/pets/create", new PetCreateHandler());
        server2.createContext("/api/pets/delete", new PetDeleteHandler());

        // 訂單管理：列表、新增、刪除
        server2.createContext("/api/orders", new OrderListHandler());
        server2.createContext("/api/orders/create", new OrderCreateHandler());
        server2.createContext("/api/orders/delete", new OrderDeleteHandler());

        // 報表查看：每日營收、品項占比（狗貓分開）
        server2.createContext("/api/reports/daily-revenue", new DailyRevenueHandler());
        server2.createContext(
                "/api/reports/service-share/dogs",
                new ServiceShareDogsHandler());
        server2.createContext(
                "/api/reports/service-share/cats",
                new ServiceShareCatsHandler());

        server2.start();
        System.out.println("Server started at http://localhost:8000");
    }

    // ========== 登入 Handler ==========
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS & JSON header
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");

            // OPTIONS 預檢
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST,OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                exchange.getResponseBody().close();
                return;
            }
            // 只允許 POST
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
                return;
            }

            // 讀取 body
            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }

            // 解析 JSON
            Map<String, String> cred = DBConnect.parseJson(body);
            String account = cred.get("account"); // 對應 employees.Account
            String password = cred.get("password"); // 對應 employees.Password
            String role = cred.get("role"); // 對應 employees.Role

            // 查詢
            String sql = "SELECT * FROM employees WHERE Account = ? AND Password = ? AND Role = ?";
            Map<String, String> user = DBConnect.queryForMap(sql, account, password, role);

            String response;
            int status;
            if (user != null) {
                // 登入成功，回傳 name / role / ID
                response = String.format(
                        "{\"success\":true,\"message\":\"登入成功\",\"employeeName\":\"%s\",\"role\":\"%s\",\"employeeID\":\"%s\"}",
                        user.get("EmployeeName"), user.get("Role"), user.get("EmployeesID"));
                status = 200;
            } else {
                response = "{\"success\":false,\"message\":\"帳號、密碼或身分錯誤\"}";
                status = 401;
            }

            // 寫回
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ========== 客戶管理 Handlers ==========
    static class CustomerListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String response;
            try (ResultSet rs = DBConnect.selectQuery(
                    "SELECT * FROM customers WHERE deleted_at IS NULL ORDER BY CustomersID ASC")) {
                StringBuilder json = new StringBuilder("[");
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{");
                    for (int i = 1; i <= columnCount; i++) {
                        String col = meta.getColumnLabel(i);
                        String val = rs.getString(i);
                        if (i > 1)
                            json.append(",");
                        json.append("\"").append(col).append("\":\"")
                                .append(val == null ? "" : escapeJson(val)).append("\"");
                    }
                    json.append("}");
                }
                json.append("]");
                rs.getStatement().getConnection().close();
                response = json.toString();
            } catch (Exception e) {
                response = "[{\"error\":\"" + escapeJson(e.getMessage()) + "\"}]";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class CustomerCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String body;
            try (InputStream is = ex.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }
            Map<String, String> map = DBConnect.parseJson(body);
            int id = DBConnect.getNextId("customers", "CustomersID");
            DBConnect.executeUpdate(
                    "INSERT INTO customers (CustomersID, CustomersName, CustomersPhoneNumbers, CustomersMail) VALUES (?, ?, ?, ?)",
                    id, map.get("name"), map.get("phone"), map.get("mail"));
            String resp = "{\"success\":true}";
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static class CustomerDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "DELETE, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String body;
            try (InputStream is = ex.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }
            Map<String, String> map = DBConnect.parseJson(body);
            DBConnect.executeUpdate(
                    "UPDATE customers SET deleted_at = NOW() WHERE CustomersID = ?",
                    Integer.parseInt(map.get("id")));
            String resp = "{\"success\":true}";
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    // ========== 寵物管理 Handlers ==========
    // ====== 寵物列表 Handler（加入 JOIN 顯示主人姓名與分類文字） ======
    static class PetListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

            String response;
            // 使用 JOIN 把外鍵關聯文字取出，並抓取原始的數值 code
            String sql = "SELECT p.PetsID, p.PetsName, p.PetsAge, "
                    + "p.PetsCategory1 AS Category1Code, "
                    + "p.PetsCategory2 AS Category2Code, "
                    + "c.CustomersName AS Owner, "
                    + "pc.PetsCategories AS Category1, "
                    + "pcl.PetsClassification AS Category2, "
                    + "p.SpecialDiseases, p.Remark "
                    + "FROM pets p "
                    + "JOIN customers c ON p.PetsOwnersID = c.CustomersID "
                    + "JOIN petscategories pc ON p.PetsCategory1 = pc.PetsCategoriesID "
                    + "JOIN petsclassification pcl ON p.PetsCategory2 = pcl.PetsClassificationID "
                    + "WHERE p.deleted_at IS NULL "
                    + "ORDER BY p.PetsID ASC";

            try (ResultSet rs = DBConnect.selectQuery(sql)) {
                StringBuilder json = new StringBuilder("[");
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1)
                            json.append(",");
                        String col = meta.getColumnLabel(i);
                        String val = rs.getString(i);
                        json.append("\"").append(col).append("\":\"")
                                .append(val == null ? "" : escapeJson(val))
                                .append("\"");
                    }
                    json.append("}");
                }
                json.append("]");
                // 關閉連線釋放資源
                rs.getStatement().getConnection().close();
                response = json.toString();
            } catch (Exception e) {
                response = "[{\"error\":\"" + escapeJson(e.getMessage()) + "\"}]";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class PetCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String body;
            try (InputStream is = ex.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }
            Map<String, String> m = DBConnect.parseJson(body);
            int id = DBConnect.getNextId("pets", "PetsID");
            DBConnect.executeUpdate(
                    "INSERT INTO pets (PetsID, PetsName, PetsAge, PetsOwnersID, PetsCategory1, PetsCategory2, SpecialDiseases, Remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    m.get("name"),
                    m.get("age"),
                    m.get("owner"),
                    m.get("category1"),
                    m.get("category2"),
                    m.get("specialDiseases"),
                    m.get("remark"));
            String resp = "{\"success\":true}";
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static class PetDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "DELETE, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String body;
            try (InputStream is = ex.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }
            Map<String, String> m = DBConnect.parseJson(body);
            DBConnect.executeUpdate(
                    "UPDATE pets SET deleted_at = NOW() WHERE PetsID = ?",
                    Integer.parseInt(m.get("id")));
            String resp = "{\"success\":true}";
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    // ========== 訂單管理 Handlers ==========
    static class OrderListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String sql = "SELECT o.OrdersID, o.CreateTime, p.PetsName AS Pet, c.CustomersName AS Owner, "
                    + "sd1.Services AS Service1, sd2.Services AS Service2, sd3.Services AS Service3, "
                    + "e.EmployeeName AS CheckedBy, o.CheckTime "
                    + "FROM orders o "
                    + "JOIN pets p ON o.PetsID = p.PetsID "
                    + "JOIN customers c ON p.PetsOwnersID = c.CustomersID "
                    + "LEFT JOIN servicepricing sp1 ON o.Services1 = sp1.ServicePricingID "
                    + "LEFT JOIN servicesfordogs sd1 ON sp1.ServicesForDogsID = sd1.ServicesForDogsID "
                    + "LEFT JOIN servicesforcats sc1 ON sp1.ServicesForCatsID = sc1.ServicesForCatsID "
                    + "LEFT JOIN servicesfordogs sd2 ON sp1.ServicesForDogsID = sd2.ServicesForDogsID "
                    + "LEFT JOIN servicesforcats sc2 ON sp1.ServicesForCatsID = sc2.ServicesForCatsID "
                    + "LEFT JOIN servicesfordogs sd3 ON sp1.ServicesForDogsID = sd3.ServicesForDogsID "
                    + "LEFT JOIN servicesforcats sc3 ON sp1.ServicesForCatsID = sc3.ServicesForCatsID "
                    + "JOIN employees e ON o.CheckByWho = e.EmployeesID "
                    + "WHERE o.deleted_at IS NULL "
                    + "ORDER BY o.OrdersID ASC";
            String response;
            try (ResultSet rs = DBConnect.selectQuery(sql)) {
                StringBuilder json = new StringBuilder("[");
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{");
                    for (int i = 1; i <= cols; i++) {
                        String col = meta.getColumnLabel(i);
                        String val = rs.getString(i);
                        if (i > 1)
                            json.append(",");
                        json.append("\"").append(col).append("\":\"")
                                .append(val == null ? "" : escapeJson(val)).append("\"");
                    }
                    json.append("}");
                }
                json.append("]");
                rs.getStatement().getConnection().close();
                response = json.toString();
            } catch (Exception e) {
                response = "[{\"error\":\"" + escapeJson(e.getMessage()) + "\"}]";
            }
            byte[] b = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
    }

    static class OrderCreateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            // CORS + JSON headers …
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "POST,OPTIONS");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                ex.getResponseBody().close();
                return;
            }

            String responseJson;
            int statusCode;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8))) {

                Map<String, String> m = DBConnect.parseJson(reader.lines().collect(Collectors.joining()));
                int nextOrderId = DBConnect.getNextId("orders", "OrdersID");
                String custType = m.get("custType");
                int service1 = Integer.parseInt(m.get("service1"));
                Integer service2 = (m.get("service2") == null || m.get("service2").isEmpty())
                        ? null
                        : Integer.valueOf(m.get("service2"));
                Integer service3 = (m.get("service3") == null || m.get("service3").isEmpty())
                        ? null
                        : Integer.valueOf(m.get("service3"));
                int checkedBy = Integer.parseInt(m.get("checkedBy"));

                int petId;
                if ("new".equalsIgnoreCase(custType)) {
                    // --- 新客戶：新增 customers ---
                    int custId = DBConnect.getNextId("customers", "CustomersID");
                    DBConnect.executeUpdate(
                            "INSERT INTO customers (CustomersID, CustomersName, CustomersPhoneNumbers) VALUES (?,?,?)",
                            custId,
                            m.get("newOwnerName"),
                            m.get("newOwnerPhone"));

                    // --- 新客戶：新增 pets（含 Category1/2）---
                    petId = DBConnect.getNextId("pets", "PetsID");
                    int cat1 = Integer.parseInt(m.get("newPetCat1"));
                    int cat2 = Integer.parseInt(m.get("newPetCat2"));
                    DBConnect.executeUpdate(
                            "INSERT INTO pets "
                                    + "(PetsID, PetsName, PetsAge, PetsOwnersID, PetsCategory1, PetsCategory2, SpecialDiseases, Remark) "
                                    + "VALUES (?,?,?,?,?,?,?,?)",
                            petId,
                            m.get("newPetName"),
                            Integer.parseInt(m.get("newPetAge")),
                            custId,
                            cat1, // <- 新增的參數
                            cat2, // <- 新增的參數
                            "", // SpecialDiseases
                            "" // Remark
                    );
                } else {
                    // 已有客戶，直接取 petId
                    petId = Integer.parseInt(m.get("petId"));
                }

                // --- 插入 orders ---
                String sql = "INSERT INTO orders "
                        + "(OrdersID, CreateTime, PetsID, Services1, Services2, Services3, CheckTime, CheckByWho) "
                        + "VALUES (?, NOW(), ?, ?, ?, ?, NOW(), ?)";
                DBConnect.executeUpdate(sql,
                        nextOrderId,
                        petId,
                        service1,
                        service2,
                        service3,
                        checkedBy);

                statusCode = 200;
                responseJson = "{\"success\":true}";
            } catch (Exception e) {
                statusCode = 500;
                String msg = e.getMessage() == null ? "Unknown error" : e.getMessage().replace("\"", "\\\"");
                responseJson = String.format("{\"success\":false,\"message\":\"%s\"}", msg);
            }

            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class OrderDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "DELETE,OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            String body;
            try (InputStream is = ex.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining());
            }
            Map<String, String> m = DBConnect.parseJson(body);
            DBConnect.executeUpdate("UPDATE orders SET deleted_at=NOW() WHERE OrdersID=?",
                    Integer.parseInt(m.get("id")));
            String resp = "{\"success\":true}";
            byte[] rb = resp.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, rb.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(rb);
            }
        }
    }

    // ========== 報表查看 Handlers ==========
    static class DailyRevenueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            // CORS & JSON
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,OPTIONS");
                ex.sendResponseHeaders(204, -1);
                ex.getResponseBody().close();
                return;
            }

            String response;
            String sql = ""
                    + "SELECT DATE(o.CreateTime) AS day, "
                    + "       SUM(sp.ServicePrice) AS total_revenue, "
                    + "       COUNT(*) AS order_count "
                    + "FROM orders o "
                    + "JOIN servicepricing sp ON o.Services1 = sp.ServicePricingID "
                    + "WHERE o.deleted_at IS NULL "
                    + "GROUP BY DATE(o.CreateTime) "
                    + "ORDER BY DATE(o.CreateTime)";

            try (ResultSet rs = DBConnect.selectQuery(sql)) {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        sb.append(",");
                    first = false;
                    sb.append("{")
                            .append("\"day\":\"").append(rs.getString("day")).append("\",")
                            .append("\"total_revenue\":\"").append(rs.getString("total_revenue")).append("\",")
                            .append("\"order_count\":\"").append(rs.getString("order_count")).append("\"")
                            .append("}");
                }
                sb.append("]");
                rs.getStatement().getConnection().close();
                response = sb.toString();
            } catch (Exception e) {
                String msg = e.getMessage().replace("\"", "\\\"");
                response = "[{\"error\":\"" + msg + "\"}]";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // --- 狗的服務佔比 ---
    static class ServiceShareDogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,OPTIONS");
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String sql = "SELECT d.Services AS service_name, COUNT(*) AS count\n" +
                    "FROM (\n" +
                    "  SELECT Services1 AS spid FROM orders WHERE deleted_at IS NULL\n" +
                    "  UNION ALL\n" +
                    "  SELECT Services2 FROM orders WHERE deleted_at IS NULL AND Services2 IS NOT NULL\n" +
                    "  UNION ALL\n" +
                    "  SELECT Services3 FROM orders WHERE deleted_at IS NULL AND Services3 IS NOT NULL\n" +
                    ") t\n" +
                    "JOIN servicepricing sp ON t.spid = sp.ServicePricingID\n" +
                    "JOIN servicesfordogs d ON sp.ServicesForDogsID = d.ServicesForDogsID\n" +
                    "WHERE sp.ServicesForDogsID IS NOT NULL\n" +
                    "GROUP BY d.Services";

            String response;
            try (ResultSet rs = DBConnect.selectQuery(sql)) {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        sb.append(",");
                    first = false;
                    sb.append("{")
                            .append("\"service_name\":\"").append(escapeJson(rs.getString("service_name")))
                            .append("\",")
                            .append("\"count\":").append(rs.getInt("count"))
                            .append("}");
                }
                sb.append("]");
                rs.getStatement().getConnection().close();
                response = sb.toString();
            } catch (Exception e) {
                response = "[{\"error\":\"" + escapeJson(e.getMessage()) + "\"}]";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // --- 貓的服務佔比 ---
    static class ServiceShareCatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,OPTIONS");
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String sql = "SELECT c.Services AS service_name, COUNT(*) AS count\n" +
                    "FROM (\n" +
                    "  SELECT Services1 AS spid FROM orders WHERE deleted_at IS NULL\n" +
                    "  UNION ALL\n" +
                    "  SELECT Services2 FROM orders WHERE deleted_at IS NULL AND Services2 IS NOT NULL\n" +
                    "  UNION ALL\n" +
                    "  SELECT Services3 FROM orders WHERE deleted_at IS NULL AND Services3 IS NOT NULL\n" +
                    ") t\n" +
                    "JOIN servicepricing sp ON t.spid = sp.ServicePricingID\n" +
                    "JOIN servicesforcats c ON sp.ServicesForCatsID = c.ServicesForCatsID\n" +
                    "WHERE sp.ServicesForCatsID IS NOT NULL\n" +
                    "GROUP BY c.Services";

            String response;
            try (ResultSet rs = DBConnect.selectQuery(sql)) {
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first)
                        sb.append(",");
                    first = false;
                    sb.append("{")
                            .append("\"service_name\":\"").append(escapeJson(rs.getString("service_name")))
                            .append("\",")
                            .append("\"count\":").append(rs.getInt("count"))
                            .append("}");
                }
                sb.append("]");
                rs.getStatement().getConnection().close();
                response = sb.toString();
            } catch (Exception e) {
                response = "[{\"error\":\"" + escapeJson(e.getMessage()) + "\"}]";
            }

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
