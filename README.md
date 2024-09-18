# Server - Client WebMail Service

This project is a simple webmail client implemented in Java using the JavaFX framework for the graphical user interface. The project is built with Maven.

## Features

- User authentication with email and password
- Email sending and receiving
- Server connection status checking
- Password validation

## Project Structure

The project follows the standard Maven project structure. The main source code is located in the `src/main/java` directory. The project is divided into several packages, each containing related classes.

The `com.prog3.email.prog3_webmail.Client` package contains the main classes for the application:

- `ClientMain.java`: The entry point of the application. It sets up the main window and starts the JavaFX application.
- `LoginController.java`: Handles user login, including password validation and server connection checking.

## How to Run

To run the project, you need to have Java and Maven installed on your machine. You can then use the following command to compile and run the project:

```bash
mvn clean compile exec:java
```

## Contributing

Contributions are welcome. Please open an issue to discuss your idea or submit a pull request.

## License

This project is licensed under the terms of the MIT license.
