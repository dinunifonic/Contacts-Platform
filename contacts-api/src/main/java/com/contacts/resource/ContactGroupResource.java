package com.contacts.resource;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.service.ContactGroupService;
import com.contacts.service.ContactService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;


@Path("/api/v1/groups")
@Authenticated
@SecurityRequirement(name = "keycloak")
public class ContactGroupResource {
    @Inject
    ContactService contactService;

    @Inject
    ContactGroupService contactGroupService;

    @Inject
    PaginationValidator paginationValidator;

    @Operation(
            summary = "Get contacts in a group",
            description = "Returns a paginated list of contacts belonging to a specific group."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contacts retrieved successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PaginatedResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid pagination parameters"),
            @APIResponse( responseCode = "404", description = "Contact group not found :(" )
    })
    @GET
    @Path("/{groupId}/contacts")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactResponse> getContactsByGroup(@Parameter(description = "Zero-based page number", example = "0") @QueryParam("page")@DefaultValue("0") int page,
                                                                 @Parameter(description = "Number of contacts per page. Must be between 1 and 100", example = "20") @QueryParam("size")@DefaultValue("20") int size,
                                                                 @Parameter(description = "Unique identifier of the contact group", example = "1") @PathParam("groupId") Long groupId){
        paginationValidator.validate(page, size);
        return contactService.getContactsByGroup(page, size, groupId);
    }

    @Operation(
            summary = "Create a contact group",
            description = "Creates a new contact group."
    )
    @APIResponses({
            @APIResponse( responseCode = "201", description = "Contact group created successfully!",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactGroupResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid request" ),
            @APIResponse( responseCode = "409", description = "A group with the same name already exists" )
    })
    @POST
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContactGroup(@Valid ContactGroupRequest contactGroupRequest){
        ContactGroupResponse contactGroupResponse= contactGroupService.createContactGroup(contactGroupRequest);
        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/contacts/" + contactGroupResponse.getId())
                .entity(contactGroupResponse)
                .build();
    }

    @Operation(
            summary = "Get a group by ID",
            description = "Returns a single contact group by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contact group found :)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactGroupResponse.class))
            ),
            @APIResponse( responseCode = "404", description = "Contact group not found :(" )
    })
    @GET
    @Path("/{groupId}")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public ContactGroupResponse getContactGroupById(@Parameter(description = "Unique identifier of the contact group", example = "1") @PathParam("groupId") Long id){
        return contactGroupService.getContactGroupById(id);
    }

    @Operation(
            summary = "Get contact groups",
            description = "Returns a paginated list of contact groups, optionally filtered by name."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contact groups retrieved successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PaginatedResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid pagination parameters" )
    })
    @GET
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactGroupResponse> getAllContactGroups(@Parameter(description = "Zero-based page number", example = "0") @QueryParam("page")@DefaultValue("0") int page,
                                                                       @Parameter(description = "Number of contacts per page. Must be between 1 and 100", example = "20") @QueryParam("size")@DefaultValue("20") int size,
                                                                       @Parameter(description = "Filter groups by exact group name", example = "Friends") @QueryParam("name") String name){
        paginationValidator.validate(page, size);
        return contactGroupService.getAllContactGroups(page, size, name);
    }

    @Operation(
            summary = "Update a contact group",
            description = "Updates an existing contact group by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contact group updated successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactGroupResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid request" ),
            @APIResponse( responseCode = "404", description = "Contact group not found :(" ),
            @APIResponse( responseCode = "409", description = "A group with the same name already exists" )
    })
    @PUT
    @Path("/{groupId}")
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactGroupResponse updateContactGroup(@Parameter(description = "Unique identifier of the contact group", example = "1") @PathParam("groupId") Long id, @Valid ContactGroupRequest contactGroupRequest){
        return contactGroupService.updateContactGroup(id, contactGroupRequest);
    }

    @Operation(
            summary = "Delete a contact group",
            description = "Deletes an existing contact group by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "204", description = "Contact group deleted successfully!" ),
            @APIResponse( responseCode = "404", description = "Contact group not found :(" )
    })
    @DELETE
    @Path("/{groupId}")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContactGroup(@Parameter(description = "Unique identifier of the contact group", example = "1") @PathParam("groupId") Long id){
        contactGroupService.deleteContactGroup(id);
        return Response.noContent().build();
    }

}
