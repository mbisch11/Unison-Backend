# AGENTS.md

## Project Overview
Unison is a Spring Boot + MongoDB backend for a Study Group Finder application.  
It allows users to authenticate, create study groups, join sessions, and track participation.

Tech Stack:
- Java 21
- Spring Boot
- MongoDB
- Gradle

---

## Architecture

This project follows a layered architecture:

- **controller/** → API endpoints (HTTP layer)
- **service/** → business logic (rules, validation)
- **repo/** → MongoDB access (Spring Data)
- **model/** → MongoDB documents

STRICT RULE:
- Controllers must NOT contain business logic.
- Services must handle all validation and rules.
- Repositories are only for database access.

---

## Core Features to Implement

### Authentication
- Session-based authentication using `AuthSession`
- All protected endpoints require `X-Session-Id`
- `SessionResolver` is the single source of truth for user identity

### Users
- Create user profiles
- Retrieve user info
- Associate users with course IDs

### Study Groups
- Create study groups
- Search/filter by:
  - courseId
  - time range
  - virtual/in-person
- Join group (with capacity enforcement)
- Leave group
- Confirm attendance
- List group members

### Participation Tracking
- Log actions using `AttendanceEvent`
- Track:
  - JOINED
  - LEFT
  - CONFIRMED
- Reliability scoring may be added later

---

## Database Design

Collections:
- `users`
- `auth_sessions`
- `study_groups`
- `group_memberships`
- `attendance_events`

RULES:
- Do NOT merge collections
- Use separate documents for relationships (e.g., GroupMembership)
- Avoid embedding large lists inside StudyGroup

---

## Coding Conventions

### General
- Use clear, descriptive method names
- Prefer small, focused methods
- Avoid unnecessary abstraction

### Naming
- camelCase for variables and methods
- PascalCase for classes
- Repository methods must match field names EXACTLY

### Validation
- Always validate inputs in service layer
- Throw `IllegalArgumentException` for invalid requests

---

## API Design Rules

- Use REST conventions:
  - GET → read
  - POST → create/action
  - DELETE → remove
- Always return appropriate HTTP status codes
- Use `@RequestHeader("X-Session-Id")` for authentication

---

## Testing Rules

- Use JUnit + Mockito
- Mock repositories in service tests
- Mock services in controller tests
- Do NOT require a real MongoDB instance

Run tests:
```bash
./gradlew test
```

---

## Agent Behavior Guidelines
### Do
 - Follow existing architecture strictly
 - Reuse existing services/repositories
 - Keep changes minimal and consistent
 - Add validation where missing
 - Write tests when adding new logic
### Do NOT
 - Add new frameworks or dependencies without reason
 - Put logic inside controllers
 - Change database structure unless explicitly required
 - Rename fields used by repositories (breaks queries)

---

## Common Pitfalls
 - Mismatched field names → breaks Spring Data queries
 - Using wrong date types (must match model + repo)
 - Missing @Id or @Document
 - Forgetting X-Session-Id in controllers

---

## Future Enhancements (Optional)
 - Reliability scoring system
 - Notification system
 - Role-based authorization (admin/moderation)
 - Calendar integration

---

## Key Entry Points
 - Main app: UnisonApplication.java
 - Auth: auth/
 - Users: users/
 - Groups: groups/

Start here when adding new features:

 - Groups → StudyGroupService
 - Auth → SessionResolver

---