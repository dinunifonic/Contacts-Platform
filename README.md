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

| Method | Endpoint                | Description         |
| ------ | ----------------------- | ------------------- |
| POST   | `/api/v1/contacts`      | Create a contact    |
| GET    | `/api/v1/contacts`      | Get all contacts    |
| GET    | `/api/v1/contacts/{id}` | Get a contact by ID |
| PUT    | `/api/v1/contacts/{id}` | Update a contact    |
| DELETE | `/api/v1/contacts/{id}` | Delete a contact    |

### Contact Groups

| Method | Endpoint                            | Description                       |
| ------ | ----------------------------------- | --------------------------------- |
| POST   | `/api/v1/groups`                    | Create a contact group            |
| GET    | `/api/v1/groups`                    | Get all contact groups            |
| GET    | `/api/v1/groups/{groupId}`          | Get a contact group by ID         |
| PUT    | `/api/v1/groups/{groupId}`          | Update a contact group            |
| DELETE | `/api/v1/groups/{groupId}`          | Delete a contact group            |
| GET    | `/api/v1/groups/{groupId}/contacts` | Get contacts belonging to a group |

Pagination and filtering for list endpoints will be defined as part of the API contract work in D2.

## Validation

Request DTOs use Jakarta Bean Validation.

For example:

* `firstName` and `lastName` must not be blank.
* `email` must not be blank and must have a valid email format.
* `phone` must not be blank.
* A group name must not be blank.

Validation is triggered at the REST resource layer using `@Valid`.

Invalid request payloads are therefore rejected before they reach the service layer.

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

## Swagger UI

The API is documented using OpenAPI.

When the application is running, Swagger UI is available at:

```text
http://localhost:8080/q/swagger-ui
```

Swagger UI can be used to explore and test the available endpoints without needing to know the implementation details of the application.

## Testing the API

The API can be tested through Swagger UI.

Example workflow:

1. Start the application.
2. Open Swagger UI.
3. Create a contact group using `POST /api/v1/groups`.
4. Create a contact using `POST /api/v1/contacts`.
5. Provide the group's ID as `groupId` if the contact should belong to that group.
6. Retrieve contacts or groups using the corresponding `GET` endpoints.
7. Update resources using the `PUT` endpoints.
8. Delete resources using the `DELETE` endpoints.

The CRUD endpoints were manually tested through Swagger UI during development.

## Configuration

The application uses Quarkus configuration profiles.

The application message differs between development and production:

```properties
app.message=Contacts API
%dev.app.message=Contacts API - DEV
%prod.app.message=Contacts API - PROD
```

This provides a development/production configuration difference without changing the application code.

Automatic Hibernate schema generation is disabled:

```properties
quarkus.hibernate-orm.schema-management.strategy=none
```

Database schema creation and changes will be managed through migrations.

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

### Group Membership

`ContactGroupRequest` does not contain a list of contact IDs.

Instead, the relationship is managed from the Contact side using `groupId`.

This keeps the relationship consistent with the `Contact` entity, which contains the reference to its `ContactGroup`.

### Database Schema Management

Automatic Hibernate schema generation is disabled.

The database schema will be created and modified through migration scripts rather than being automatically generated from the entity classes.

### OpenAPI / Swagger

OpenAPI documentation is used so that the API contract can be explored independently of the implementation.

Swagger UI provides an interactive way to understand and test the available endpoints.

## Known Decisions for Later API Design

Some API contract decisions are intentionally left for the next stage of development.

In particular:

* Pagination format for list endpoints
* Filtering parameters
* List response structure
* Standard HTTP status codes for all API operations
* Unified machine-readable error response
* Centralized error handling

These will be finalized according to the API standards defined for D2.

## API Contract

The exported OpenAPI contract is committed to the repository and represents the API contract independently of the implementation.

The contract will be updated whenever the API contract changes.
