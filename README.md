# StayFlow — Property Management Console

menu-driven Java application for practicing the fundamentals of property operations: room inventory, reservations, guest records, food orders, checkout billing, and local persistence

> **Project status:** Educational console application. It is not a production booking system and does not include authentication, payment processing, or a database server

## Highlights

- **Room inventory:** 60 rooms across four room categories
- **Reservations:** View availability, reserve rooms, and prevent double-booking
- **Guest records:** Store guest names and contact details for occupied rooms
- **Food service:** Add menu items to a room's folio.
- **Checkout:** Calculate a simple bill for room nights and food orders
- **Persistence:** Save and reload the local application state
- **Input handling:** Validate menu choices, room numbers, quantities, and empty-room actions
- **Readable structure:** A small Java codebase with clear naming and focused responsibilities

## Requirements

- Java 17 or newer
- Maven 3.8+ (recommended)

Check your setup:

```bash
java -version
mvn -version
```

## Run locally

```bash
git clone https://github.com/walidwh21/Property-Management
cd Property-Management
mvn clean package
mvn exec:java
```

Alternatively, compile and run without Maven:

```bash
javac -d out src/main/java/com/stayflow/Main.java
java -cp out com.stayflow.Main
```

## Main menu

```text
╔══════════════════════════════════════════════╗
║                  STAYFLOW                    ║
║          PROPERTY MANAGEMENT CONSOLE         ║
╚══════════════════════════════════════════════╝

  1. Room overview
  2. Check availability
  3. Create reservation
  4. Add food order
  5. Check out and print bill
  6. Save and exit
```

## Design overview

The program separates the command-line interface from the domain model using nested classes:

- `StayFlowApp` coordinates menus and user actions
- `Property` owns the room inventory and reservation operations
- `Room` represents room category, rate, and current occupancy
- `Guest` holds guest details
- `FoodOrder` represents a menu item and quantity.
- `Reservation` stores guest, check-in date, and food orders
- `Persistence` reads and writes the local application snapshot

See [`docs/design.md`](docs/design.md) for the class relationship diagram and design notes

