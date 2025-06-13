/*
 * import com.sun.net.httpserver.*;
 * import java.io.IOException;
 * import java.io.OutputStream;
 * import java.net.InetSocketAddress;
 * import java.sql.ResultSet;
 * 
 * public class Server {
 * public static void main(String[] args) throws IOException {
 * // 建立一個綁定在 localhost:8000 的 server
 * HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
 * 
 * // 綁定 API 路由
 * server.createContext("/api/hello", new HelloHandler());
 * server.createContext("/api/customers", new CustHandler());
 * 
 * // 啟動 server
 * server.start();
 * System.out.println("Server started at http://localhost:8000");
 * }
 * 
 * // 定義第二個handler
 * static class CustHandler implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * String response = "Welcome";
 * StringBuilder json = new StringBuilder();
 * json.append("[");
 * try {
 * ResultSet rs = DBConnect.selectQuery("SELECT * FROM customers");
 * boolean first = true;
 * while (rs.next()) {
 * if (!first)
 * json.append(",");
 * first = false;
 * String id = rs.getString("CustomersID");
 * String name = rs.getString("CustomersName");
 * json.append("{")
 * .append("\"CustomersID\":\"").append(id).append("\",")
 * .append("\"CustomersName\":\"").append(name).append("\"")
 * .append("}");
 * }
 * rs.getStatement().getConnection().close(); // 手動關閉連線
 * } catch (Exception e) {
 * json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
 * }
 * json.append("]");
 * response = json.toString();
 * exchange.getResponseHeaders().set("Content-Type",
 * "text/plain; charset=utf-8");
 * exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 * byte[] responseBytes = response.toString().getBytes("UTF-8");
 * exchange.sendResponseHeaders(200, responseBytes.length);
 * OutputStream os = exchange.getResponseBody();
 * os.write(responseBytes);
 * os.close();
 * }
 * }
 * 
 * // 定義第一個handler
 * static class HelloHandler implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * String response = "Welcome";
 * StringBuilder json = new StringBuilder();
 * json.append("[");
 * try {
 * ResultSet rs = DBConnect.selectQuery("SELECT * FROM view0002");
 * boolean first = true;
 * while (rs.next()) {
 * if (!first)
 * json.append(",");
 * first = false;
 * String id = rs.getString("寵物ID");
 * String name = rs.getString("寵物名稱");
 * json.append("{")
 * .append("\"寵物ID\":\"").append(id).append("\",")
 * .append("\"寵物名稱\":\"").append(name).append("\"")
 * .append("}");
 * }
 * rs.getStatement().getConnection().close(); // 手動關閉連線
 * } catch (Exception e) {
 * json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
 * }
 * json.append("]");
 * 
 * response = json.toString();
 * exchange.getResponseHeaders().set("Content-Type",
 * "text/plain; charset=utf-8");
 * exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 * byte[] responseBytes = response.toString().getBytes("UTF-8");
 * exchange.sendResponseHeaders(200, responseBytes.length);
 * OutputStream os = exchange.getResponseBody();
 * os.write(responseBytes);
 * os.close();
 * }
 * }
 * 
 * }
 * import java.sql.*;
 * import java.util.HashMap;
 * import java.util.Map;
 * import java.util.stream.Collectors;
 * 
 * import java.time.LocalDateTime;
 * import java.time.format.DateTimeFormatter;
 * 
 * import com.sun.net.httpserver.*;
 * import java.beans.Customizer;
 * import java.io.BufferedReader;
 * import java.io.BufferedWriter;
 * import java.io.FileWriter;
 * import java.io.IOException;
 * import java.io.InputStreamReader;
 * import java.io.OutputStream;
 * import java.net.InetSocketAddress;
 * import java.nio.charset.StandardCharsets;
 * 
 * public class Server2 {
 * public static void main(String[] args) throws IOException {
 * // 建立一個綁定在 localhost:8000 的 server
 * HttpServer server2 = HttpServer.create(new InetSocketAddress(8000), 0);
 * 
 * // 綁定 API 路由
 * server2.createContext("/api/customers", new
 * TableDataHandler("select * from customers"));
 * server2.createContext("/api/pets", new
 * TableDataHandler("select * from pets"));
 * server2.createContext("/api/orders", new
 * TableDataHandler("select * from orders"));
 * 
 * server2.createContext("/chat/send", new ChatHandler.SendHandler());
 * server2.createContext("/chat/messages", new ChatHandler.MessageHandler());
 * 
 * server2.createContext("/getdemo", new getdemo());
 * server2.createContext("/postdemo", new postdemo());
 * server2.createContext("/insertdata", new InsertDataHandler());
 * 
 * server2.createContext("/appointment", new AppointmentHandler());
 * 
 * server2.createContext("/login", new LoginHandler());
 * 
 * // 啟動 server
 * server2.start();
 * System.out.println("Server started at http://localhost:8000");
 * }
 * 
 * // 1140519 add InsertDataHandler
 * static class InsertDataHandler implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
 * exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
 * exchange.getResponseHeaders().add("Access-Control-Allow-Methods",
 * "POST, GET, OPTIONS");
 * exchange.getResponseHeaders().add("Access-Control-Allow-Headers",
 * "Content-Type");
 * exchange.sendResponseHeaders(204, -1);
 * return;
 * }
 * exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
 * try {
 * String body;
 * try (BufferedReader reader = new BufferedReader(
 * new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
 * body = reader.lines().collect(Collectors.joining());
 * }
 * Map<String, String> map = DBConnect.parseJson(body);
 * String targetTable = map.get("table");
 * switch (targetTable) {
 * case "customers":
 * String name = map.get("name");
 * String phone = map.get("phone");
 * int id = DBConnect.getNextId("customers", "CustomersID");
 * DBConnect.executeUpdate(
 * "INSERT INTO customers (CustomersID, CustomersName, CustomersPhoneNumbers) VALUES (?, ?, ?)"
 * ,
 * id, name, phone);
 * exchange.sendResponseHeaders(200, -1);
 * break;
 * default:
 * exchange.sendResponseHeaders(400, -1);
 * break;
 * }
 * } catch (Exception e) {
 * e.printStackTrace();
 * String response = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") +
 * "\"}";
 * exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 * exchange.getResponseHeaders().set("Content-Type", "application/json");
 * exchange.sendResponseHeaders(500, response.getBytes().length);
 * try (OutputStream os = exchange.getResponseBody()) {
 * os.write(response.getBytes());
 * }
 * }
 * }
 * }
 * 
 * // 1140517 add 櫃台人員
 * static class getdemo implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * String response = "Hello Welcome!";
 * // 指定回傳格式:text, 開放完全存取 *
 * exchange.getResponseHeaders().set("Content-Type",
 * "text/plain; charset=utf-8");
 * exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 * // 格式化回傳的內容 文字->bytes
 * byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
 * // 啟動回傳，通知成功與長度，再回傳內容
 * exchange.sendResponseHeaders(200, responseBytes.length);
 * OutputStream os = exchange.getResponseBody();
 * os.write(responseBytes);
 * os.close();
 * }
 * }
 * 
 * // 定義postdemo handler
 * static class postdemo implements HttpHandler {
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * String file = "temppost.txt";
 * String body = new String(exchange.getRequestBody().readAllBytes(),
 * StandardCharsets.UTF_8);
 * try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
 * bw.write(body);
 * }
 * exchange.sendResponseHeaders(200, -1);
 * }
 * }
 * 
 * // 定義handler
 * static class TableDataHandler implements HttpHandler {
 * String sql = "";
 * 
 * public TableDataHandler(String sql) {
 * this.sql = sql;
 * }
 * 
 * @Override
 * public void handle(HttpExchange exchange) throws IOException {
 * String response = "";
 * StringBuilder json = new StringBuilder();
 * json.append("[");
 * try {
 * ResultSet rs = DBConnect.selectQuery(sql);
 * ResultSetMetaData meta = rs.getMetaData();
 * int columnCount = meta.getColumnCount();
 * 
 * boolean firstRow = true;
 * while (rs.next()) {
 * if (!firstRow)
 * json.append(",");
 * firstRow = false;
 * 
 * json.append("{");
 * for (int i = 1; i <= columnCount; i++) {
 * String columnName = meta.getColumnLabel(i);
 * String value = rs.getString(i);
 * if (i > 1)
 * json.append(",");
 * json.append("\"").append(columnName).append("\":");
 * json.append("\"").append(value == null ? "" :
 * escapeJson(value)).append("\"");
 * }
 * json.append("}");
 * }
 * rs.getStatement().getConnection().close(); // 關閉連線
 * } catch (Exception e) {
 * json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
 * }
 * json.append("]");
 * 
 * response = json.toString();
 * exchange.getResponseHeaders().set("Content-Type",
 * "text/plain; charset=utf-8");
 * exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
 * byte[] responseBytes = response.toString().getBytes("UTF-8");
 * exchange.sendResponseHeaders(200, responseBytes.length);
 * OutputStream os = exchange.getResponseBody();
 * os.write(responseBytes);
 * os.close();
 * }
 * 
 * private static String escapeJson(String s) {
 * return s.replace("\\", "\\\\")
 * .replace("\"", "\\\"")
 * .replace("\n", "\\n")
 * .replace("\r", "\\r");
 * }
 * }
 * }
 */