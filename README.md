# Contacts Platform

A multi-module Quarkus application for managing contacts and contact groups.

## Project Structure

The project is organized as a Maven multi-module project:

```text
Contacts-Platform/
├── pom.xml
├── contacts-api/
│   ├── pom.xml
│   └── src/
└── contacts-reporter/
```

The `contacts-api` module contains the main REST API. The `contacts-reporter` module will be added in a later stage.

## Technologies

* Java 21
* Quarkus 3.39.5
* Maven
* RESTEasy Reactive / Jakarta REST
* Hibernate ORM with Panache
* Jakarta Bean Validation
* MySQL
* OpenAPI / Swagger UI
* Flyway migrations

## Architecture

The `contacts-api` follows a layered architecture:

```text
HTTP Request
     ↓
Resource
     ↓
Service
     ↓
Repository
     ↓
Entity
     ↓
Database
```

The layers have separate responsibilities:

* **Resource** — handles HTTP requests and responses.
* **Service** — contains application and business logic.
* **Repository** — handles persistence and database operations.
* **Entity** — represents the persistence model.
* **DTO** — represents the data exchanged through the API.

Resources do not access repositories directly, and persistence entities are not exposed directly through the REST API.

## API Versioning

All REST endpoints are currently versioned under:

```text
/api/v1
```

For example:

```text
/api/v1/contacts
/api/v1/groups
```

Versioning is included in the URL so that future API versions can be introduced without changing the existing contract.

## Endpoints

### Contacts

| Method | Endpoint                | Description                                                |
| ------ | ----------------------- | ---------------------------------------------------------- |
| POST   | `/api/v1/contacts`      | Create a contact                                           |
| GET    | `/api/v1/contacts`      | Get contacts, with pagination and optional group filtering |
| GET    | `/api/v1/contacts/{id}` | Get a contact by ID                                        |
| PUT    | `/api/v1/contacts/{id}` | Update a contact                                           |
| DELETE | `/api/v1/contacts/{id}` | Delete a contact                                           |

### Contact Groups

| Method | Endpoint                            | Description                                                     |
| ------ | ----------------------------------- | --------------------------------------------------------------- |
| POST   | `/api/v1/groups`                    | Create a contact group                                          |
| GET    | `/api/v1/groups`                    | Get contact groups, with pagination and optional name filtering |
| GET    | `/api/v1/groups/{groupId}`          | Get a contact group by ID                                       |
| PUT    | `/api/v1/groups/{groupId}`          | Update a contact group                                          |
| DELETE | `/api/v1/groups/{groupId}`          | Delete a contact group                                          |
| GET    | `/api/v1/groups/{groupId}/contacts` | Get paginated contacts belonging to a group                     |

## API Standards

### HTTP Methods and Status Codes

The API uses standard HTTP methods according to the operation being performed.

| Operation         | Method | Success Status   |
| ----------------- | ------ | ---------------- |
| Create resource   | POST   | `201 Created`    |
| Retrieve resource | GET    | `200 OK`         |
| Update resource   | PUT    | `200 OK`         |
| Delete resource   | DELETE | `204 No Content` |

POST requests return `201 Created` and include a `Location` header pointing to the newly created resource.

PUT requests return `200 OK` together with the updated resource representation.

DELETE requests return `204 No Content` because the resource has been successfully deleted and no response body is returned.

### Pagination

List endpoints support pagination using the following query parameters:

* `page` — zero-based page number, default `0`
* `size` — number of resources per page, default `20`
* Maximum `size` — `100`

Example:

```text
GET /api/v1/contacts?page=0&size=20
```

Invalid pagination values return `400 Bad Request`.

Paginated responses use the following structure:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Filtering

Contacts can be filtered by contact group ID:

```text
GET /api/v1/contacts?groupId=1
```

Contact groups can be filtered by exact name:

```text
GET /api/v1/groups?name=Friends
```

Filtering is performed on collection endpoints. If no resources match a filter, the API returns `200 OK` with an empty `content` list.

### Nested Group Contacts

Contacts belonging to a specific group can be retrieved using:

```text
GET /api/v1/groups/{groupId}/contacts
```

This endpoint supports the same pagination parameters:

```text
GET /api/v1/groups/1/contacts?page=0&size=20
```

If the group does not exist, the API returns `404 Not Found`.

If the group exists but contains no contacts, the API returns `200 OK` with an empty paginated response.

## Validation

Request DTOs use Jakarta Bean Validation.

For example:

* `firstName` and `lastName` must not be blank.
* `email` must not be blank and must have a valid email format.
* `phone` must not be blank.
* A group name must not be blank.

Validation is triggered at the REST resource layer using `@Valid`.

Invalid request payloads return `400 Bad Request`.

## Error Handling

The API uses a single exception mapper to provide a consistent error response format.

Errors use the media type:

```text
application/problem+json
```

The error response follows the RFC 9457 Problem Details structure:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "firstName: must not be blank",
  "instance": "/api/v1/contacts"
}
```

The response contains:

* `type`
* `title`
* `status`
* `detail`
* `instance`

The main error cases are:

| Situation                            | Status                      |
| ------------------------------------ | --------------------------- |
| Invalid request / validation failure | `400 Bad Request`           |
| Resource not found                   | `404 Not Found`             |
| Duplicate unique value               | `409 Conflict`              |
| Unexpected server error              | `500 Internal Server Error` |

A duplicate email or group name is returned as `409 Conflict`.

Raw database constraint error messages are not exposed to API clients.

## Data Model

### Contact

A Contact contains:

* `id`
* `firstName`
* `lastName`
* `email`
* `phone`
* `groupId`
* `createdTimestamp`

Email addresses are unique.

### ContactGroup

A ContactGroup contains:

* `id`
* `name`
* `description`
* `createdTimestamp`

Group names are unique.

### Contact–ContactGroup Relationship

A Contact can optionally belong to a ContactGroup.

A contact does not have to belong to a group, so `groupId` is optional when creating or updating a contact.

Group membership is managed through the Contact API rather than by sending a list of contact IDs when creating or updating a group.

If a contact has no group, its `groupId` is returned as `null`.

## OpenAPI / Swagger

The API is documented using MicroProfile OpenAPI annotations.

The OpenAPI documentation includes:

* Endpoint operations and descriptions
* HTTP response codes
* Request schemas
* Response schemas
* Path parameters
* Query parameters
* DTO field descriptions
* Example values

Swagger UI is available at:

```text
http://localhost:8080/q/swagger-ui
```

Swagger UI can be used to explore and test the API without needing to know the implementation details.

## How to Run

### Prerequisites

Make sure the following are installed:

* Java 21
* Docker
* Git

### Start the application

From the project root:

```bash
./mvnw quarkus:dev
```

On Windows PowerShell:

```powershell
.\mvnw.cmd quarkus:dev
```

The application runs by default at:

```text
http://localhost:8080
```

Quarkus development mode provides live reload, so changes to the application can be tested without manually restarting the application.

## Testing the API

The API can be tested through Swagger UI.

Example workflow:

1. Start the application.
2. Open Swagger UI.
3. Create a contact group using `POST /api/v1/groups`.
4. Create a contact using `POST /api/v1/contacts`.
5. Provide the group's ID as `groupId` if the contact should belong to that group.
6. Retrieve contacts or groups using the corresponding `GET` endpoints.
7. Test pagination using `page` and `size`.
8. Test filtering using `groupId` or `name`.
9. Update resources using the `PUT` endpoints.
10. Delete resources using the `DELETE` endpoints.
11. Test validation and error responses using invalid requests.

The CRUD, pagination, filtering, validation, status codes, and error handling were manually tested through Swagger UI during development.

## Configuration

The application uses Quarkus configuration profiles.

The application message differs between development and production:

```properties
app.message=Contacts API
%dev.app.message=Contacts API - DEV
%prod.app.message=Contacts API - PROD
```

This provides a development/production configuration difference without changing the application code.

Database schema management is intended to be handled through migration scripts rather than being generated from the entity classes.

## Design Decisions

### Layered Architecture

The API uses the following structure:

```text
Resource → Service → Repository → Entity
```

This separates HTTP handling, business logic, and persistence responsibilities.

The Resource layer does not access persistence directly, and business logic is kept out of the Resource layer.

### DTOs

Request and response DTOs are used instead of exposing persistence entities directly.

Request DTOs contain data accepted from API clients, while response DTOs contain data intentionally returned by the API.

This keeps the API contract separate from the database model.

### Optional Contact Group

A Contact can exist without belonging to a ContactGroup.

This was chosen so that contacts can remain in the system even when they are not assigned to a group.

Therefore, `groupId` is optional in `ContactRequest`.

### Phone Numbers as Strings

Phone numbers are stored as strings rather than numeric values.

A phone number is an identifier rather than a quantity. Using a string preserves leading zeros and allows formatting characters such as `+`, spaces, and `-`.

### API Versioning

The API uses `/api/v1` for its current endpoints.

This makes the API version explicit and allows future versions to be introduced without changing the existing API contract.

### Resource Naming

Collection resources use plural names:

```text
/api/v1/contacts
/api/v1/groups
```

The nested group endpoint:

```text
/api/v1/groups/{groupId}/contacts
```

represents the relationship between a group and the contacts belonging to it.

### Group Membership

`ContactGroupRequest` does not contain a list of contact IDs.

Instead, the relationship is managed from the Contact side using `groupId`.

This keeps the relationship consistent with the `Contact` entity, which contains the reference to its `ContactGroup`.

### Pagination

Pagination uses zero-based page numbering.

The default page size is `20`, with a maximum of `100`.

A common `PaginatedResponse<T>` structure is used so that contacts and groups return pagination information in the same format.

### Filtering

Contacts are filtered using `groupId`, while groups are filtered using their exact `name`.

Collection filters return an empty result rather than `404` when no resources match.

A `404` is reserved for requests targeting a specific resource that does not exist, such as:

```text
GET /api/v1/groups/999
```

### Error Handling

A single exception mapper is used to provide a consistent `application/problem+json` response format.

This keeps error responses consistent across the API instead of returning different error structures for different exceptions.

### OpenAPI / Swagger

OpenAPI documentation is used so that the API contract can be explored independently of the implementation.

Swagger UI provides an interactive way to understand and test the available endpoints, including their parameters, request bodies, response schemas, and status codes.

### Database Schema Management

Database schema creation and changes will be managed through migration scripts rather than being automatically generated from entity classes.

## API Contract

The exported OpenAPI contract is committed to the repository and represents the API contract independently of the implementation.

The contract will be updated whenever the API contract changes.
