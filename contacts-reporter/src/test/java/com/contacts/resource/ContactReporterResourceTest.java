package com.contacts.resource;

import com.contacts.client.ContactsApiClient;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ContactReporterResourceTest {
    @InjectMock
    @RestClient
    ContactsApiClient contactsApiClient;

    @Test
    void getContactsReporter_shouldReturnContactsFromContactsApi() {
        ContactResponse contact = new ContactResponse();
        contact.setFirstName("Dina");
        contact.setEmail("dina@test.com");
        PaginatedResponse<ContactResponse> response = new PaginatedResponse<>();
        response.setContent(List.of(contact));
        response.setTotalElements(1);
        when(contactsApiClient.getContacts(0, 20)).thenReturn(response);

        RestAssured.given()
                .when()
                .get("/reporters/contacts")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].email", equalTo("dina@test.com"));
    }

    @Test
    void getTotalContacts_shouldReturnTotalFromContactsApi() {
        PaginatedResponse<ContactResponse> response = new PaginatedResponse<>();
        response.setTotalElements(42);
        when(contactsApiClient.getContacts(0, 1)).thenReturn(response);

        RestAssured.given()
                .when()
                .get("/reporters/contacts/count")
                .then()
                .statusCode(200)
                .body(equalTo("42"));
    }
}
