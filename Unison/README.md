# Unison Backend

This is the backend for the Unison application. It is built using Spring Boot and MongoDB.

## Prerequisites

Before running this project, ensure you have:

- Java 21
- Gradle (or use included wrapper)
- MongoDB (local or Atlas)

Check versions:

```
java -version
```


## Getting the Code

1. Download the backend zip
2. Extract it
3. Navigate into the backend folder

## Configuration

Update your MongoDB connection in:

```
src/main/resources/application.properties
```

Example:

```
spring.data.mongodb.uri=mongodb://localhost:27017/unison
```

OR for MongoDB Atlas

```
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@cluster.mongodb.net/unison
```

## Running the Application

Using Gradle wrapper:
```
./gradlew bootRun
```

OR on Windows:
```
gradlew.bat bootRun
```


Alternatively, run from your IDE (recommended for development).

## API Access

Once running, backend will be available at: [http://localhost:8080]("http://localhost:8080")

## Testing the API

You can test endpoints using:

- Postman
- curl
- Browser (for GET requests)

## Project Structure

- `controller/` - API endpoints
- `service/` - Business logic
- `repository/` - MongoDB access
- `model/` - Data models

## Notes

- Make sure MongoDB is running before starting the backend
- Frontend depends on this API being active