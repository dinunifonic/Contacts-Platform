package com.contacts.resource;

import com.contacts.client.ContactsApiClient;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.service.ContactReporterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/reporters/contacts")
public class ContactReporterResource {
    @Inject
    @RestClient
    ContactsApiClient contactsApiClient;

    @Inject
    ContactReporterService contactReporterService;

    @GET
    public PaginatedResponse<ContactResponse> getContactsReporter() {
        return contactsApiClient.getContacts(0, 20);
    }

    @GET
    @Path("/count")
    public long getTotalContacts() {
        return contactReporterService.getTotalContacts();
    }

}
