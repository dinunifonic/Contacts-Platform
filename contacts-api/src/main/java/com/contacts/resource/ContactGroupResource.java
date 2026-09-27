package com.contacts.resource;

import com.contacts.dto.ContactResponse;
import com.contacts.service.ContactService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/groups")
public class ContactGroupResource {
    @Inject
    ContactService contactService;

    @GET
    @Path("/{groupId}/contacts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ContactResponse> getContactsByGroupId(@PathParam("groupId") Long groupId){
        return contactService.getContactsByGroup(groupId);
    }

}
