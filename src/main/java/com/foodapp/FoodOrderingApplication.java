package com.foodapp;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import com.foodapp.model.*;
import com.foodapp.service.*;
import com.foodapp.util.SimpleJson;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors; 


public class FoodOrderingApplication {

    private static final int DEFAULT_PORT = 8080;
    private static final FoodService foodService = new FoodService();
    private static final OrderService orderService = new OrderService();
    private static String staticResourcePath = "src/main/resources/static";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        try {
            
            File testDir = new File(staticResourcePath);
            if (!testDir.exists()) {
                staticResourcePath = "static";
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(16));

            // REST API Routes
            server.createContext("/api/menu", new MenuHandler());
            server.createContext("/api/orders", new OrderHandler());
            server.createContext("/api/coupons/validate", new CouponHandler());
            server.createContext("/api/analytics", new AnalyticsHandler());

            // Static File Server
            server.createContext("/", new StaticFileHandler());

            server.start();
            System.out.println("==========================================================");
            System.out.println(" 🍕 BITEEXPRESS FOOD ORDERING SYSTEM SERVER STARTED 🚀");
            System.out.println("==========================================================");
            System.out.println(" Access URL: http://localhost:" + port);
            System.out.println(" Status API: http://localhost:" + port + "/api/menu");
            System.out.println(" Press Ctrl+C to stop the application.");
            System.out.println("==========================================================");

        } catch (IOException e) {
            System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- MENU HANDLER ---
    static class MenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                sendJsonResponse(exchange, 200, foodService.getAllFoodAsJson());
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = SimpleJson.parseSimpleObject(body);

                FoodItem item = new FoodItem();
                item.setId(data.get("id"));
                item.setName(data.get("name"));
                item.setCategory(data.get("category"));
                item.setPrice(parseOrDefault(data.get("price"), 9.99));
                item.setDescription(data.get("description"));
                item.setRating(parseOrDefault(data.get("rating"), 4.5));
                item.setPrepTime(data.get("prepTime"));
                item.setTags(data.get("tags"));
                item.setImageUrl(data.get("imageUrl"));
                item.setAvailable("true".equalsIgnoreCase(data.get("available")));

                foodService.addFood(item);
                sendJsonResponse(exchange, 201, item.toJson());
            } else if ("DELETE".equalsIgnoreCase(method)) {
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                boolean deleted = foodService.deleteFood(id);
                if (deleted) {
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Item not found\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- ORDER HANDLER ---
    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                // Check if specific order requested
                if (path.length() > "/api/orders/".length()) {
                    String id = path.substring(path.lastIndexOf('/') + 1);
                    Order order = orderService.getOrder(id);
                    if (order != null) {
                        sendJsonResponse(exchange, 200, order.toJson());
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Order not found\"}");
                    }
                } else {
                    sendJsonResponse(exchange, 200, orderService.getAllOrdersAsJson());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> data = SimpleJson.parseSimpleObject(body);

                Order newOrder = new Order();
                newOrder.setCustomerName(data.get("customerName"));
                newOrder.setPhone(data.get("phone"));
                newOrder.setDeliveryAddress(data.get("deliveryAddress"));
                newOrder.setPaymentMethod(data.get("paymentMethod"));
                newOrder.setPromoCode(data.get("promoCode"));

                // Parse items array
                String itemsRaw = data.get("items");
                if (itemsRaw != null) {
                    List<Map<String, String>> itemMaps = SimpleJson.parseArrayOfObjects(itemsRaw);
                    for (Map<String, String> im : itemMaps) {
                        OrderItem oi = new OrderItem();
                        oi.setFoodId(im.get("foodId"));
                        oi.setName(im.get("name"));
                        oi.setPrice(parseOrDefault(im.get("price"), 0.0));
                        oi.setQuantity((int) parseOrDefault(im.get("quantity"), 1.0));
                        oi.setNotes(im.get("notes"));
                        newOrder.getItems().add(oi);
                    }
                }

                Order created = orderService.createOrder(newOrder);
                sendJsonResponse(exchange, 201, created.toJson());
            } else if ("PATCH".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                String id = path.substring(path.lastIndexOf('/') + 1);
                String body = readRequestBody(exchange);
                Map<String, String> data = SimpleJson.parseSimpleObject(body);
                String statusStr = data.get("status");

                try {
                    OrderStatus status = OrderStatus.valueOf(statusStr);
                    boolean updated = orderService.updateStatus(id, status);
                    if (updated) {
                        sendJsonResponse(exchange, 200, orderService.getOrder(id).toJson());
                    } else {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Order not found\"}");
                    }
                } catch (IllegalArgumentException e) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Invalid status value\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- COUPON HANDLER ---
    static class CouponHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = SimpleJson.parseSimpleObject(body);
                String code = data.get("code");
                Coupon coupon = orderService.validateCoupon(code);

                if (coupon != null) {
                    sendJsonResponse(exchange, 200, coupon.toJson());
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Invalid or expired coupon code\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- ANALYTICS HANDLER ---
    static class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, orderService.getAnalyticsAsJson());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // --- STATIC FILE HANDLER ---
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.equals("/")) {
                uriPath = "/index.html";
            }

            Path filePath = Paths.get(staticResourcePath, uriPath);

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                // Try fallback to root working directory
                filePath = Paths.get(uriPath.substring(1));
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String mimeType = getMimeType(filePath.toString());
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFoundMsg = "<h1>404 File Not Found</h1><p>Resource " + uriPath + " does not exist.</p>";
                exchange.sendResponseHeaders(404, notFoundMsg.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFoundMsg.getBytes());
                }
            }
        }
    }

    // Helper Methods
    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static double parseOrDefault(String val, double def) {
        if (val == null) return def;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String getMimeType(String filename) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }
}
