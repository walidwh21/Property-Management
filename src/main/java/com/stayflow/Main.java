package com.stayflow;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        new StayFlowApp().run();
    }
}

enum RoomCategory {
    PREMIUM_DOUBLE("Premium Double", 1, 10, 2, true, 4000),
    STANDARD_DOUBLE("Standard Double", 11, 30, 2, false, 3000),
    PREMIUM_SINGLE("Premium Single", 31, 40, 1, true, 2200),
    STANDARD_SINGLE("Standard Single", 41, 60, 1, false, 1200);

    final String label;
    final int firstRoom;
    final int lastRoom;
    final int capacity;
    final boolean airConditioning;
    final int nightlyRate;

    RoomCategory(String label, int firstRoom, int lastRoom, int capacity,
                 boolean airConditioning, int nightlyRate) {
        this.label = label;
        this.firstRoom = firstRoom;
        this.lastRoom = lastRoom;
        this.capacity = capacity;
        this.airConditioning = airConditioning;
        this.nightlyRate = nightlyRate;
    }

    static RoomCategory fromChoice(int choice) {
        RoomCategory[] categories = values();
        if (choice < 1 || choice > categories.length) return null;
        return categories[choice - 1];
    }

    static RoomCategory forRoomNumber(int number) {
        for (RoomCategory category : values()) {
            if (number >= category.firstRoom && number <= category.lastRoom) return category;
        }
        return null;
    }
}

final class Guest implements Serializable {
    private static final long serialVersionUID = 1L;
    final String name;
    final String contact;

    Guest(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }
}

final class FoodOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    final String itemName;
    final int unitPrice;
    final int quantity;

    FoodOrder(String itemName, int unitPrice, int quantity) {
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    int lineTotal() {
        return unitPrice * quantity;
    }
}

final class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    final Guest guest;
    final LocalDate checkIn;
    final List<FoodOrder> foodOrders = new ArrayList<>();

    Reservation(Guest guest) {
        this.guest = guest;
        this.checkIn = LocalDate.now();
    }

    int foodTotal() {
        return foodOrders.stream().mapToInt(FoodOrder::lineTotal).sum();
    }
}

final class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    final int number;
    final RoomCategory category;
    Reservation reservation;

    Room(int number, RoomCategory category) {
        this.number = number;
        this.category = category;
    }

    boolean isAvailable() {
        return reservation == null;
    }
}

final class Property implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Room> rooms = new ArrayList<>();

    Property() {
        for (RoomCategory category : RoomCategory.values()) {
            for (int number = category.firstRoom; number <= category.lastRoom; number++) {
                rooms.add(new Room(number, category));
            }
        }
    }

    Room findRoom(int number) {
        if (number < 1 || number > 60) return null;
        return rooms.get(number - 1);
    }

    List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    List<Room> availableRooms(RoomCategory category) {
        List<Room> available = new ArrayList<>();
        for (Room room : rooms) {
            if (room.category == category && room.isAvailable()) available.add(room);
        }
        return available;
    }

    boolean reserve(int roomNumber, Guest guest) {
        Room room = findRoom(roomNumber);
        if (room == null || !room.isAvailable()) return false;
        room.reservation = new Reservation(guest);
        return true;
    }

    Reservation release(int roomNumber) {
        Room room = findRoom(roomNumber);
        if (room == null || room.isAvailable()) return null;
        Reservation completed = room.reservation;
        room.reservation = null;
        return completed;
    }
}

final class Persistence {
    private Persistence() {}
    static Property load(Path path) {
        if (!Files.exists(path)) return new Property();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object value = input.readObject();
            if (value instanceof Property property) return property;
        } catch (IOException | ClassNotFoundException | RuntimeException ex) {
            System.out.println("Saved data could not be loaded. Starting with an empty inventory.");
        }
        return new Property();
    }

    static boolean save(Path path, Property property) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
            output.writeObject(property);
        } catch (IOException ex) {
            System.out.println("Could not save local data: " + ex.getMessage());
            return false;
        }
        try {
            Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            try {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException second) {
                System.out.println("Could not finalize save: " + second.getMessage());
                return false;
            }
        }
        return true;
    }
}

final class StayFlowApp {
    private static final Path DATA_FILE = Path.of("stayflow-data.bin");
    private static final Map<Integer, MenuItem> MENU = Map.of(
            1, new MenuItem("Garden Sandwich", 50),
            2, new MenuItem("Creamy Pasta", 60),
            3, new MenuItem("Stir-fried Noodles", 70),
            4, new MenuItem("Sparkling Drink", 30)
    );

    private final Scanner input = new Scanner(System.in);
    private Property property = Persistence.load(DATA_FILE);

    void run() {
        boolean running = true;
        while (running) {
            printHeader();
            System.out.println("  1. Room overview");
            System.out.println("  2. Check availability");
            System.out.println("  3. Create reservation");
            System.out.println("  4. Add food order");
            System.out.println("  5. Check out and print bill");
            System.out.println("  6. Save and exit");
            int choice = readInt("Choose an option: ", 1, 6);
            System.out.println();
            switch (choice) {
                case 1 -> showOverview();
                case 2 -> showAvailability();
                case 3 -> createReservation();
                case 4 -> addFoodOrder();
                case 5 -> checkout();
                case 6 -> {
                    if (Persistence.save(DATA_FILE, property)) {
                        System.out.println("Saved. Thanks for using StayFlow.");
                    }
                    running = false;
                }
            }
            if (running) pause();
        }
        input.close();
    }

    private void printHeader() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║                      STAYFLOW                      ║");
        System.out.println("║             PROPERTY MANAGEMENT CONSOLE            ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
    }

    private void showOverview() {
        System.out.println("ROOM OVERVIEW");
        System.out.println("─".repeat(76));
        System.out.printf("%-20s %-13s %-13s %-13s %-10s%n",
                "Category", "Room range", "Total", "Available", "Rate/night");
        for (RoomCategory category : RoomCategory.values()) {
            int total = category.lastRoom - category.firstRoom + 1;
            int available = property.availableRooms(category).size();
            System.out.printf("%-20s %2d–%-10d %-13d %-13d %,10d%n",
                    category.label, category.firstRoom, category.lastRoom,
                    total, available, category.nightlyRate);
        }
    }

    private RoomCategory chooseCategory() {
        System.out.println("Room categories:");
        RoomCategory[] categories = RoomCategory.values();
        for (int i = 0; i < categories.length; i++) {
            RoomCategory c = categories[i];
            System.out.printf("  %d. %-20s | %d guest(s) | AC: %-3s | Rate: %,d/night%n",
                    i + 1, c.label, c.capacity, c.airConditioning ? "Yes" : "No", c.nightlyRate);
        }
        return RoomCategory.fromChoice(readInt("Choose category: ", 1, categories.length));
    }

    private void showAvailability() {
        RoomCategory category = chooseCategory();
        List<Room> available = property.availableRooms(category);
        System.out.println();
        if (available.isEmpty()) {
            System.out.println("No rooms are currently available in this category.");
        } else {
            System.out.println("Available rooms: " + available.stream()
                    .map(room -> Integer.toString(room.number))
                    .reduce((a, b) -> a + ", " + b).orElse("None"));
            System.out.println("Available: " + available.size());
        }
    }

    private void createReservation() {
        RoomCategory category = chooseCategory();
        List<Room> available = property.availableRooms(category);
        if (available.isEmpty()) {
            System.out.println("This category is fully booked.");
            return;
        }
        System.out.println("Available room numbers: " + available.stream()
                .map(room -> Integer.toString(room.number))
                .reduce((a, b) -> a + ", " + b).orElse(""));
        int roomNumber = readInt("Room number: ", category.firstRoom, category.lastRoom);
        if (property.findRoom(roomNumber).category != category) {
            System.out.println("That room does not belong to the selected category.");
            return;
        }
        if (!property.findRoom(roomNumber).isAvailable()) {
            System.out.println("That room is already occupied. Choose an available room.");
            return;
        }
        String name = readText("Guest name: ");
        String contact = readText("Contact (or leave blank): ", true);
        if (property.reserve(roomNumber, new Guest(name, contact))) {
            System.out.printf("Reservation created for room %d (%s).%n", roomNumber, category.label);
        } else {
            System.out.println("Could not create the reservation.");
        }
    }

    private void addFoodOrder() {
        int roomNumber = readInt("Room number (1–60): ", 1, 60);
        Room room = property.findRoom(roomNumber);
        if (room.isAvailable()) {
            System.out.println("This room has no active reservation.");
            return;
        }
        System.out.println("FOOD MENU");
        MENU.forEach((number, item) ->
                System.out.printf("  %d. %-24s %,d%n", number, item.name, item.price));
        int choice = readInt("Choose an item: ", 1, MENU.size());
        int quantity = readInt("Quantity: ", 1, 100);
        MenuItem item = MENU.get(choice);
        room.reservation.foodOrders.add(new FoodOrder(item.name, item.price, quantity));
        System.out.printf("Added %d × %s. Line total: %,d%n",
                quantity, item.name, item.price * quantity);
    }

    private void checkout() {
        int roomNumber = readInt("Room number (1–60): ", 1, 60);
        Room room = property.findRoom(roomNumber);
        if (room.isAvailable()) {
            System.out.println("This room has no active reservation.");
            return;
        }
        Reservation reservation = room.reservation;
        long nights = Math.max(1, ChronoUnit.DAYS.between(reservation.checkIn, LocalDate.now()));
        long roomTotal = nights * room.category.nightlyRate;
        System.out.println();
        System.out.println("CHECKOUT BILL");
        System.out.println("─".repeat(48));
        System.out.println("Room: " + room.number + " — " + room.category.label);
        System.out.println("Guest: " + reservation.guest.name);
        System.out.println("Check-in: " + reservation.checkIn);
        System.out.println("Nights charged: " + nights);
        System.out.printf("Room charges: %,d%n", roomTotal);
        if (reservation.foodOrders.isEmpty()) {
            System.out.println("Food orders: None");
        } else {
            System.out.println("Food orders:");
            for (FoodOrder order : reservation.foodOrders) {
                System.out.printf("  %-24s %2d × %,d = %,d%n",
                        order.itemName, order.quantity, order.unitPrice, order.lineTotal());
            }
        }
        int foodTotal = reservation.foodTotal();
        System.out.printf("Food total: %,d%n", foodTotal);
        System.out.println("─".repeat(48));
        System.out.printf("TOTAL DUE: %,d%n", roomTotal + foodTotal);
        if (readYesNo("Complete checkout and release room? (y/n): ")) {
            property.release(roomNumber);
            System.out.println("Checkout complete. Room " + roomNumber + " is available again.");
        } else {
            System.out.println("Checkout cancelled; reservation remains active.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String raw = input.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // Re-prompt below.
            }
            System.out.printf("Enter a whole number from %d to %d.%n", min, max);
        }
    }

    private String readText(String prompt) {
        return readText(prompt, false);
    }

    private String readText(String prompt, boolean allowBlank) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim();
            if (allowBlank || !value.isBlank()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = input.nextLine().trim().toLowerCase(Locale.ROOT);
            if (value.equals("y") || value.equals("yes")) return true;
            if (value.equals("n") || value.equals("no")) return false;
            System.out.println("Please enter y or n.");
        }
    }

    private void pause() {
        System.out.print("\nPress Enter to return to the main menu...");
        input.nextLine();
    }

    private static final class MenuItem {
        final String name;
        final int price;
        MenuItem(String name, int price) {
            this.name = name;
            this.price = price;
        }
    }
}
