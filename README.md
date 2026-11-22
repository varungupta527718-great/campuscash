# Student Expense Tracker

The tracker uses Java for its records, validation, calculations, file storage, and optional JDBC storage. The React page is the interface requested earlier; it sends requests to a small Java HTTP server. There is no sign-up or login.

## Run the React interface and Java API

Double-click `run-app.bat`. It starts the Java API and React development server in separate windows and opens the page. It uses the packages already in `frontend/node_modules`; if they are missing, the batch file installs them first. The computer needs a JDK, Node.js, and npm on PATH.

To start only the Java API, run `run-backend.bat` or `run-api.ps1`. The API listens on `http://127.0.0.1:8080`. To start the React page separately, open PowerShell in `frontend` and run `npm run dev`, then open the Vite URL shown in the terminal.

## Run the Java console application

Run `run.ps1` from this folder. It compiles every Java source file and starts the console menu. Option 11 runs the course-concepts workshop; option 0 exits.

To compile manually in PowerShell:

```powershell
$javaFiles = Get-ChildItem -Path .\src\main\java\com\studentexpensetracker -Filter *.java
javac -encoding UTF-8 -d out $javaFiles.FullName
java -cp out com.studentexpensetracker.Main
```

## Java methods and course topics

The Java logic uses named methods for common work instead of a JSON library, date library, or money library:

- `DateTools` checks dates and calculates the current date, month, and day differences.
- `MoneyTools` checks amounts, rounds to two decimal places, and formats them.
- `SimpleJson` reads the flat form data sent by the React interface.
- `CsvExpenseRepository` reads/writes CSV using character streams and its own small CSV parsing methods.
- `ExpenseService` adds, edits, searches, removes, and totals records using loops and collections.

The project examples cover classes, constructors, `this`, static members, inheritance, polymorphism, an abstract class, interfaces, 2D arrays, String immutability/pool/comparison/search, `StringBuilder`/`StringBuffer`, wrappers, checked and unchecked exceptions, custom exceptions, collections and iterators, sorting, generics, lambdas, method references, streams, threads, synchronization, thread states, `wait`/`notify`/`notifyAll`, JDBC, byte/character streams, `StreamTokenizer`, and serialization. Choose option 11 in the console to see runnable examples.

The React interface requires React, JavaScript, HTML, and CSS because it was requested as the UI. The Java HTTP server is also needed to connect it to the Java code; this networking bridge is outside the listed course topics. The Java application itself has no third-party Java-library dependency. Its regular imports are standard Java I/O, collections, threads, serialization, and the JDBC API from the course.

## Data files

- `data/expenses.csv` stores income and expense records.
- `data/tracker-settings.bin` stores the profile, category budgets, and savings goals using Java object serialization.
- Running workshop option 11 creates example files under `data/course-demo/`.

## Optional JDBC storage

CSV is the default. To use a database instead, set `EXPENSE_JDBC_URL` and optionally `EXPENSE_JDBC_USER` and `EXPENSE_JDBC_PASSWORD`, then provide the matching JDBC driver jar when starting Java. The JDBC repository uses `Connection`, `Statement`, `PreparedStatement`, and `ResultSet`.
