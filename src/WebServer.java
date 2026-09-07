import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebServer.java
 *
 * Turns the existing OOP model (Vehicle, Customer, RentalAgency, etc.)
 * into a real web application, using ONLY Java's built-in HttpServer —
 * no frameworks, no dependency downloads. Any machine with a JDK can
 * run this with zero setup.
 *
 * DESIGN CHOICE: This class is intentionally the ONLY new file. Every
 * class we built for the console app (Vehicle, Car, RentalAgency,
 * custom exceptions...) is reused completely unchanged. That's the
 * real payoff of OOP done well: the business logic doesn't care
 * whether it's being driven by a terminal (Main.java) or a browser
 * (this class) — it's the same RentalAgency object either way.
 */
public class WebServer {

    // Shared state across all HTTP requests. In a real production app
    // this would be a database; here it's the same in-memory objects
    // from our console version, just kept alive for the server's lifetime.
    private static final RentalAgency agency = new RentalAgency("Metro Wheels Rentals");
    private static final Map<String, Customer> customersByName = new LinkedHashMap<>();
    private static final Map<String, RentalTransaction> activeTransactions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        seedDemoData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/vehicles", WebServer::handleVehicles);
        server.createContext("/api/customers", WebServer::handleCustomers);
        server.createContext("/api/rent", WebServer::handleRent);
        server.createContext("/api/return", WebServer::handleReturn);
        server.createContext("/api/report", WebServer::handleReport);
        server.createContext("/", WebServer::handleStatic); // serves the frontend
        server.setExecutor(null);
        server.start();

        System.out.println("Metro Wheels Rentals web app running at:");
        System.out.println("  http://localhost:8080");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    /** Sets up the same starting fleet and customers as the console demo. */
    private static void seedDemoData() {
        agency.addVehicleToFleet(new Car("CAR-100", "Toyota", "Camry", 2023, 45.00, 5));
        agency.addVehicleToFleet(new Motorcycle("BIKE-200", "Harley-Davidson", "Iron 883", 2022, 35.00, true));
        agency.addVehicleToFleet(new Truck("TRK-300", "Ford", "F-150", 2021, 90.00, 2.5));

        Customer alice = new Customer("Alice Johnson", "D1234567", 30, "Standard", true);
        Customer bob = new Customer("Bob Young", "D7654321", 19, "Standard", false);
        Customer carlos = new Customer("Carlos Diaz", "D1122334", 40, "Standard", false);
        Customer dana = new Customer("Dana Reyes", "D9988776", 35, "Commercial", false);

        for (Customer c : new Customer[]{alice, bob, carlos, dana}) {
            agency.registerCustomer(c);
            customersByName.put(c.getName(), c);
        }
    }

    // ============================================================
    // API HANDLERS
    // ============================================================

    private static void handleVehicles(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        StringBuilder json = new StringBuilder("[");
        List<Vehicle> vehicles = agency.getFleet().getAllVehicles();
        for (int i = 0; i < vehicles.size(); i++) {
            json.append(vehicleToJson(vehicles.get(i)));
            if (i < vehicles.size() - 1) json.append(",");
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    private static void handleCustomers(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        StringBuilder json = new StringBuilder("[");
        List<Customer> customers = new ArrayList<>(customersByName.values());
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            json.append(String.format(
                "{\"name\":\"%s\",\"age\":%d,\"licenseClass\":\"%s\",\"helmetCert\":%b}",
                escapeJson(c.getName()), c.getAge(), c.getLicenseClass(), c.hasHelmetCertification()
            ));
            if (i < customers.size() - 1) json.append(",");
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    /**
     * Handles a rental request from the browser.
     * This is where POLYMORPHISM still does the real work: we call
     * agency.rentVehicle(), which internally calls vehicle.checkEligibility()
     * — the web layer has no idea (and doesn't need to know) whether
     * that vehicle is a Car, Motorcycle, or Truck.
     */
    private static void handleRent(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        String customerName = body.get("customerName");
        String plate = body.get("plate");
        int days;
        try {
            days = Integer.parseInt(body.getOrDefault("days", "1"));
            if (days < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, "{\"success\":false,\"message\":\"Enter a valid number of days (1 or more).\"}");
            return;
        }

        Customer customer = customersByName.get(customerName);
        if (customer == null) {
            sendJson(exchange, 400, "{\"success\":false,\"message\":\"Unknown customer.\"}");
            return;
        }

        try {
            RentalTransaction tx = agency.rentVehicle(customer, plate, days);
            activeTransactions.put(plate, tx);
            sendJson(exchange, 200, String.format(
                "{\"success\":true,\"message\":\"Rented to %s for %d day(s).\",\"totalCost\":%.2f}",
                escapeJson(customer.getName()), days, tx.getTotalCost()
            ));
        } catch (VehicleUnavailableException | EligibilityException e) {
            // Same custom exceptions from the console app, now surfaced
            // as a normal JSON error the frontend can display.
            sendJson(exchange, 200, String.format(
                "{\"success\":false,\"message\":\"%s\"}", escapeJson(e.getMessage())
            ));
        }
    }

    private static void handleReturn(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        String plate = body.get("plate");
        RentalTransaction tx = activeTransactions.get(plate);
        if (tx == null) {
            sendJson(exchange, 400, "{\"success\":false,\"message\":\"This vehicle has no active rental.\"}");
            return;
        }
        agency.returnVehicle(plate, tx);
        activeTransactions.remove(plate);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"Vehicle returned.\"}");
    }

    private static void handleReport(HttpExchange exchange) throws IOException {
        List<Vehicle> vehicles = agency.getFleet().getAllVehicles();
        int available = agency.getFleet().findAvailableVehicles().size();
        double totalRevenue = 0.0;
        for (Customer c : customersByName.values()) {
            totalRevenue += c.getRentalHistory().getTotalSpent();
        }
        String json = String.format(
            "{\"totalVehicles\":%d,\"availableVehicles\":%d,\"totalCustomers\":%d,\"totalRevenue\":%.2f}",
            vehicles.size(), available, customersByName.size(), totalRevenue
        );
        sendJson(exchange, 200, json);
    }

    private static String vehicleToJson(Vehicle v) {
        return String.format(
            "{\"plate\":\"%s\",\"type\":\"%s\",\"make\":\"%s\",\"model\":\"%s\",\"year\":%d,\"dailyRate\":%.2f,\"mileage\":%.1f,\"available\":%b}",
            escapeJson(v.getLicensePlate()), v.getVehicleType(), escapeJson(v.getMake()),
            escapeJson(v.getModel()), v.getYear(), v.getBaseDailyRate(), v.getMileage(), v.isAvailable()
        );
    }

    // ============================================================
    // STATIC FILE SERVING (the frontend lives in /public)
    // ============================================================

    private static void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        Path filePath = Paths.get("public" + path);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound);
            }
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        String contentType = path.endsWith(".css") ? "text/css"
                : path.endsWith(".js") ? "application/javascript"
                : "text/html";
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ============================================================
    // SMALL HELPERS (no external JSON library needed for this scale)
    // ============================================================

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Minimal parser for flat, single-level JSON objects like {"key":"value"}. */
    private static Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String, String> map = new HashMap<>();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
            for (String pair : body.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    map.put(stripQuotes(kv[0]), stripQuotes(kv[1]));
                }
            }
        }
        return map;
    }

    private static String stripQuotes(String s) {
        return s.trim().replaceAll("^\"|\"$", "");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
