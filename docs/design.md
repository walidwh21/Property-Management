# Design notes

## Responsibilities

- **StayFlowApp** — owns the console interaction loop and validates user input.
- **Property** — creates the fixed room inventory and coordinates room lookup and reservation changes.
- **Room** — stores a room number, category, nightly rate, and optional active reservation.
- **Guest** — stores the name and contact value supplied for a reservation.
- **Reservation** — stores guest information, check-in date, and ordered food items.
- **FoodOrder** — calculates a line total from menu item and quantity.
- **Persistence** — serializes the current `Property` snapshot to a local file and restores it on startup.

## Class relationship diagram

```mermaid
classDiagram
    class StayFlowApp {
      -Scanner input
      -Property property
      +run()
      -showMenu()
      -createReservation()
      -addFoodOrder()
      -checkout()
    }
    class Property {
      -List~Room~ rooms
      +findRoom(int) Room
      +availableRooms(RoomCategory) List~Room~
      +reserve(int, Guest) boolean
      +release(int)
    }
    class Room {
      -int number
      -RoomCategory category
      -int nightlyRate
      -Reservation reservation
      +isAvailable() boolean
    }
    class Guest {
      -String name
      -String contact
    }
    class Reservation {
      -Guest guest
      -LocalDate checkIn
      -List~FoodOrder~ foodOrders
      +foodTotal() int
      +totalFor(int) int
    }
    class FoodOrder {
      -String itemName
      -int unitPrice
      -int quantity
      +lineTotal() int
    }
    class Persistence {
      +load(Path) Property
      +save(Path, Property)
    }
    StayFlowApp --> Property
    Property o-- Room
    Room --> RoomCategory
    Room --> Reservation
    Reservation --> Guest
    Reservation o-- FoodOrder
    StayFlowApp --> Persistence
```

## Important scope limits

This is a learning project. Java object serialization is used for simplicity, not as a recommended production storage format. Production systems should use a database, authentication and authorization, data validation appropriate to local law, backups, and secure handling of personal information.
