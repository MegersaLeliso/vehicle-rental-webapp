# Vehicle Rental Web Application

A lightweight vehicle rental web app with a Java HTTP server and a responsive
HTML/JavaScript dashboard. The backend uses Java's built-in `HttpServer`; the
domain model keeps fleet, customer, and rental-history data in fixed-capacity
arrays.

## Run

From the repository root with JDK 11 or later:

```sh
javac src/*.java
java -cp src WebServer
```

Open [http://localhost:8080](http://localhost:8080) in a browser. The server
seeds sample vehicles and customers and exposes JSON endpoints for fleet,
customer, rental, return, and report data.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/vehicles` | List fleet vehicles and availability |
| `GET` | `/api/customers` | List registered customers |
| `POST` | `/api/rent` | Rent using `customerName`, `plate`, and `days` |
| `POST` | `/api/return` | Return a vehicle using `plate` |
| `GET` | `/api/report` | Read fleet counts and total revenue |

The dashboard calls these endpoints with asynchronous `fetch()` requests.
