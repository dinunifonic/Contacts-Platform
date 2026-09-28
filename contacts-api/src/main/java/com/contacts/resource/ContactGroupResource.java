package com.contacts.resource;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.dto.ContactResponse;
import com.contacts.service.ContactGroupService;
import com.contacts.service.ContactService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/groups")
public class ContactGroupResource {
    @Inject
    ContactService contactService;

    @Inject
    ContactGroupService contactGroupService;

    @GET
    @Path("/{groupId}/contacts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ContactResponse> getContactsByGroupId(@PathParam("groupId") Long groupId){
        return contactService.getContactsByGroup(groupId);
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
    public List<ContactGroupResponse> getAllContactGroups(){
        return contactGroupService.getAllContactGroups();
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
