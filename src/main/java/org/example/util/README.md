# 🛠️ Util Package

**Path:** `src/main/java/org/example/util`

## 📌 Purpose
This folder contains utility classes that provide shared, reusable helper functions for the rest of the application.

## 📝 Files & Data Flow

### `DBUtil.java`
* **What it is:** The central Database Connection manager.
* **How it works:** It reads connection details (URL, username, password) from the `db.properties` file in `src/main/resources/`.
* **Key Method:** `public static Connection getConnection()`
  * This method loads the Oracle JDBC driver (`oracle.jdbc.OracleDriver`).
  * It returns an active connection to the Oracle `FREEPDB1` database.
* **Data Flow Impact:** Every single Servlet in the application uses `DBUtil.getConnection()` anytime it needs to query the database or save data. Without this file, the application cannot access its memory!
