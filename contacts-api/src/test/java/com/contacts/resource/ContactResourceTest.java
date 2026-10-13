package com.contacts.resource;

import com.contacts.entity.Contact;
import com.contacts.repository.ContactRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class ContactResourceTest {
    @Inject
    ContactRepository contactRepository;

    @Test
    @TestSecurity(user = "empty-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "empty-user")
    })
    void getAllContacts_shouldReturn200() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    void getAllContacts_withoutAuthentication_shouldReturn401() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "paging-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "paging-user")
    })
    void getAllContacts_secondPage_shouldReturnRemainingContactAndCorrectTotals() {
        createContact("paging1@test.com");
        createContact("paging2@test.com");
        createContact("paging3@test.com");

        RestAssured.given()
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("page", equalTo(1))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2));
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getAllContacts_withInvalidPagination_shouldReturn400(int page, int size) {
        RestAssured.given()
                .queryParam("page", page)
                .queryParam("size", size)
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getAllContacts_withNonExistingGroup_shouldReturn404() {
        RestAssured.given()
                .queryParam("groupId", 999999)
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "user-b", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "user-b")
    })
    void getAllContacts_shouldNotReturnOtherUsersContacts() {
        persistContactOwnedBy("user-a", "owned-by-a-list@test.com");

        RestAssured.given()
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContact_shouldReturn201AndPersistContact() {
        String email = "create@test.com";
        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {
                      "firstName": "Dina",
                      "lastName": "Ismail",
                      "email": "create@test.com",
                      "phone": "01116787399"
                    }
                    """)
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(201);

        RestAssured.given()
                .when()
                .get("/api/v1/contacts")
                .then()
                .statusCode(200)
                .body("content.find { it.email == '%s' }.firstName"
                        .formatted(email), equalTo("Dina"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContact_withInvalidEmail_shouldReturn400() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {
                      "firstName": "Dina",
                      "lastName": "Ismail",
                      "email": "not-an-email",
                      "phone": "01116787399"
                    }
                    """)
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("title", equalTo("Validation Failed"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContact_withMissingFirstName_shouldReturn400() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {
                      "lastName": "Ismail",
                      "email": "nofirstname@test.com",
                      "phone": "01116787399"
                    }
                    """)
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContact_withDuplicateEmail_shouldReturn409() {
        createContact("duplicate@test.com");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {
                      "firstName": "Donia",
                      "lastName": "Ismail",
                      "email": "duplicate@test.com",
                      "phone": "01116787300"
                    }
                    """)
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(409)
                .contentType("application/problem+json");
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContact_withNonExistingGroup_shouldReturn404() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                    {
                      "firstName": "Dina",
                      "lastName": "Ismail",
                      "email": "nogroup@test.com",
                      "phone": "01116787399",
                      "groupId": 999999
                    }
                    """)
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactById_shouldReturnContact() {
        String email = "getbyid@test.com";
        Integer contactId = createContact(email);

        RestAssured.given()
                .when()
                .get("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contactId))
                .body("firstName", equalTo("Dina"))
                .body("lastName", equalTo("Ismail"))
                .body("email", equalTo(email));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactById_whenContactDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts/999999")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("title", equalTo("Not Found"));
    }

    @Test
    @TestSecurity(user = "user-b", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "user-b")
    })
    void getContactById_ofAnotherUser_shouldReturn404() {
        Long contactId = persistContactOwnedBy("user-a", "owned-by-a@test.com");

        RestAssured.given()
                .when()
                .get("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "admin"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-user")
    })
    void getContactById_ofAnotherUser_asAdmin_shouldReturn200() {
        Long contactId = persistContactOwnedBy("user-a", "admin-can-see@test.com");

        RestAssured.given()
                .when()
                .get("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContact_shouldReturnUpdatedContact() {
        String email = "update@test.com";
        Integer contactId = createContact(email);

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "firstName": "Donia",
                  "lastName": "Ismail",
                  "email": "update@test.com",
                  "phone": "01216787399"
                }
                """)
                .when()
                .put("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(200)
                .body("id", equalTo(contactId))
                .body("firstName", equalTo("Donia"))
                .body("lastName", equalTo("Ismail"))
                .body("phone", equalTo("01216787399"))
                .body("email", equalTo(email));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContact_whenContactDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "firstName": "Donia",
                  "lastName": "Ismail",
                  "email": "update404@test.com",
                  "phone": "01216787399"
                }
                """)
                .when()
                .put("/api/v1/contacts/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContact_withInvalidEmail_shouldReturn400() {
        Integer contactId = createContact("update400@test.com");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "firstName": "Donia",
                  "lastName": "Ismail",
                  "email": "not-an-email",
                  "phone": "01216787399"
                }
                """)
                .when()
                .put("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void deleteContact_asRegularUser_shouldReturn403() {
        RestAssured.given()
                .when()
                .delete("/api/v1/contacts/1")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "admin"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-user")
    })
    void deleteContact_asAdmin_shouldReturn204AndDeleteContact() {
        Long contactId = persistContactOwnedBy("another-user", "delete@test.com");

        RestAssured.given()
                .when()
                .delete("/api/v1/contacts/" + contactId)
                .then()
                .statusCode(204);

        Contact deleted = QuarkusTransaction.requiringNew()
                .call(() -> contactRepository.findById(contactId));
        assertThat(deleted).isNull();
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "admin"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-user")
    })
    void deleteContact_whenContactDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .when()
                .delete("/api/v1/contacts/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void getContactsReporter_withoutAuthentication_shouldReturn401() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts/reporter")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactsReporter_asRegularUser_shouldReturn403() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts/reporter")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "service-account-contacts-reporter", roles = {"read-contacts"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "service-account-contacts-reporter")
    })
    void getContactsReporter_withReadContactsRole_shouldReturn200() {
        RestAssured.given()
                .when()
                .get("/api/v1/contacts/reporter")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "service-account-contacts-reporter", roles = {"read-contacts"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "service-account-contacts-reporter")
    })
    void getContactsReporter_withInvalidPagination_shouldReturn400() {
        RestAssured.given()
                .queryParam("size", 0)
                .when()
                .get("/api/v1/contacts/reporter")
                .then()
                .statusCode(400);
    }

    private Integer createContact(String email) {
        return RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "firstName": "Dina",
                  "lastName": "Ismail",
                  "email": "%s",
                  "phone": "01116787399"
                }
                """.formatted(email))
                .when()
                .post("/api/v1/contacts")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Long persistContactOwnedBy(String owner, String email) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Contact contact = new Contact();
            contact.setFirstName("Dina");
            contact.setLastName("Ismail");
            contact.setEmail(email);
            contact.setPhone("01116787399");
            contact.setOwner(owner);
            contactRepository.persist(contact);
            return contact.getId();
        });
    }
}