import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatHandler {
    private static final String FILE_PATH = "chatlog.json";

    public static class SendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                        "Content-Type, ngrok-skip-browser-warning");
                exchange.sendResponseHeaders(204, -1); // ✅ 回傳 No Content（成功）
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> input = parseJson(body);
            String name = input.get("name");
            String message = input.get("message");

            if (name == null || message == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            List<String> messages = readMessages();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String escapedMessage = escape(message);
            String escapedName = escape(name);
            String json = String.format("{\"name\":\"%s\",\"message\":\"%s\",\"time\":\"%s\"}", escapedName,
                    escapedMessage, timestamp);

            messages.add(json);

            while (messages.size() > 200)
                messages.remove(0);

            writeMessages(messages);

            exchange.sendResponseHeaders(200, -1);
        }
    }

    public static class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> messages = readMessages();
            String response = "[" + String.join(",", messages) + "]";
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                    "Content-Type, ngrok-skip-browser-warning");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }

    // ✅ 讀檔並轉成 List<String>（每筆為 JSON 字串）
    private static List<String> readMessages() {
        File file = new File(FILE_PATH);
        if (!file.exists())
            return new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null || line.length() < 2)
                return new ArrayList<>();
            line = line.substring(1, line.length() - 1); // 移除 []
            String[] parts = line.split("(?<=\\}),\\s*(?=\\{)");
            return new ArrayList<>(Arrays.asList(parts));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    // ✅ 寫入 List<String> 作為 JSON 陣列
    private static void writeMessages(List<String> messages) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            bw.write("[" + String.join(",", messages) + "]");
        }
    }

    // ✅ 超簡易 JSON parser（只支援平面 key-value）
    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2)
                map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}