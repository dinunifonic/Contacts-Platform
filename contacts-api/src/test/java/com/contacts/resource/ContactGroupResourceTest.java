package com.contacts.resource;

import com.contacts.entity.Contact;
import com.contacts.repository.ContactGroupRepository;
import com.contacts.repository.ContactRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class ContactGroupResourceTest {
    @Inject
    ContactRepository contactRepository;

    @Inject
    ContactGroupRepository contactGroupRepository;

    @Test
    void getAllContactGroups_withoutAuthentication_shouldReturn401() {
        RestAssured.given()
                .when()
                .get("/api/v1/groups")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContactGroup_shouldReturn201WithLocationHeader() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "Friends",
                  "description": "My friends"
                }
                """)
                .when()
                .post("/api/v1/groups")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/groups/"))
                .body("name", equalTo("Friends"))
                .body("description", equalTo("My friends"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContactGroup_withBlankName_shouldReturn400() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "",
                  "description": "No name"
                }
                """)
                .when()
                .post("/api/v1/groups")
                .then()
                .statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void createContactGroup_withDuplicateName_shouldReturn409() {
        createGroup("Duplicate Group");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "Duplicate Group",
                  "description": "Same name again"
                }
                """)
                .when()
                .post("/api/v1/groups")
                .then()
                .statusCode(409)
                .contentType("application/problem+json");
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactGroupById_shouldReturnGroup() {
        Integer groupId = createGroup("Get By Id Group");

        RestAssured.given()
                .when()
                .get("/api/v1/groups/" + groupId)
                .then()
                .statusCode(200)
                .body("id", equalTo(groupId))
                .body("name", equalTo("Get By Id Group"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactGroupById_whenGroupDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .when()
                .get("/api/v1/groups/999999")
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getAllContactGroups_filteredByName_shouldReturnOnlyMatchingGroup() {
        createGroup("Family");
        createGroup("People");

        RestAssured.given()
                .queryParam("name", "Family")
                .when()
                .get("/api/v1/groups")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].name", equalTo("Family"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getAllContactGroups_withInvalidPageSize_shouldReturn400() {
        RestAssured.given()
                .queryParam("page", 0)
                .queryParam("size", 0)
                .when()
                .get("/api/v1/groups")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContactGroup_shouldReturnUpdatedGroup() {
        Integer groupId = createGroup("Bestie");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "BestFriends",
                  "description": "Updated description"
                }
                """)
                .when()
                .put("/api/v1/groups/" + groupId)
                .then()
                .statusCode(200)
                .body("id", equalTo(groupId))
                .body("name", equalTo("BestFriends"))
                .body("description", equalTo("Updated description"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContactGroup_whenGroupDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "Missing Group",
                  "description": "Does not exist"
                }
                """)
                .when()
                .put("/api/v1/groups/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContactGroup_withBlankName_shouldReturn400() {
        Integer groupId = createGroup("Update Invalid Group");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "",
                  "description": "No name"
                }
                """)
                .when()
                .put("/api/v1/groups/" + groupId)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void updateContactGroup_toAnExistingName_shouldReturn409() {
        createGroup("Taken Name Group");
        Integer groupId = createGroup("Rename Me Group");

        RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "Taken Name Group",
                  "description": "Clashes with another group"
                }
                """)
                .when()
                .put("/api/v1/groups/" + groupId)
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void deleteContactGroup_asRegularUser_shouldReturn403() {
        Integer groupId = createGroup("Forbidden Delete Group");

        RestAssured.given()
                .when()
                .delete("/api/v1/groups/" + groupId)
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "admin"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-user")
    })
    void deleteContactGroup_asAdmin_shouldReturn204AndKeepItsContactsWithoutGroup() {
        Integer groupId = createGroup("Delete Group");
        Long contactId = persistContactInGroup("another-user", "in.deleted.group@test.com", groupId);

        RestAssured.given()
                .when()
                .delete("/api/v1/groups/" + groupId)
                .then()
                .statusCode(204);

        Contact contact = QuarkusTransaction.requiringNew()
                .call(() -> contactRepository.findById(contactId));
        assertThat(contact).isNotNull();
        assertThat(contact.getContactGroup()).isNull();
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "admin"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "admin-user")
    })
    void deleteContactGroup_whenGroupDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .when()
                .delete("/api/v1/groups/999999")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "group-reader", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "group-reader")
    })
    void getContactsByGroup_shouldReturnOnlyCurrentUsersContactsInGroup() {
        Integer groupId = createGroup("Shared Group");
        persistContactInGroup("group-reader", "mine.in.group@test.com", groupId);
        persistContactInGroup("someone-else", "theirs.in.group@test.com", groupId);

        RestAssured.given()
                .when()
                .get("/api/v1/groups/" + groupId + "/contacts")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].email", equalTo("mine.in.group@test.com"));
    }

    @Test
    @TestSecurity(user = "test-user", roles = {"user"})
    @OidcSecurity(claims = {
            @Claim(key = "sub", value = "test-user")
    })
    void getContactsByGroup_whenGroupDoesNotExist_shouldReturn404() {
        RestAssured.given()
                .when()
                .get("/api/v1/groups/999999/contacts")
                .then()
                .statusCode(404);
    }

    private Integer createGroup(String name) {
        return RestAssured.given()
                .contentType("application/json")
                .body("""
                {
                  "name": "%s",
                  "description": "Test group"
                }
                """.formatted(name))
                .when()
                .post("/api/v1/groups")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Long persistContactInGroup(String owner, String email, Integer groupId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Contact contact = new Contact();
            contact.setFirstName("Dina");
            contact.setLastName("Ismail");
            contact.setEmail(email);
            contact.setPhone("01116787399");
            contact.setOwner(owner);
            contact.setContactGroup(contactGroupRepository.findById(groupId.longValue()));
            contactRepository.persist(contact);
            return contact.getId();
        });
    }
}