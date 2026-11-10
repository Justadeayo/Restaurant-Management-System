# Restaurant Management System (RMS)

RMS is a desktop application for running a restaurant's daily work: taking orders, recording payments, tracking stock, and reporting on sales. It is built with Java Swing, JDBC, MySQL, and Gradle.

There are two kinds of staff accounts:

- **Manager:** manages the menu, stock, staff accounts, and sales reports.
- **Cashier:** takes orders and records payments.

Payments are simulated (cash, card, or transfer), and stock is deducted automatically when a payment succeeds. Menu items are deactivated rather than deleted, so order history stays intact.

---

## Setup

### Requirements

- JDK 26 (Gradle downloads it automatically if it is missing)
- MySQL Server 8 or newer
- Git

### 1. Create the database

Start the MySQL server, then run the schema from the project root:

```
mysql -u root -p < database_schema.sql
```

This creates the `restaurant_management` database, its tables, and a default manager account.

### 2. Configure the database connection

Copy `config.properties.example` to `config.properties` in the project root, then set your MySQL details:

```
db.url=jdbc:mysql://localhost:3306/restaurant_management
db.user=root
db.password=your_mysql_password
```

`config.properties` is listed in `.gitignore`, so your password is never committed.

### 3. Build and run

macOS and Linux:

```
./gradlew run
```

Windows:

```
gradlew.bat run
```

To build without running:

```
./gradlew build
```