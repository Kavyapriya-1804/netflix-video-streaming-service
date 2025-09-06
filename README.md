# Netflix Video Streaming Service

This is the backend service for a Netflix clone application, built using **Spring Boot** and **Java**. The project uses **Maven** for dependency management and follows a modular structure for scalability and maintainability.

## Features

- **Video Management**: Manage video metadata such as title, description, content type, and file path.
- **Genre Management**: Organize videos into genres.
- **Custom Responses**: Use `CustomMessage` for consistent API responses.

## Technologies Used

- **Java**
- **Spring Boot**
- **Maven**
- **Jakarta Persistence API (JPA)** for ORM
- **Lombok** for reducing boilerplate code
- **ffmpeg** for video processing

## Project Structure

- `com.clone.netflix.entities`: Contains the entity classes for `Video` and `Genre`.
- `com.clone.netflix.playload`: Contains the `CustomMessage` class for API responses.

## Entity Overview

### Video
- **Fields**: `videoId`, `title`, `description`, `contentType`, `filePath`
- **Table**: `videos`

### Genre
- **Fields**: `id`, `title`
- **Table**: `genres`

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- A database (e.g., MySQL, PostgreSQL)
- ffmpeg installed on your system

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/netflix-clone-backend.git
   ```
2. Navigate to the project directory:
   ```bash
   cd netflix-clone-backend
   ```
3. Build the project:
   ```bash
   mvn clean install
   ```

### Running the Application

1. Configure the database connection in `application.properties` or `application.yml`.
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### API Endpoints

- **Video Management**: Endpoints for managing videos (to be implemented).
- **Genre Management**: Endpoints for managing genres (to be implemented).

## Contributing

Contributions are welcome! Please fork the repository and create a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
