package com.contacts.resource;

import com.contacts.dto.ContactRequest;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.service.ContactService;
import jakarta.ws.rs.*;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("/api/v1/contacts")
public class ContactResource {
    @Inject
    ContactService contactService;

    @Inject
    PaginationValidator paginationValidator;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactResponse createContact(@Valid ContactRequest contactRequest){
        return contactService.createContact(contactRequest);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContactResponse getContactById(@PathParam("id") Long id){
        return contactService.getContactById(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactResponse> getAllContacts(@QueryParam("page") @DefaultValue("0") int page,
                                                             @QueryParam("size") @DefaultValue("20") int size,
                                                             @QueryParam("groupId") Long groupId){
        paginationValidator.validate(page, size);
        return contactService.getAllContacts(page, size, groupId);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactResponse updateContact(@PathParam("id") Long id, @Valid ContactRequest contactRequest){
        return contactService.updateContact(id, contactRequest);
    }

    @DELETE
    @Path("/{id}")
    public void deleteContact(@PathParam("id") Long id){
        contactService.deleteContact(id);
    }



}
