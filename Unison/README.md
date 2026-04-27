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

Set your MongoDB connection using an environment variable:

```
$env:MONGODB_URI="mongodb://localhost:27017/unison"
```

The default fallback is already configured for local development:

```
spring.data.mongodb.uri=mongodb://localhost:27017/unison
```

For MongoDB Atlas, use:

```
$env:MONGODB_URI="mongodb+srv://<username>:<password>@cluster.mongodb.net/unison"
```

If you prefer Spring profiles, copy:

```
src/main/resources/application-local.properties.example
```

to:

```
src/main/resources/application-local.properties
```

then run with:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
./gradlew.bat bootRun
```

## Admin Bootstrap

Admin accounts cannot use the public signup flow. They must be created from a one-time invite link.

To generate the very first admin invite on startup, set:

```powershell
$env:UNISON_BOOTSTRAP_ADMIN_ENABLED="true"
$env:UNISON_BOOTSTRAP_ADMIN_EMAIL="admin@example.com"
$env:UNISON_BOOTSTRAP_ADMIN_EXPIRES_HOURS="24"
$env:UNISON_FRONTEND_BASE_URL="http://localhost:5173"
```

Then start the backend and copy the one-time admin signup link from the backend logs.

## New Backend Features

- Admin invite workflow:
  - `POST /admin/invites`
  - `GET /admin/invites`
  - `GET /admin/invites/validate?token=...`
  - `POST /admin/invites/{inviteId}/revoke`
  - `POST /auth/admin/signup`
- Notifications:
  - `GET /notifications`
  - `POST /notifications/{notificationId}/read`
  - `POST /notifications/read-all`
- Calendar:
  - `GET /calendar/events`
  - `GET /calendar/export.ics`

Study groups now also support `durationMinutes`, which is used for reminders and calendar exports.

## Running the Application

Using Gradle wrapper:
```
./gradlew bootRun
```

OR on Windows:
```
gradlew.bat bootRun
```

With an env var on Windows PowerShell:
```powershell
$env:MONGODB_URI="mongodb://localhost:27017/unison"
./gradlew.bat bootRun
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
- Notification reminders run on a scheduler. You can tune them with:
  - `UNISON_NOTIFICATION_REMINDER_WINDOW_MINUTES`
  - `UNISON_NOTIFICATION_REMINDER_RATE_MS`
