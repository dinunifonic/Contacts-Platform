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
│       ├── main/
│       └── test/
│           ├── resource/
│           │   ├── ContactResourceTest.java
│           │   └── ContactGroupResourceTest.java
│           └── service/
│               ├── ContactServiceTest.java
│               └── ContactGroupServiceTest.java
└── contacts-reporter/
    ├── pom.xml
    └── src/
        ├── main/
        │   └── service/
        │       └── ContactReporterService.java
        └── test/
            ├── resource/
            │   └── ContactReporterResourceTest.java
            └── service/
                └── ContactReporterServiceTest.java
```

Test classes live under `src/test/java/com/contacts/`, mirroring the package of the class they test.

The `contacts-api` module contains the main REST API, including contact and group management, persistence, validation, and security. The `contacts-reporter` module is a second Quarkus application that calls the API and exposes reporting data.

## Technologies

* Java 21
* Quarkus 3.39.5
* Maven
* Jakarta REST / Quarkus REST
* Hibernate ORM with Panache
* Jakarta Bean Validation
* MySQL
* Flyway
* Quarkus Dev Services
* OpenAPI / Swagger UI
* Keycloak
* Quarkus OIDC and OIDC Client
* MicroProfile REST Client
* JUnit 5, Mockito and AssertJ
* `@QuarkusTest` with RestAssured
* Testcontainers (through Quarkus Dev Services)
* JaCoCo

## Architecture

The `contacts-api` follows a layered architecture:

```text
HTTP Request
    ↓
Resource
    ↓
DTO
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

* **Resource** - handles HTTP requests and responses.
* **DTO** - represents data exchanged through the API.
* **Service** - contains application and business logic.
* **Repository** - handles persistence and database operations.
* **Entity** - represents the persistence model.

Resources do not access repositories directly, and persistence entities are not exposed directly through the REST API.

The `contacts-reporter` follows a smaller structure. Its REST resource receives reporting requests, its service derives reporting data, its REST client calls `contacts-api`, and its DTOs represent the data returned by the API. It does not maintain its own database.

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

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/contacts` | Create a contact |
| GET | `/api/v1/contacts` | Get contacts, with pagination and optional group filtering |
| GET | `/api/v1/contacts/{id}` | Get a contact by ID |
| PUT | `/api/v1/contacts/{id}` | Update a contact |
| DELETE | `/api/v1/contacts/{id}` | Delete a contact |

### Contact Groups

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/groups` | Create a contact group |
| GET | `/api/v1/groups` | Get contact groups, with pagination and optional name filtering |
| GET | `/api/v1/groups/{groupId}` | Get a contact group by ID |
| PUT | `/api/v1/groups/{groupId}` | Update a contact group |
| DELETE | `/api/v1/groups/{groupId}` | Delete a contact group |
| GET | `/api/v1/groups/{groupId}/contacts` | Get paginated contacts belonging to a group |

### Reporting

| Method | Endpoint | Description |
|---|---|---|
| GET | `/reporters/contacts/count` | Return the total number of contacts through `contacts-reporter` |

The reporter endpoint calls `GET /api/v1/contacts` as the reporting service account (`read-contacts` role) and returns a derived count. It does not query the database directly.

## API Standards

### HTTP Methods and Status Codes

The API uses standard HTTP methods according to the operation being performed.

| Operation | Method | Success Status |
|---|---|---|
| Create resource | POST | `201 Created` |
| Retrieve resource | GET | `200 OK` |
| Update resource | PUT | `200 OK` |
| Delete resource | DELETE | `204 No Content` |

POST requests return `201 Created` and include a `Location` header pointing to the newly created resource.

PUT requests return `200 OK` together with the updated resource representation.

DELETE requests return `204 No Content` because the resource has been successfully deleted and no response body is returned.

### Pagination

List endpoints support pagination using the following query parameters:

* `page` --> zero-based page number, default `0`
* `size` --> number of resources per page, default `20`
* Maximum `size` = `100`

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

| Situation | Status |
|---|---|
| Invalid request / validation failure | `400 Bad Request` |
| Resource not found | `404 Not Found` |
| Duplicate unique value | `409 Conflict` |
| Framework errors, such as an unknown URL or malformed JSON | The status of the original `WebApplicationException`, for example `404` or `400` |
| Unexpected server error | `500 Internal Server Error` |

A duplicate email or group name is returned as `409 Conflict`.

Raw database constraint error messages are not exposed to API clients.

For unexpected `500` errors, the real exception is written to the server log, while the caller only receives a generic message (`An unexpected error occurred`). Internal details such as class names or SQL are never sent to the caller.

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
* `owner`

Email addresses are unique. The owner identifies the user associated with a contact and is used to enforce data isolation.

### ContactGroup

A ContactGroup contains:

* `id`
* `name`
* `description`
* `createdTimestamp`

Group names are unique.

### Contact-to-ContactGroup Relationship

A Contact can optionally belong to a ContactGroup.

A contact does not have to belong to a group, so `groupId` is optional when creating or updating a contact.

Group membership is managed through the Contact API rather than by sending a list of contact IDs when creating or updating a group.

If a contact has no group, its `groupId` is returned as `null`.

The relationship is represented as:

```text
ContactGroup
    1
    |
    | contains
    |
    *
 Contact
```

The `Contact` entity owns the relationship through its `contactGroup` field. The `ContactGroup` entity uses `mappedBy = "contactGroup"` for the inverse side.

## Persistence

### Hibernate ORM and Panache

Hibernate ORM with Panache is used for object-relational mapping and persistence.

Entities are mapped to database tables, while Panache repositories provide the database access layer.

The application uses repositories rather than exposing persistence operations directly from the REST resources.

### Database Migrations

Flyway is used to manage the database schema.

The initial schema is created by:

```text
contacts-api/src/main/resources/db/migration/V1__create_contacts_schema.sql
```

The migration creates:

* `contact_group`
* `contact`
* Primary keys
* Unique constraints for group names and contact emails
* The foreign key between contacts and contact groups

Later migrations introduce additional schema changes. For example, the owner field is added through a versioned migration rather than by relying on automatic Hibernate schema generation.

The `contact_group` table is created before the `contact` table because the contact table contains a foreign key referencing the group table.

### Migration-Only Schema Management

Hibernate schema generation is disabled:

```properties
quarkus.hibernate-orm.schema-management.strategy=none
```

This means Hibernate does not create or modify database tables at runtime.

Flyway is responsible for creating and updating the database schema.

This was chosen so that database structure is explicitly version-controlled through migration scripts rather than being implicitly generated from entity classes.

### ID Generation

The entities use explicitly defined IDs:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

The database columns use MySQL `AUTO_INCREMENT`.

This keeps the Hibernate ID generation strategy consistent with the database schema managed by Flyway.

### Transactions

Database-changing operations are executed within transactions using `@Transactional` in the service layer.

This keeps transaction management outside the REST resource layer and ensures that persistence operations are handled as part of the application's business logic.

### Dev Services

Quarkus Dev Services is used to provide a MySQL database automatically during development and testing.

No fixed database URL, username, or password is required for the development database.

When the application starts in development mode, Quarkus starts a MySQL container automatically through Docker.

The development database is then initialized by Flyway.

The resulting startup flow is:

```text
Quarkus starts
     ↓
Dev Services starts MySQL
     ↓
Flyway validates migrations
     ↓
Flyway applies pending migrations
     ↓
Hibernate connects to the existing schema
     ↓
Application starts
```

This allows the application to use a reproducible database environment without requiring a manually configured local MySQL server.

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

### Start Keycloak

Keycloak must be running before `contacts-api` or `contacts-reporter` are started. The whole realm configuration is committed in:

```text
keycloak/realm-export.json
```

Start Keycloak 26.8.0 with Docker and import the realm from the project root.

On Windows PowerShell:

```powershell
docker run --name keycloak -p 8081:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=CHOOSE_A_LOCAL_PASSWORD `
  -v "${PWD}/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json" `
  quay.io/keycloak/keycloak:26.8.0 start-dev --import-realm
```

* Keycloak runs on port `8081`, so it does not clash with `contacts-api` on `8080`.
* `--import-realm` creates the `contacts` realm from the mounted file on startup.
* Only `realm-export.json` is mounted, so no other file in the `keycloak/` folder is imported by mistake.
* The bootstrap admin is only for the local Keycloak admin console. Choose a local password; it is not used by the applications.

The admin console is available at:

```text
http://localhost:8081
```

#### What the Realm Contains

| Item | Name | Purpose |
|---|---|---|
| Realm | `contacts` | Realm trusted by `contacts-api` |
| Client | `contacts-api` | Resource server; defines the `read-contacts` client role |
| Client | `contacts-frontend` | Public client used by Swagger UI for user login |
| Client | `contacts-reporter` | Confidential client with a service account, used by the reporter |
| Client | `audience-test` | Client used to demonstrate that a token with the wrong audience is rejected |
| Realm role | `user` | Ordinary user: read and create own contacts |
| Realm role | `admin` | Administrator: full access, including delete |
| Client role | `read-contacts` | Assigned to the `contacts-reporter` service account |

#### Set the Reporter Secret After Import

Client secrets are masked (`**********`) in the export, so they are never stored in the repository. After importing the realm, generate a new secret for the reporter:

1. Open the admin console and select the `contacts` realm.
2. Go to **Clients → contacts-reporter → Credentials**.
3. Click **Regenerate** and copy the new client secret.
4. Provide it to the reporter through the `REPORTER_CLIENT_SECRET` environment variable, as described in [Start the Reporter](#start-the-reporter).

#### Create the Test Users

To demonstrate role-based access and the owner rule, two users are needed in the `contacts` realm:

| Username | Realm roles | Can |
|---|---|---|
| `alice` | `user` | Read and create her own contacts |
| `admin-user` | `user`, `admin` | Read all contacts and delete contacts |

If they are not present after the import, create them in the admin console:

1. Go to **Users → Create new user**, enter the username and click **Create**.
2. Open the **Credentials** tab, set a password and turn **Temporary** off.
3. Open the **Role mapping** tab, click **Assign role**, filter by realm roles and assign the roles from the table above.

These are local test users only. Their passwords must not be real passwords used anywhere else.

#### Verify the Setup

Fetch a token for the reporter's service account, with no user involved:

```powershell
curl.exe -X POST http://localhost:8081/realms/contacts/protocol/openid-connect/token `
  -d "grant_type=client_credentials" `
  -d "client_id=contacts-reporter" `
  -d "client_secret=$env:REPORTER_CLIENT_SECRET"
```

A successful response contains an `access_token`. Decoding it should show `iss` ending in `/realms/contacts`, `contacts-api` in `aud`, and `read-contacts` under `resource_access.contacts-api.roles`.

To start again from a clean state, remove the container and run the `docker run` command again:

```powershell
docker rm -f keycloak
```

### Start the API

From the project root:

```bash
./mvnw -pl contacts-api quarkus:dev
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -pl contacts-api quarkus:dev
```

The API runs at:

```text
http://localhost:8080
```

Quarkus development mode provides live reload, so changes to the application can be tested without manually restarting the application.

When running in development mode, Quarkus Dev Services automatically starts the MySQL database when Docker is available.

Flyway then applies the database migrations before the application starts.

### Start the Reporter

The reporter needs the API and Keycloak to be running. Configure its client secret through an environment variable before starting it.

On Windows PowerShell:

```powershell
$env:REPORTER_CLIENT_SECRET = "YOUR_ACTUAL_CLIENT_SECRET"
.\mvnw.cmd -pl contacts-reporter quarkus:dev
```

The reporter runs at:

```text
http://localhost:8082
```

The secret must be set locally. Do not commit the actual value to the repository.

## Testing the API

The API can be tested through Swagger UI.

Example workflow:

1. Start the application.
2. Open Swagger UI.
3. Create a contact group using `POST /api/v1/groups`.
4. Retrieve the group using `GET /api/v1/groups/{id}`.
5. Create a contact using `POST /api/v1/contacts`.
6. Provide the group's ID as `groupId` if the contact should belong to that group.
7. Retrieve contacts or groups using the corresponding `GET` endpoints.
8. Retrieve the contacts of a group using `GET /api/v1/groups/{groupId}/contacts`.
9. Test pagination using `page` and `size`.
10. Test filtering using `groupId` or `name`.
11. Update resources using the `PUT` endpoints.
12. Delete resources using the `DELETE` endpoints.
13. Test validation and error responses using invalid requests.

The CRUD, pagination, filtering, validation, status codes, error handling, and persistence behavior were manually tested through Swagger UI during development.

## Automated Tests

### Test Pyramid

The test suite follows the test pyramid:

```text
        @QuarkusTest + RestAssured
       (endpoints, security, database)
                    ↑
         Unit tests (JUnit 5 + Mockito)
          (business logic in services)
```

* **Unit tests** test the service classes in isolation. Repositories, `JsonWebToken`, `SecurityIdentity` and the REST client are mocked with Mockito, so these tests are fast and do not start Quarkus.
* **Integration tests** start the application with `@QuarkusTest` and call the real HTTP endpoints with RestAssured, against a real MySQL database.

Every test follows the Arrange / Act / Assert (AAA) structure and is named as a sentence describing the behaviour, for example `deleteContact_asRegularUser_shouldReturn403`.

### Test Classes

| Module | Test class | Type | Covers |
|---|---|---|---|
| `contacts-api` | `ContactServiceTest` | Unit | Contact business logic, owner rules, admin access, paging maths, missing groups, publishing a `ContactCreatedEvent` when a contact is created |
| `contacts-api` | `ContactGroupServiceTest` | Unit | Group business logic, paging maths, unassigning contacts before deleting a group |
| `contacts-api` | `ContactResourceTest` | Integration | Contact endpoints: happy path, invalid input, not found, duplicate, security, `read-contacts` access, malformed JSON and unknown paths |
| `contacts-api` | `ContactGroupResourceTest` | Integration | Group endpoints: happy path, invalid input, not found, duplicate, security, contacts of a group |
| `contacts-api` | `ContactEventPublisherTest` | Unit | Event sent to RabbitMQ and Kafka, contact ID used as the Kafka key |
| `contacts-api` | `ContactEventPublisherKafkaTest` | Integration | Real Kafka broker: events of the same contact land in the same partition and arrive in publish order |
| `contacts-reporter` | `ContactEventConsumerTest` | Unit | Idempotent consumer: duplicate events on either broker are processed only once |
| `contacts-reporter` | `ContactReporterServiceTest` | Unit | Total contacts derived from the API response |
| `contacts-reporter` | `ContactReporterResourceTest` | Integration | Reporter count endpoint with a mocked `ContactsApiClient`, and that the raw contact list is not exposed |

At least one parameterised test (`@ParameterizedTest`) is included, for example invalid pagination values and different contact names.

### What the Integration Tests Verify

For each endpoint, the integration tests cover:

| Case | Expected status |
|---|---|
| Happy path | `200 OK` / `201 Created` / `204 No Content` |
| Invalid input | `400 Bad Request` |
| Resource not found | `404 Not Found` |
| Duplicate email or group name | `409 Conflict` |

Error responses are also checked for the `application/problem+json` media type.

### Security Tests

Security is tested with `@TestSecurity` and `@OidcSecurity` from `quarkus-test-security-oidc`. These annotations define the authenticated user, its roles and its `sub` claim for a single test, without requiring Keycloak.

| Scenario | Expected status |
|---|---|
| No token | `401 Unauthorized` |
| Wrong role, for example a `user` deleting a contact | `403 Forbidden` |
| Correct role, for example an `admin` deleting a contact | `204 No Content` |
| `read-contacts` listing contacts | `200 OK`, including other users' contacts |
| `read-contacts` creating a contact or reading a single contact | `403 Forbidden` |

Data isolation is tested with two different users: a contact owned by `user-a` returns `404 Not Found` for `user-b`, while an `admin` can still read it.

### Test Data and Isolation

All `@QuarkusTest` classes share one database during a test run. To keep tests independent of their execution order:

* Each test uses its own unique email addresses and group names, because both are unique in the database.
* Tests that count results use their own user (`sub` claim), so contacts created by other tests are not visible to them.
* Data owned by another user is arranged directly through the repository inside `QuarkusTransaction.requiringNew()`, so it is committed before the HTTP call is made.

### Dev Services and Testcontainers

No database configuration is needed for tests. When `@QuarkusTest` starts, Quarkus Dev Services uses Testcontainers to start a MySQL container through Docker, and Flyway builds the schema from the migrations.

Docker must therefore be running before the tests are started.

The `contacts-reporter` tests do not need `contacts-api` or Keycloak. The REST client is replaced with a Mockito mock using `@InjectMock @RestClient`.

### Coverage Gate

Line coverage is measured with JaCoCo in both modules:

* `quarkus-jacoco` records coverage for `@QuarkusTest` tests.
* `jacoco-maven-plugin` (`prepare-agent`) records coverage for the plain unit tests.
* Both write into the same file, `target/jacoco-quarkus.exec`.

The `check` goal runs in the `verify` phase and fails the build if line coverage is below 80% in:

```text
com.contacts.service
com.contacts.resource
```

The gate was verified by temporarily raising the minimum and disabling a test class, which made the build fail with a `Rule violated` message.

Coverage is a floor, not a goal. The suite is meant to fail when behaviour changes, not only to reach a percentage.

### Running the Tests

Make sure Docker is running.

In IntelliJ, open the **Maven** tool window, expand the module, select **Lifecycle → clean** and **verify**, and run them.

Or from a module folder on Windows PowerShell:

```powershell
cd contacts-api
.\mvnw.cmd clean verify
```

The build output shows the test results and the coverage gate result:

```text
Tests run: ..., Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

The HTML coverage report is generated at:

```text
contacts-api/target/jacoco-report/index.html
contacts-reporter/target/jacoco-report/index.html
```

### Test Configuration

Test-only configuration in `application.properties`:

```properties
quarkus.http.test-port=0
%test.quarkus.jacoco.reuse-data-file=true
```

* `quarkus.http.test-port=0` runs the tests on a random free port, so they do not clash with Keycloak on `8081`.
* `reuse-data-file` lets JaCoCo combine unit-test and `@QuarkusTest` coverage in one report.

In `contacts-reporter`, the client secret is given a placeholder value in the test profile only, because the REST client is mocked and no token is requested:

```properties
%test.quarkus.oidc-client.credentials.secret=test-only-not-a-real-secret
```

This is not a real secret. The real secret is still provided through `REPORTER_CLIENT_SECRET`.

### Bugs Found by the Tests

Writing the tests revealed real bugs, which were then fixed:

| Bug | Found by |
|---|---|
| `POST /api/v1/groups` returned a `Location` header pointing to `/api/v1/contacts/{id}` | `createContactGroup_shouldReturn201WithLocationHeader` |
| `GET /api/v1/contacts/reporter?size=0` returned `500` instead of `400` (this endpoint was later removed) | `getContactsReporter_withInvalidPagination_shouldReturn400` |
| Unknown URLs and malformed JSON returned `500`, because the exception mapper caught framework exceptions | `unknownPath_shouldReturn404`, `createContact_withMalformedJson_shouldReturn400` |

### Testing Design Decisions

* **Mocked unit tests for services:** business rules such as ownership and paging are tested quickly without a database or Quarkus.
* **`@QuarkusTest` for endpoints:** validation, exception mapping, security annotations and the database constraints are only fully tested through the running application.
* **`@TestSecurity` instead of Keycloak in tests:** the tests check the application's authorization rules, not Keycloak itself. The real token flow is tested manually, as described in the service-to-service section.
* **Real MySQL through Dev Services:** uniqueness and foreign keys are enforced by the database, so they are tested against the same database engine used in development.
* **Coverage gate in the build:** the build fails automatically when coverage drops, instead of relying on someone checking a report.

## Configuration

The application uses Quarkus configuration profiles.

The application message differs between development and production:

```properties
app.message=Contacts API
%dev.app.message=Contacts API - DEV
%prod.app.message=Contacts API - PROD
```

This provides a development/production configuration difference without changing the application code.

The database configuration specifies MySQL:

```properties
quarkus.datasource.db-kind=mysql
```

Flyway runs migrations automatically when the application starts:

```properties
quarkus.flyway.migrate-at-start=true
```

Hibernate schema generation is disabled:

```properties
quarkus.hibernate-orm.schema-management.strategy=none
```

Therefore, the database schema is managed through Flyway migrations.

## Design Decisions

### Layered Architecture

The API uses the following structure:

```text
Resource → DTO → Service → Repository → Entity
```

This separates HTTP handling, API data transfer, business logic, and persistence responsibilities.

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

Database schema creation and changes are managed through Flyway migration scripts rather than being automatically generated from entity classes.

Hibernate schema generation is explicitly disabled using:

```properties
quarkus.hibernate-orm.schema-management.strategy=none
```

This makes the database schema version-controlled and reproducible.

### ID Generation

Entity IDs use:

```
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

This matches MySQL `AUTO_INCREMENT` columns and avoids relying on Hibernate-generated sequence tables.

### Dev Services

Quarkus Dev Services is used for the development database.

This provides an automatically managed MySQL container through Docker and avoids requiring developers to manually configure a local database for development.

### Persistence Relationships

The `Contact` entity owns the relationship to `ContactGroup` through a `ManyToOne` association.

A `ContactGroup` can therefore contain multiple contacts, while each contact can optionally reference one group.

The database enforces the relationship through a foreign key.

The association uses `fetch = FetchType.LAZY`. With the default eager fetching, listing contacts loaded each contact's group with an extra query (one query for the list plus one per contact). The API only needs the group ID, which Hibernate can read without loading the group.

### Owner Index

Every query made by an ordinary user filters on `owner`. The `V3__add_contact_owner_index.sql` migration adds an index on this column so these queries do not scan the whole table.

### Validation Location

Bean Validation annotations are placed only on the request DTOs, because input is validated where it enters the API. The entities do not repeat these annotations. The database still enforces `NOT NULL` and `UNIQUE` constraints through the Flyway migrations.

### Column Naming

Column names use the Java field names in camelCase, for example `firstName` and `contactGroup_id`, instead of the more common MySQL `snake_case`. This is a known inconsistency. It was kept on purpose, because renaming would require a migration that renames existing columns plus a Hibernate naming strategy, which carries more risk than benefit at this stage.

### Production Configuration

Development and tests use Dev Services, so no database connection is configured for them. In the `prod` profile, the database URL, username and password are read from the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` environment variables, so no credentials are stored in the repository.

## API Contract

The exported OpenAPI contract is committed to the repository and represents the API contract independently of the implementation.

The contract is updated whenever the API contract changes.

## Git Workflow

We use GitHub Flow for this project.

* `main` contains the stable version of the project.
* New work is developed on a dedicated feature branch.
* Commits should be small and focused.
* Feature branches are pushed to GitHub and merged through pull requests.
* Pull requests are reviewed before merging into `main`.
* After a pull request is merged, the feature branch can be deleted.

We chose GitHub Flow because this project is developed incrementally, with each major task delivered through a dedicated pull request. GitFlow was not chosen because the project does not require long-lived `develop`, release, or hotfix branches. Trunk-based development was also not chosen because the assignment specifically requires working with feature branches and pull requests.

### Workflow Decisions

The `git-workflow` branch was created from a clean and up-to-date `main` branch. During the task, `main` and the feature branch were intentionally given changes to the same part of the README so that a real conflict could be demonstrated.

The conflict was resolved manually during a rebase with `main`. The final version kept the updated content from `main` while preserving the Git Workflow documentation from the feature branch. After resolving the conflict, the rebase was completed and the rebased branch was pushed using `git push --force-with-lease`.

The changes were then submitted through a pull request, reviewed, confirmed to have no conflicts with `main`, and merged.

### Conventional Commits

Earlier commits in the project were created before adopting the Conventional Commits convention, so they do not consistently use conventional prefixes. Starting with the `git-workflow` branch commits, new commits follow the convention where appropriate, such as `docs: document GitHub Flow strategy` and `docs: update API contract wording`.

## Security and Authentication

### Keycloak

Keycloak is used as the identity provider for `contacts-api` and the service-to-service authentication flow. The configured realm is `contacts`.

The API is configured to accept bearer tokens issued by the expected realm. The `contacts-reporter` authenticates to Keycloak as a service rather than as a human user.

### User Authentication and Authorization

The API uses bearer-token authentication and role-based authorization.

* Ordinary users have the `user` role.
* Administrators have the `admin` role.
* The reporter service account has the `read-contacts` client role for `contacts-api`.

The API verifies that a token is valid before allowing access to protected endpoints. Authorization rules determine which operations the authenticated identity may perform.

For contact data, ownership rules restrict ordinary users to their own contacts, while administrators have broader access. The owner is associated with the authenticated identity rather than being trusted as arbitrary client-supplied data.

### Token Validation

The API expects tokens issued by the configured Keycloak realm and intended for the `contacts-api` audience.

The audience configuration is:

```properties
quarkus.oidc.token.audience=contacts-api
```

The `contacts-reporter` client has an Audience mapper configured to include `contacts-api` in its access tokens. A token with the wrong audience is rejected, even if it was issued by Keycloak.

Tokens issued by a different realm are rejected because their issuer does not match the configured realm.

Important JWT claims include:

* `iss` — identifies the issuer that created the token.
* `aud` — identifies the intended audience of the token.
* `exp` — identifies the token's expiration time.
* `sub` — identifies the token subject.
* `azp` — identifies the authorized party/client associated with the token.
* `resource_access` — may contain client-specific roles, including `read-contacts`.

The API must validate the token's signature and relevant claims. Decoding a JWT only displays its contents; decoding alone does not prove that the token is authentic or valid.

### Secret Management

The reporter's client secret is provided through the `REPORTER_CLIENT_SECRET` environment variable instead of being stored in source code.

For example, in PowerShell:

```powershell
$env:REPORTER_CLIENT_SECRET = "YOUR_ACTUAL_CLIENT_SECRET"
```

The placeholder should be replaced locally with the secret configured in Keycloak. The real value must not be committed to Git or included in documentation.

When the client secret is rotated in Keycloak, the environment variable must be updated and the reporter restarted so it uses the new secret. Secret rotation was tested successfully.

## Service-to-Service Authentication

### Client Credentials Grant

The `contacts-reporter` uses the OAuth 2.0 client-credentials grant. This is suitable for backend-to-backend communication because the reporter acts using its own service identity, without a human user logging in.

The reporter is configured as a confidential Keycloak client with client authentication and service accounts enabled. Its service account is assigned the `read-contacts` client role for `contacts-api`.

The reporter's Quarkus configuration includes:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8081/realms/contacts
quarkus.oidc-client.client-id=contacts-reporter
quarkus.oidc-client.credentials.secret=${REPORTER_CLIENT_SECRET}
quarkus.oidc-client.grant.type=client

quarkus.rest-client.contacts-api.url=http://localhost:8080
quarkus.http.port=8082
```

The client secret is supplied by the environment variable and must not be hard-coded into this configuration.

### Token Acquisition and Propagation

The reporter uses Quarkus `OidcClient` configuration to obtain an access token from Keycloak. Its MicroProfile REST client uses `@OidcClientFilter` to attach the bearer token to outgoing API requests.

The flow is:

```text
contacts-reporter
       |
       | client credentials
       v
Keycloak
       |
       | access token
       v
contacts-reporter
       |
       | REST client with Bearer token
       v
contacts-api
       |
       | validate token and role
       v
Reporting response
```

This avoids implementing token retrieval and bearer-header propagation separately for each request.

### Reporting Endpoints

The reporter calls the normal contacts endpoint of the API:

```text
GET /api/v1/contacts?page=0&size=1
```

This endpoint accepts the `user` and `read-contacts` roles. Ordinary users only see their own contacts. Administrators and the reporting service account (`read-contacts`) may read all contacts. The service account receives a role on the existing API rather than a separate private endpoint, so there is only one implementation of listing, paging and validation.

The `read-contacts` role only grants reading the contact list. It cannot create, update, delete or read single contacts, which is covered by tests.

The reporter exposes a derived value:

```text
GET http://localhost:8082/reporters/contacts/count
```

`ContactReporterService` requests a single page of size `1` from the API and returns its `totalElements`, because only the total is needed, not the contacts themselves. The reporter does not have its own database.

The reporter deliberately does not expose the raw contact list. Its own endpoints are not secured, so returning contacts would give every owner's personal data to anyone who can reach the reporter. Only an aggregate number leaves the reporter.

### Failure-Case Testing

The service-to-service flow was tested using both successful requests and invalid configurations.

| Scenario | Expected result | Observed result |
|---|---|---|
| Correct client secret and required role | Reporter returns API data | Successful JSON response |
| Incorrect client secret | Keycloak refuses client authentication | `unauthorized_client` / invalid client credentials |
| Service account lacks `read-contacts` | API refuses authorization | `403 Forbidden` |
| Token has the wrong audience | API rejects the token | `401 Unauthorized` |
| Token is issued by a different realm | API rejects the token | `401 Unauthorized` |
| Expired token | API rejects the token | Expiry check performed |
| Rotated secret is configured in the reporter | Reporter authenticates using the new secret | Successful JSON response after update and restart |

The wrong-role test was performed by temporarily removing `read-contacts` from the service account and then restoring it. The wrong-audience test used a token that did not contain the required `contacts-api` audience. The wrong-issuer test used a temporary realm; that realm and its test client were deleted after testing.

The expiry check verifies that expired access tokens are not accepted. The reporter also needs to obtain a fresh token for subsequent requests rather than relying on an expired token.

### Service to Service Design Decisions

* **Client credentials instead of user login:** the reporter is a backend service and must authenticate when no user is present.
* **Confidential client and service account:** the reporter has its own identity and authenticates to Keycloak using a client secret.
* **Quarkus OIDC client:** token acquisition is delegated to the Quarkus OIDC client implementation.
* **REST client token filter:** `@OidcClientFilter` propagates the service token on outgoing requests.
* **Dedicated reporting role:** `read-contacts` grants the reporter the permissions required for reporting without granting it administrator access.
* **Role on the normal endpoint instead of a separate one:** the service account reads through `GET /api/v1/contacts` with the `read-contacts` role. An earlier version used a separate `/api/v1/contacts/reporter` endpoint; it duplicated the listing logic and validated pagination differently, so it was removed.
* **No raw data from the reporter:** the reporter only returns a derived count, so personal data is not re-exposed through a second service.
* **Audience validation:** the token must be intended for `contacts-api`, not merely validly issued by Keycloak.
* **Issuer validation:** tokens must originate from the configured `contacts` realm.
* **Environment-based secret configuration:** the secret can be changed without editing or committing source code.
* **Failure-path testing:** incorrect credentials, missing roles, wrong audience, wrong issuer, and expiry are included in verification rather than testing only the happy path.

## Running the Reporter

Start Keycloak and `contacts-api` first. Then configure the reporter secret and start the reporter module from the project root.

On Windows PowerShell:

```powershell
$env:REPORTER_CLIENT_SECRET = "YOUR_ACTUAL_CLIENT_SECRET"
.\mvnw.cmd -pl contacts-reporter quarkus:dev
```

The current local development setup uses these ports:

* Keycloak: `8081`
* `contacts-api`: `8080`
* `contacts-reporter`: `8082`

Open the reporter endpoint:

```text
http://localhost:8082/reporters/contacts/count
```

A successful response demonstrates that the reporter can obtain a service token, call the API, and return its data without a user logging in.

## RabbitMQ Messaging

### Architecture

The Contacts Platform uses RabbitMQ to publish contact-created events from `contacts-api` and consume them in `contacts-reporter`.

- **Exchange:** `contacts.events`
- **Exchange type:** `topic`
- **Routing key:** `contact.created`
- **Reporter queue:** `contacts-reporter.contact-created`
- **Dead-letter exchange:** `contacts.events.dlx`
- **Dead-letter queue:** `contacts-reporter.contact-created.dlq`

The topic exchange was chosen because it supports routing by event type and allows future consumers to subscribe to additional contact events using routing-key patterns.

RabbitMQ Dev Services provides a broker automatically during Quarkus development, so a separate broker setup is not required for local development.

### Event payload and tracing

Each `ContactCreatedEvent` contains:

- `eventId` — unique identifier for the event.
- `correlationId` — identifier used to trace the originating action across services.
- `contactId` — identifier of the created contact.
- `occurredAt` — event creation timestamp.

The reporter logs the event ID, correlation ID, and contact ID to make message processing observable and help trace an event across services.

### Exchanges, queues, bindings, and routing keys

- **Exchange:** receives published messages and routes them according to its type and bindings.
- **Queue:** stores messages until they are consumed and acknowledged or rejected.
- **Binding:** connects a queue to an exchange using a routing key or pattern.
- **Routing key:** identifies the event type. The current key is `contact.created`.
- **Topic exchange:** supports pattern-based routing. For example, `contact.*` matches a single word after `contact`, while `contact.#` can match multiple words.

### Work queues and publish/subscribe

In a **work queue**, multiple consumers can compete to process messages from the same queue. Each message is normally handled by one consumer.

In **publish/subscribe**, each subscriber has its own queue bound to the exchange. Each queue can receive a copy of every matching event, allowing independent services to react to the same event.

The topic exchange supports adding more event types and subscribers in the future.

### Acknowledgements and prefetch

Acknowledgements tell RabbitMQ that a message has been processed successfully and can be removed from the queue. If processing fails, the configured `reject` failure strategy rejects the message without requeueing it.

The reporter configures:

`mp.messaging.incoming.contact-created-in.max-outstanding-messages=1`

This limits outstanding messages to one and is used to control the consumer's in-flight workload.

### Consumer failure and redelivery

If a consumer connection is lost while a message remains unacknowledged, RabbitMQ can requeue the message and deliver it again when a consumer becomes available.

This behavior was tested by pausing event processing, stopping `contacts-reporter` before processing completed, and restarting it. The same `eventId` appeared again, and processing subsequently finished successfully.

This demonstrates at-least-once delivery behavior: the same event may be delivered more than once. Consumers should therefore be designed to handle duplicate events safely.

### Bounded retries and dead-letter queue

The reporter implements four application-level processing attempts: one initial attempt and up to three retries. These retries happen immediately within the same delivery; they are not four separate RabbitMQ redeliveries.

If all four attempts fail, the exception propagates to SmallRye Reactive Messaging. The configured `reject` failure strategy rejects the message, and RabbitMQ routes it to the dead-letter queue using the configured dead-letter exchange and routing key.

The dead-letter queue retains failed messages for later inspection or recovery.

### Verification

The following behavior was tested:

- Creating a contact publishes an event that the reporter receives and logs.
- Event IDs and correlation IDs can be used to trace processing.
- A simulated processing failure produces four application-level attempts for the same event ID.
- After the final failed attempt, the message is rejected and appears in the dead-letter queue.
- Two subsequent events are processed successfully while the dead-letter queue count remains unchanged.
- Stopping the reporter during processing and restarting it results in the same event ID being delivered again and successfully processed.

These tests demonstrate event publishing and consumption, routing, failure handling, bounded application-level retries, dead-lettering, and RabbitMQ redelivery after consumer disconnection.

## Event-Driven Architecture

The Contacts Platform uses RabbitMQ and Apache Kafka to publish and consume contact-created events. When a contact is created, `contacts-api` publishes a `ContactCreatedEvent` to both brokers, and `contacts-reporter` consumes the event through the corresponding messaging channels.

This allows the reporter to react to contact creation without making a direct REST call to the API for event notifications.

### Event Payload

The event is represented by `ContactCreatedEvent` and contains:

| Field | Purpose |
|---|---|
| `eventId` | Unique UUID identifying the event and allowing duplicate deliveries to be recognized. |
| `correlationId` | Identifier used to trace the originating action across application logs. |
| `contactId` | ID of the created contact, allowing consumers to identify the affected resource. |
| `occurredAt` | Timestamp indicating when the event occurred. |

The event deliberately contains the contact ID rather than the complete contact entity. The reporter only needs to identify the event and observe its processing; sending the entire entity would expose additional personal data unnecessarily and couple the consumer to the API's entity structure.

### Event Publishing

`ContactEventPublisher` is responsible for publishing the event to both brokers.

- RabbitMQ uses the `contact-created-out` channel.
- Kafka uses the `contact-created-kafka-out` channel.
- Both channels receive the same `ContactCreatedEvent`, including its event ID and correlation ID.

The publisher combines the asynchronous publishing results using `CompletableFuture.allOf()`. This allows the caller to observe whether both publishing operations completed successfully or at least one failed.

The Kafka message key is the contact ID converted to a string. This ensures that messages for the same contact are routed to the same partition under a stable partitioning configuration.

### RabbitMQ

RabbitMQ provides the first event-delivery channel between `contacts-api` and `contacts-reporter`.

The existing RabbitMQ configuration uses a topic exchange and routing keys to route contact-created events to the reporter's queue. The dead-letter configuration and bounded processing retries allow messages that repeatedly fail processing to be moved to a dead-letter queue instead of being retried indefinitely.

The reporter logs the event ID, correlation ID, contact ID, and processing outcome so that message handling is observable.

### Kafka

Kafka is implemented as a second publishing and consumption channel for the same contact-created event.

The outgoing channel is configured as follows:

```properties
mp.messaging.outgoing.contact-created-kafka-out.connector=smallrye-kafka
mp.messaging.outgoing.contact-created-kafka-out.topic=contacts.created
mp.messaging.outgoing.contact-created-kafka-out.value.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer
```

The incoming channel is configured as follows:

```properties
mp.messaging.incoming.contact-created-kafka-in.connector=smallrye-kafka
mp.messaging.incoming.contact-created-kafka-in.topic=contacts.created
```

The publisher adds `OutgoingKafkaRecordMetadata` to each Kafka message and sets the message key to the contact ID.

#### Message Ordering

Kafka guarantees ordering within a partition, not across all partitions in a topic.

Using the contact ID as the key causes messages for the same contact to be assigned to the same partition, provided the partitioning configuration remains compatible. Kafka can then preserve their order within that partition.

If a suitable key were omitted or changed inconsistently, events for the same contact could be assigned to different partitions. Their relative processing order would no longer be guaranteed.

Dev Services creates the `contacts.created` topic with three partitions:

```properties
quarkus.kafka.devservices.topic-partitions."contacts.created"=3
```

With Kafka's default of a single partition, every record would land in partition `0` and stay in order regardless of the key, so the ordering tests would pass even with a missing or wrong key. Three partitions make the key relevant.

The integration tests verify that multiple events for the same contact use the same partition and are received in publish order.

#### At-Least-Once Delivery

Kafka delivers records at least once, so the same event can reach the reporter more than once:

* **Consumer side:** the reporter processes a record and then commits its offset. If the reporter crashes after processing but before the offset is committed, Kafka delivers the record again after the restart.
* **Producer side:** if `contacts-api` does not receive the broker's acknowledgement in time, the producer can retry, and the record may be written twice.

Kafka does not remove these duplicates for the consumer. Duplicates are therefore handled by the reporter, as described in [Consumer Idempotency](#consumer-idempotency).

### Broker Choice

The same event is published to both brokers so that both approaches can be compared. They are suited to different needs:

| | RabbitMQ | Kafka |
|---|---|---|
| Model | Message queue: an exchange routes each message into queues | Log: records are appended to partitions of a topic |
| After consumption | A message is removed from the queue once it is acknowledged | Records stay in the topic for the retention period and can be read again |
| Ordering | Per queue, as long as there is a single consumer | Per partition; the key decides the partition, so per contact |
| Routing | Flexible routing through exchange types, bindings and routing keys | Consumers subscribe to whole topics |
| Failure handling | Built-in acknowledgements, rejection and dead-letter queues | Consumer offsets; retries and dead-letter topics are handled by the application or connector |
| Multiple consumers | Competing consumers on one queue, or one queue per subscriber | Consumer groups; each group receives every record, instances in a group share the partitions |

**RabbitMQ** fits task-style messages that must be processed once and then removed, where routing and dead-lettering of individual failed messages matter.

**Kafka** fits an event stream that several services may read independently, where order per key matters and events should be replayable later.

For the contact-created event, Kafka is the better long-term fit. It is a fact that other services may want to consume independently, order per contact matters, and keeping the events allows a new consumer to replay the history. RabbitMQ remains the simpler choice for the reporter's dead-letter handling, which is why both are demonstrated.

### Consumer Idempotency

Both messaging channels use the same `ContactEventConsumer` processing logic. The consumer tracks processed event IDs and ignores an event when its ID has already been processed.

This is important because messaging systems can deliver the same logical event more than once. A duplicate delivery should not increment the processing count again.

The current implementation uses an in-memory concurrent set:

```
private final Set<UUID> processedEventIds =
        ConcurrentHashMap.newKeySet();

private final AtomicInteger processedEventCount =
        new AtomicInteger(0);
```

The event ID is added to the set before the processing count is incremented. If the ID is already present, the consumer logs the duplicate and returns without incrementing the count.

This provides in-process duplicate suppression for the current demonstration. The set is not persistent, so deduplication state is lost when the application restarts and is not shared between multiple application replicas. A production implementation requiring durable idempotency would use persistent deduplication records and appropriate transaction handling.

### Database Commit and Event Publishing Failure

The database transaction and message publishing are separate operations in the current implementation.

`ContactService.createContact()` is `@Transactional`. It saves the contact, calls `contactEventPublisher.publish(event)` and returns the response. It does not wait for the `CompletionStage` returned by `publish()`, and the transaction commits only after the method returns.

**The database commit succeeds but publishing fails:**

* The contact is saved.
* The caller receives `201 Created`, because the service does not wait for the publishing result.
* The event is lost for the broker that failed. The only trace is the `Failed to publish contact event` error logged by `ContactEventPublisher`, including the event ID.
* The reporter never learns about this contact, and nothing retries the event later.

**The event is published but the commit fails:**

Because `publish()` is called before the transaction commits, the opposite can also happen. The event is sent, then the commit fails, for example because of a database error. The caller receives an error, but the reporter has already received an event for a contact that does not exist.

In both cases the database and the event stream no longer agree, and the caller cannot see it.

For a production system, the Transactional Outbox pattern would be a suitable improvement. The contact and an event record would be stored in the same database transaction. A separate publisher would send pending events to the brokers, retry failures, and record delivery progress. Consumers would still need idempotency because events could be delivered more than once.

### Messaging Tests

The following tests cover event publishing, Kafka ordering, and consumer idempotency.

| Test class | Test | Purpose |
|---|---|---|
| `ContactServiceTest` | `createContact_shouldPublishContactCreatedEventForTheNewContact` | Verifies that contact creation publishes an event with the generated contact ID and a non-null event ID. |
| `ContactEventPublisherTest` | `publish_shouldSendEventToRabbitMQAndKafka` | Verifies that the publisher sends the event to both messaging channels. |
| `ContactEventPublisherTest` | `publish_shouldUseContactIdAsKafkaKey` | Verifies that the contact ID is used as the Kafka message key. |
| `ContactEventPublisherKafkaTest` | `publish_shouldDeliverEventsOfTheSameContactToTheSamePartition` | Verifies that messages for the same contact use the same partition. |
| `ContactEventPublisherKafkaTest` | `publish_shouldDeliverEventsOfTheSameContactInPublishOrder` | Verifies that events are received in publish order. |
| `ContactEventConsumerTest` | `consumeFromKafka_shouldIgnoreDuplicateEvent` | Verifies that duplicate Kafka deliveries do not increase the processing count twice. |
| `ContactEventConsumerTest` | `consume_shouldIgnoreDuplicateRabbitMQEvent` | Verifies duplicate suppression for RabbitMQ deliveries. |
| `ContactEventConsumerTest` | `consumeFromRabbitMQAndKafka_shouldProcessSameEventOnlyOnce` | Verifies that the same event delivered through both brokers is processed once within the consumer instance. |
| `ContactEventConsumerTest` | `consumeFromKafka_shouldProcessDifferentEventsForSameContact` | Verifies that different events for the same contact are both processed, so deduplication uses the event ID and not the contact ID. |
| `ContactEventConsumerTest` | `consumeFromKafka_shouldNotThrowForDuplicateEvent` | Verifies that a duplicate is skipped without an exception, so it is acknowledged normally instead of being retried or dead-lettered. |

#### Contact Creation Event Test

The service unit test `createContact_shouldPublishContactCreatedEventForTheNewContact` verifies the connection between contact creation and event publishing.

The test prepares a `ContactRequest`, mocks the authenticated user's subject, and configures the repository to assign an ID to the newly persisted contact. It then calls `createContact()` and captures the event passed to `ContactEventPublisher`.

The assertions verify that:

- The publisher is invoked.
- The event contains the generated contact ID.
- The event has a non-null event ID.

This verifies that the service constructs and publishes an event for the newly created contact. It complements the publisher and consumer tests, which test the messaging behavior separately.

#### Kafka Integration Testing

`ContactEventPublisherKafkaTest` uses `@QuarkusTest` and `KafkaCompanionResource` to publish events to the actual Kafka broker used by the test environment and inspect the records received from the `contacts.created` topic.

The test verifies partition assignment and event order using the contact ID as the message key.

These tests provide integration-level evidence for Kafka publishing and ordering, while the consumer unit tests cover duplicate suppression. A separate end-to-end test that observes the reporter's Kafka consumer would provide additional evidence that the complete cross-application flow works.

### Messaging Design Decisions

- **Small event payload:** the event carries identifiers and a timestamp rather than the full contact entity, minimizing unnecessary data exposure and coupling.
- **Event ID:** a UUID identifies a logical event independently of its delivery attempts.
- **Correlation ID:** supports tracing a user action across the API and reporter logs.
- **Same event on both brokers:** RabbitMQ and Kafka receive the same event data, allowing both messaging approaches to be demonstrated.
- **Contact ID as Kafka key:** preserves per-contact ordering within a partition.
- **Shared consumer deduplication logic:** both brokers use the same event-processing method, avoiding separate idempotency rules.
- **In-memory deduplication for the demonstration:** keeps the implementation simple but does not provide durable or distributed deduplication.

# CI/CD Documentation

## 1. GitHub Actions CI Pipeline

Created `.github/workflows/ci.yml` to automate the build, test, and Docker image verification process.

The workflow runs on:
- Pushes to `main`
- Pull requests targeting `main`

The pipeline:
1. Checks out the repository.
2. Sets up Java 21 using Temurin.
3. Caches Maven dependencies.
4. Runs `mvn --batch-mode --no-transfer-progress verify`.
5. Builds the `contacts-api` Docker image.
6. Builds the `contacts-reporter` Docker image.

## 2. Automated Testing and Coverage

Configured JaCoCo in both Quarkus modules to enforce a minimum of **80% line coverage** for the selected service and resource packages.

The coverage check runs during Maven's `verify` phase. Both modules passed their tests and coverage checks. We also tested the enforcement mechanism by temporarily setting an unattainable threshold and confirming that the build failed.

## 3. Branch Protection and Merge Gate

Configured branch protection for `main` to require:
- Pull requests before merging.
- The `Build and test` GitHub Actions status check to pass.

Verified the merge gate by creating a temporary pull request containing an intentionally failing test. CI failed as expected. The test was removed, CI passed again, and the temporary pull request was closed without merging.

## 4. Docker Image Verification

The CI workflow builds both application images using:
- `Dockerfile.api`
- `Dockerfile.reporter`

Both image builds completed successfully in GitHub Actions.

## 5. Docker Compose Clean-Start Verification

Stopped and rebuilt the Docker Compose stack using:

```powershell
docker compose -f DockerCompose.yaml down
docker compose -f DockerCompose.yaml up -d --build
docker compose -f DockerCompose.yaml ps
```

All six services started successfully:
- `contacts-api`
- `contacts-reporter`
- MySQL
- Keycloak
- RabbitMQ
- Kafka

MySQL, Keycloak, RabbitMQ, and Kafka reported healthy status. The named data volumes were preserved by not using the `-v` option with `docker compose down`.

## 6. Runtime Verification

- Confirmed that the Contacts API started in production mode.
- Confirmed that all three Flyway migrations were validated and the database schema was up to date.
- Confirmed that the API connected to Kafka and RabbitMQ.
- Opened `/q/openapi` successfully and verified that the API specification lists the expected contacts and contact-group endpoints.
- Verified the reporter-to-API request using `/reporters/contacts/count`.

## 7. Service-to-Service Authentication Troubleshooting

The reporter initially returned an HTTP 500 error because Keycloak rejected its client credentials with `unauthorized_client`.

The issue was traced to a mismatch between `REPORTER_CLIENT_SECRET` in the local `.env` file and the secret configured for the reporter's Keycloak client.

Updated the local environment variable to match the Keycloak client secret and recreated the reporter container. The reporter then successfully authenticated and called the Contacts API, returning a count of `0`.

## 8. Secret and Git Hygiene

Verified that `.env` is ignored by Git using:

```powershell
git check-ignore .env
```

The command returned `.env`, confirming that the ignore rule applies. Also confirmed that `git status --short` returned no changes on the current branch.

**Security note:** The ignore rule prevents normal untracked-file additions, but it does not prove that `.env` was never committed in the past. If needed, check the Git history separately.

## Final Result

CI pipeline, coverage enforcement, Docker image builds, branch protection, merge-gate behavior, and Docker Compose clean-start verification were completed successfully. The reporter's Keycloak client-credentials authentication was also restored and verified at runtime.