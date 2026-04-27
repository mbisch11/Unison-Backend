# SKILL.md

## Purpose

This file defines reusable development workflows ("skills") for the Unison backend.
Agents should follow these step-by-step patterns when implementing features.

---

## Skill: Create a New API Endpoint

### Steps

1. **Define Request Shape (if needed)**
   - Create a DTO in `groups/dto/` or relevant package
   - Use simple public fields

2. **Add Method to Service Layer**
   - Place in appropriate service (e.g., `StudyGroupService`)
   - Perform:
     - session validation (`SessionResolver`)
     - input validation
     - business logic
     - repository calls
   - Throw `IllegalArgumentException` for invalid cases

3. **Add Controller Endpoint**
   - Use correct annotation:
     - `@GetMapping`, `@PostMapping`, etc.
   - Extract inputs using:
     - `@RequestParam`
     - `@PathVariable`
     - `@RequestBody`
     - `@RequestHeader("X-Session-Id")`
   - Call service method directly
   - Return result

4. **Add Test**
   - Controller test → mock service
   - Service test → mock repositories

---

## Skill: Add a New Database Query

### Steps

1. Identify model field names exactly
2. Add method to repository using Spring Data naming:
   - Example:
     ```java
     findByCourseIdAndStartTimeBetween(String courseId, LocalDateTime from, LocalDateTime to)
     ```
3. Ensure:
   - Field names match model exactly
   - Types match (e.g., `LocalDateTime` vs `Instant`)
4. Use method inside service

---

## Skill: Implement Business Logic

### Steps

1. Always validate session first:
   ```java
   String userId = sessionResolver.requireUserId(sessionId);
2. Validate inputs:
    - null checks
    - empty strings
    - invalid values
3. Fetch required data from repositories
4. Apply rules:
    - capacity checks
    - existence checks
    - permission checks (future)
5. Persist changes
6. Optionally log events (e.g., AttendanceEvent)

---

## Skill: Join Group Flow
### Steps
1. Validate session → get userId
2. Fetch group
3. Check:
    - group exists
    - not already joined
    - capacity not exceeded
4. Create `GroupMembership`
5. Save membership
6. Log `AttendanceEvent("JOINED")`

---

## Skill: Confirm Attendance
### Steps
1. Validate session
2. Ensure membership exists
3. Update status (if applicable)
4. Log `AttendanceEvent("CONFIRMED")`

---

## Skill: Leave Group
### Steps
1. Validate session
2. Find membership
3. Delete membership
4. Log `AttendanceEvent("LEFT")`

---

## Skill: Write Service Test
### Steps
1. Mock all repositories
2. Mock `SessionResolver`
3. Inject mocks into service
4. Call method under test
5. Assert:
     - return values
     - exceptions
     - repository interactions

---

## Skill: Write Controller Test
### Steps
1. Use `@WebMvcTest`
2. Mock service with `@MockBean`
3. Use `MockMvc` to call endpoint
4. Assert:
    - HTTP status
    - JSON response
5. Verify service method was called

---

## Skill: Add New Model
### Steps
1. Create class in `model/`
2. Add:
    - `@Document(collection = "...")`
    - `@Id` field
3. Add fields + getters
4. Add no-args constructor
5. Create repository interface

---

## Skill: Error Handling
### Rules
 - Throw IllegalArgumentException in services
 - Do NOT catch exceptions in controllers
 - Let Spring return default error response
 - (Future) Replace with global exception handler

---

## Skill: Maintain Consistency
### Rules
 - Follow existing package structure
 - Do not introduce new patterns unnecessarily
 - Reuse existing services/repositories
 - Keep controllers thin
 - Keep services focused

---

## Skill: Debugging Issues
### Common Fixes
 - If query fails → check field name mismatch
 - If date fails → check type mismatch
 - If endpoint fails → check missing X-Session-Id
 - If test fails → check mocks

---

## Skill: Adding a New Feature
### Steps
1. Identify domain (auth/users/groups)
2. Define data model changes (if needed)
3. Add repository methods
4. Implement service logic
5. Add controller endpoint
6. Add tests

---

## Notes
 - Prefer clarity over cleverness
 - Avoid over-engineering
 - Keep logic readable and predictable
 - Follow REST conventions