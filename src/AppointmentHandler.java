import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AppointmentHandler implements HttpHandler {
    private static final String FILE_PATH = "appointments.json";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> input = parseJson(body);

        String name = input.get("name");
        String gender = input.get("gender");
        String birth = input.get("birth");
        String appointment = input.get("appointment");

        if (name == null || birth == null || appointment == null) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String json = String.format("{\"name\":\"%s\",\"gender\":\"%s\",\"birth\":\"%s\",\"appointment\":\"%s\"}",
                escape(name), escape(gender), escape(birth), escape(appointment));

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(json + "\n"); // 每筆資料一行
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, -1);
    }

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