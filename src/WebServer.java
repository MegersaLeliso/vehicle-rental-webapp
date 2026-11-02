import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/**
 * Lightweight HTTP server and JSON API for the vehicle rental application.
 */
public class WebServer {

    private static final int PORT = 8080;
    private static final int MAX_REQUEST_BYTES = 8192;

    private final RentalAgency agency;
    private final HttpServer server;

    public WebServer() throws IOException {
        this.agency = createAgency();
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handleRequest);
    }

    public static void main(String[] args) throws IOException {
        WebServer webServer = new WebServer();
        webServer.server.start();
        System.out.println("Vehicle Rental server running at http://localhost:" + PORT);
    }

    private static RentalAgency createAgency() {
        RentalAgency agency = new RentalAgency("Metro Wheels Rentals", 10, 10);
        agency.addVehicleToFleet(new Car("CAR-100", "Toyota", "Camry", 2023, 45.00, 5));
        agency.addVehicleToFleet(new Motorcycle("BIKE-200", "Harley-Davidson", "Iron 883", 2022, 35.00, true));
        agency.addVehicleToFleet(new Truck("TRK-300", "Ford", "F-150", 2021, 90.00, 2.5));

        agency.registerCustomer(new Customer("Alex Morgan", "LIC-100", 30, "Standard", true));
        agency.registerCustomer(new Customer("Jamie Lee", "LIC-200", 20, "Standard", false));
        agency.registerCustomer(new Customer("Taylor Smith", "LIC-300", 35, "Commercial", true));
        return agency;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("/".equals(path) && "GET".equals(method)) {
            sendHtml(exchange, Files.readString(Path.of("public", "index.html"), StandardCharsets.UTF_8));
            return;
        }

        try {
            switch (path) {
                case "/api/vehicles":
                    requireMethod(method, "GET");
                    sendJson(exchange, 200, vehiclesJson());
                    break;
                case "/api/customers":
                    requireMethod(method, "GET");
                    sendJson(exchange, 200, customersJson());
                    break;
                case "/api/rent":
                    requireMethod(method, "POST");
                    handleRent(exchange);
                    break;
                case "/api/return":
                    requireMethod(method, "POST");
                    handleReturn(exchange);
                    break;
                case "/api/report":
                    requireMethod(method, "GET");
                    sendJson(exchange, 200, reportJson());
                    break;
                default:
                    sendJson(exchange, 404, "{\"success\":false,\"message\":\"Endpoint not found.\"}");
            }
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, resultJson(false, exception.getMessage(), null));
        } catch (MethodNotAllowedException exception) {
            exchange.getResponseHeaders().set("Allow", exception.allowedMethod);
            sendJson(exchange, 405, resultJson(false, exception.getMessage(), null));
        } catch (IllegalStateException exception) {
            sendJson(exchange, 409, resultJson(false, exception.getMessage(), null));
        }
    }

    private void handleRent(HttpExchange exchange) throws IOException {
        RentalRequest request = parseRentalRequest(readRequestBody(exchange));
        synchronized (agency) {
            Customer customer = agency.findCustomerByName(request.customerName);
            if (customer == null) {
                sendJson(exchange, 400, resultJson(false, "Customer is not registered.", null));
                return;
            }

            try {
                RentalTransaction transaction = agency.rentVehicle(customer, request.plate, request.days);
                sendJson(exchange, 200, resultJson(true, "Rental confirmed.", transaction.getTotalCost()));
            } catch (VehicleUnavailableException | EligibilityException exception) {
                sendJson(exchange, 400, resultJson(false, exception.getMessage(), null));
            }
        }
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
        RentalRequest request = parseReturnRequest(readRequestBody(exchange));
        synchronized (agency) {
            boolean returned = agency.returnVehicle(request.plate);
            String message = returned
                    ? "Vehicle " + request.plate + " has been returned."
                    : "No active rental found for plate " + request.plate + ".";
            sendJson(exchange, returned ? 200 : 400, resultJson(returned, message, null));
        }
    }

    private String vehiclesJson() {
        synchronized (agency) {
            Vehicle[] vehicles = agency.getFleet().getAllVehicles();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < vehicles.length; i++) {
                Vehicle vehicle = vehicles[i];
                if (i > 0) {
                    json.append(',');
                }
                json.append('{')
                        .append("\"plate\":\"").append(jsonEscape(vehicle.getLicensePlate())).append("\",")
                        .append("\"type\":\"").append(jsonEscape(vehicle.getVehicleType())).append("\",")
                        .append("\"make\":\"").append(jsonEscape(vehicle.getMake())).append("\",")
                        .append("\"model\":\"").append(jsonEscape(vehicle.getModel())).append("\",")
                        .append("\"year\":").append(vehicle.getYear()).append(',')
                        .append("\"dailyRate\":").append(numberJson(vehicle.getBaseDailyRate())).append(',')
                        .append("\"available\":").append(vehicle.isAvailable()).append(',')
                        .append("\"info\":\"").append(jsonEscape(vehicle.getInfo())).append("\"}");
            }
            return json.append(']').toString();
        }
    }

    private String customersJson() {
        synchronized (agency) {
            Customer[] customers = agency.getCustomers();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < customers.length; i++) {
                Customer customer = customers[i];
                if (i > 0) {
                    json.append(',');
                }
                json.append('{')
                        .append("\"name\":\"").append(jsonEscape(customer.getName())).append("\",")
                        .append("\"licenseNumber\":\"").append(jsonEscape(customer.getLicenseNumber())).append("\",")
                        .append("\"age\":").append(customer.getAge()).append(',')
                        .append("\"licenseClass\":\"").append(jsonEscape(customer.getLicenseClass())).append("\",")
                        .append("\"helmetCertification\":").append(customer.hasHelmetCertification()).append('}');
            }
            return json.append(']').toString();
        }
    }

    private String reportJson() {
        synchronized (agency) {
            Vehicle[] vehicles = agency.getFleet().getAllVehicles();
            int availableCount = (int) Arrays.stream(vehicles)
                    .filter(Vehicle::isAvailable)
                    .count();
            return "{"
                    + "\"agencyName\":\"" + jsonEscape(agency.getAgencyName()) + "\","
                    + "\"totalFleetCount\":" + vehicles.length + ","
                    + "\"availableCount\":" + availableCount + ","
                    + "\"totalRevenue\":" + numberJson(agency.getTotalRevenue()) + ","
                    + "\"customerCount\":" + agency.getCustomers().length
                    + "}";
        }
    }

    private static String resultJson(boolean success, String message, Double totalCost) {
        StringBuilder json = new StringBuilder("{")
                .append("\"success\":").append(success).append(',')
                .append("\"message\":\"").append(jsonEscape(message)).append('"');
        if (totalCost != null) {
            json.append(",\"totalCost\":").append(numberJson(totalCost));
        }
        return json.append('}').toString();
    }

    private static RentalRequest parseRentalRequest(String body) {
        JsonObjectParser parser = new JsonObjectParser(body);
        String customerName = parser.stringField("customerName");
        String plate = parser.stringField("plate");
        int days = parser.integerField("days");
        parser.finish();
        if (customerName.isBlank() || plate.isBlank()) {
            throw new IllegalArgumentException("customerName and plate are required.");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be greater than zero.");
        }
        return new RentalRequest(customerName, plate, days);
    }

    private static RentalRequest parseReturnRequest(String body) {
        JsonObjectParser parser = new JsonObjectParser(body);
        String plate = parser.stringField("plate");
        parser.finish();
        if (plate.isBlank()) {
            throw new IllegalArgumentException("plate is required.");
        }
        return new RentalRequest(null, plate, 0);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        byte[] body;
        try (java.io.InputStream requestBody = exchange.getRequestBody()) {
            body = requestBody.readNBytes(MAX_REQUEST_BYTES + 1);
        }
        if (body.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("Request body exceeds the allowed size.");
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static void requireMethod(String actualMethod, String expectedMethod) {
        if (!expectedMethod.equals(actualMethod)) {
            throw new MethodNotAllowedException(expectedMethod);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        sendResponse(exchange, status, body);
    }

    private static void sendHtml(HttpExchange exchange, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        sendResponse(exchange, 200, body);
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().close();
    }

    private static String numberJson(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

    private static final class RentalRequest {
        private final String customerName;
        private final String plate;
        private final int days;

        private RentalRequest(String customerName, String plate, int days) {
            this.customerName = customerName;
            this.plate = plate;
            this.days = days;
        }
    }

    private static final class MethodNotAllowedException extends RuntimeException {
        private final String allowedMethod;

        private MethodNotAllowedException(String allowedMethod) {
            super("Method not allowed.");
            this.allowedMethod = allowedMethod;
        }
    }

    private static final class JsonObjectParser {
        private final String input;
        private int position;
        private String customerName;
        private String plate;
        private Integer days;

        private JsonObjectParser(String input) {
            this.input = input;
            parseObject();
        }

        private String stringField(String field) {
            String value;
            if ("customerName".equals(field)) {
                value = customerName;
            } else if ("plate".equals(field)) {
                value = plate;
            } else {
                throw invalidJson();
            }
            if (value == null) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
            return value;
        }

        private int integerField(String field) {
            if (!"days".equals(field) || days == null) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
            return days;
        }

        private void finish() {
            skipWhitespace();
            if (position != input.length()) {
                throw invalidJson();
            }
        }

        private void parseObject() {
            skipWhitespace();
            expect('{');
            boolean firstField = true;
            while (true) {
                skipWhitespace();
                if (take('}')) {
                    break;
                }
                if (!firstField) {
                    expect(',');
                    skipWhitespace();
                    if (position < input.length() && input.charAt(position) == '}') {
                        throw invalidJson();
                    }
                }
                firstField = false;

                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                switch (key) {
                    case "customerName":
                        if (customerName != null) {
                            throw invalidJson();
                        }
                        customerName = parseString();
                        break;
                    case "plate":
                        if (plate != null) {
                            throw invalidJson();
                        }
                        plate = parseString();
                        break;
                    case "days":
                        if (days != null) {
                            throw invalidJson();
                        }
                        days = parseInteger();
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected request field: " + key);
                }
            }
        }

        private int parseInteger() {
            int start = position;
            if (position < input.length() && input.charAt(position) == '-') {
                position++;
            }
            int digitStart = position;
            while (position < input.length()
                    && input.charAt(position) >= '0'
                    && input.charAt(position) <= '9') {
                position++;
            }
            if (digitStart == position) {
                throw invalidJson();
            }
            if (input.charAt(digitStart) == '0' && position - digitStart > 1) {
                throw invalidJson();
            }
            try {
                return Integer.parseInt(input.substring(start, position));
            } catch (NumberFormatException exception) {
                throw invalidJson();
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }
                if (character < 0x20) {
                    throw invalidJson();
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position >= input.length()) {
                    throw invalidJson();
                }
                char escape = input.charAt(position++);
                switch (escape) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(escape);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        if (position + 4 > input.length()) {
                            throw invalidJson();
                        }
                        try {
                            value.append((char) Integer.parseInt(input.substring(position, position + 4), 16));
                        } catch (NumberFormatException exception) {
                            throw invalidJson();
                        }
                        position += 4;
                        break;
                    default:
                        throw invalidJson();
                }
            }
            throw invalidJson();
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private boolean take(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) {
                throw invalidJson();
            }
        }

        private IllegalArgumentException invalidJson() {
            return new IllegalArgumentException("Request body must be a valid JSON object with the required fields.");
        }
    }
}
