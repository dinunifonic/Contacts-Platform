package com.contacts.resource;

import com.contacts.service.ContactReporterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/reporters/contacts")
public class ContactReporterResource {
    @Inject
    ContactReporterService contactReporterService;

    @GET
    @Path("/count")
    public long getTotalContacts() {
        return contactReporterService.getTotalContacts();
    }

}
