package com.contacts.resource;

import com.contacts.client.ContactsApiClient;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ContactReporterResourceTest {
    @InjectMock
    @RestClient
    ContactsApiClient contactsApiClient;

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

    @Test
    void getContacts_rawListIsNotExposed_shouldReturn404() {
        RestAssured.given()
                .when()
                .get("/reporters/contacts")
                .then()
                .statusCode(404);
    }
}
