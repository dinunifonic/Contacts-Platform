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
| GET | `/api/v1/contacts/reporter` | Return paginated contacts for authorized reporting clients |
| GET | `/reporters/contacts` | Return reporting data through `contacts-reporter` |
| GET | `/reporters/contacts/count` | Return the total number of contacts through `contacts-reporter` |

The reporter endpoint calls the API reporting endpoint and returns its response. It does not query the database directly.

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
| Unexpected server error | `500 Internal Server Error` |

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
| `contacts-api` | `ContactServiceTest` | Unit | Contact business logic, owner rules, admin access, paging maths, missing groups |
| `contacts-api` | `ContactGroupServiceTest` | Unit | Group business logic, paging maths, unassigning contacts before deleting a group |
| `contacts-api` | `ContactResourceTest` | Integration | Contact endpoints: happy path, invalid input, not found, duplicate, security, reporter endpoint |
| `contacts-api` | `ContactGroupResourceTest` | Integration | Group endpoints: happy path, invalid input, not found, duplicate, security, contacts of a group |
| `contacts-reporter` | `ContactReporterServiceTest` | Unit | Total contacts derived from the API response |
| `contacts-reporter` | `ContactReporterResourceTest` | Integration | Reporter endpoints with a mocked `ContactsApiClient` |

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
| `user` calling the reporter endpoint | `403 Forbidden` |
| `read-contacts` calling the reporter endpoint | `200 OK` |

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
| `GET /api/v1/contacts/reporter?size=0` returned `500` instead of `400` | `getContactsReporter_withInvalidPagination_shouldReturn400` |

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

The API exposes a dedicated endpoint for authorized reporting access:

```text
GET /api/v1/contacts/reporter?page=0&size=20
```

It requires the `read-contacts` role. Keeping reporting access on a separate endpoint avoids weakening the existing user-ownership rules for ordinary contact endpoints.

The reporter exposes:

```text
GET http://localhost:8082/reporters/contacts
```

It calls the API and returns the paginated response. The reporter does not have its own database.

The reporter also exposes a derived value:

```text
GET http://localhost:8082/reporters/contacts/count
```

`ContactReporterService` requests a single page of size `1` from the API and returns its `totalElements`, because only the total is needed, not the contacts themselves.

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
* **Separate reporting endpoint:** reporting access is isolated from normal user-owned contact operations.
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
http://localhost:8082/reporters/contacts
```

A successful response demonstrates that the reporter can obtain a service token, call the API, and return its data without a user logging in.
