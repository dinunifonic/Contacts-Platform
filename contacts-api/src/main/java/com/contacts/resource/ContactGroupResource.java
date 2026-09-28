package com.contacts.resource;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.service.ContactGroupService;
import com.contacts.service.ContactService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;


@Path("/api/v1/groups")
public class ContactGroupResource {
    @Inject
    ContactService contactService;

    @Inject
    ContactGroupService contactGroupService;

    @Inject
    PaginationValidator paginationValidator;

    @GET
    @Path("/{groupId}/contacts")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactResponse> getContactsByGroupId(@QueryParam("page")@DefaultValue("0") int page,
                                                                   @QueryParam("size")@DefaultValue("20") int size,
                                                                   @PathParam("groupId") Long groupId){
        paginationValidator.validate(page, size);
        return contactService.getContactsByGroup(page, size, groupId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactGroupResponse createContactGroup(@Valid ContactGroupRequest contactGroupRequest){
        return contactGroupService.createContactGroup(contactGroupRequest);
    }

    @GET
    @Path("/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContactGroupResponse getContactGroupById(@PathParam("groupId") Long id){
        return contactGroupService.getContactGroupById(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactGroupResponse> getAllContactGroups(@QueryParam("page") @DefaultValue("0") int page,
                                                                       @QueryParam("size") @DefaultValue("20") int size,
                                                                       @QueryParam("name") String name){
        paginationValidator.validate(page, size);
        return contactGroupService.getAllContactGroups(page, size, name);
    }

    @PUT
    @Path("/{groupId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactGroupResponse updateContactGroup(@PathParam("groupId") Long id, @Valid ContactGroupRequest contactGroupRequest){
        return contactGroupService.updateContactGroup(id, contactGroupRequest);
    }

    @DELETE
    @Path("/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteContactGroup(@PathParam("groupId") Long id){
        contactGroupService.deleteContactGroup(id);
    }

}
