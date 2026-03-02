# Unison – Study Group Finder Backend

## Overview

Unison is a Spring Boot + MongoDB backend for a university-integrated study group platform. It allows students to create, discover, and manage course-based study sessions while tracking participation and enforcing accountability.

Built with:
- Java 21
- Spring Boot
- MongoDB
- Gradle

---

## Core Features

### Authentication
- Simulated login/logout
- Session validation via `X-Session-Id`
- Role-ready design

### Users
- Create user profiles
- Retrieve user information
- Associate users with course IDs

### Study Groups
- Create study sessions
- Search/filter by course and time
- Join / leave groups
- Confirm attendance
- Enforce capacity limits
- List group members

---

## Project Structure
src/main/java/com/_bit/Unison/
auth/
users/
groups/


Each package contains model, repository, service, and controller layers (where applicable).

---

## Running the Application

### Requirements
- Java 21
- MongoDB running locally on port 27017

### Mongo Configuration
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/unison
```

### Run
```agsl
./gradlew bootRun
```

### Run Tests
```agsl
./gradlew test
```

---
## Contributions

| Contributor      | Contribution                                                                                                                                            |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Michael Bischoff | Overall backend architecture design, Authentication System implementation, StudyGroupService, StudyGroupController, StudyGroupRepository implementation |
| Angel Isardat    | Implemented UserProfile model, service, repository and controller, implemented search/filter query design                                               |
| Zach Lucero      | Implemented GroupMembership model, repository, service and controller                                                                                   |
| Brendan Chan     | Wrote all test cases as well as Study group model.                                                                                                      
