package com.contacts.resource;

import com.contacts.client.ContactsApiClient;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/reporters/contacts")
public class ContactReporterResource {
    @Inject
    @RestClient
    ContactsApiClient contactsApiClient;

    @GET
    public PaginatedResponse<ContactResponse> getContactsReporter() {
        return contactsApiClient.getContacts(0, 20);
    }

}
