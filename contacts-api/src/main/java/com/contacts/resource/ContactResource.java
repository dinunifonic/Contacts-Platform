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
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.List;

@Path("/api/v1/contacts")
public class ContactResource {
    @Inject
    ContactService contactService;

    @Inject
    PaginationValidator paginationValidator;

    @Operation(
            summary = "Create a contact",
            description = "Creates a new contact."
    )
    @APIResponses({
            @APIResponse( responseCode = "201", description = "Contact created successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid request" ),
            @APIResponse( responseCode = "409", description = "A contact with the same email already exists" )
    })
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContact(@Valid ContactRequest contactRequest){
        ContactResponse contactResponse=contactService.createContact(contactRequest);
        return Response.status(Response.Status.CREATED)
                .header("Location", "/api/v1/contacts/" + contactResponse.getId())
                .entity(contactResponse)
                .build();
    }

    @Operation(
            summary = "Get a contact by ID",
            description = "Returns a contact by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contact found :)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactResponse.class))
            ),
            @APIResponse( responseCode = "404", description = "Contact not found :(" )
    })
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContactResponse getContactById(@Parameter(description = "Unique identifier of the contact", example = "1")@PathParam("id") Long id){
        return contactService.getContactById(id);
    }

    @Operation(
            summary = "Get all contacts",
            description = "Returns a paginated list of contacts, optionally filtered by group ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contacts retrieved successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PaginatedResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid pagination parameters" )
    })
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<ContactResponse> getAllContacts(@Parameter(description = "Zero-based page number", example = "0")  @QueryParam("page") @DefaultValue("0") int page,
                                                             @Parameter(description = "Number of contacts per page. Must be between 1 and 100", example = "20") @QueryParam("size") @DefaultValue("20") int size,
                                                             @Parameter(description = "Filter contacts by contact group ID", example = "1") @QueryParam("groupId") Long groupId){
        paginationValidator.validate(page, size);
        return contactService.getAllContacts(page, size, groupId);
    }

    @Operation(
            summary = "Update a contact",
            description = "Updates an existing contact by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "200", description = "Contact updated successfully!",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ContactResponse.class))
            ),
            @APIResponse( responseCode = "400", description = "Invalid request" ),
            @APIResponse( responseCode = "404", description = "Contact not found :(" ),
            @APIResponse( responseCode = "409", description = "A contact with the same email already exists" )
    })
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ContactResponse updateContact(@Parameter(description = "Unique identifier of the contact", example = "1") @PathParam("id") Long id, @Valid ContactRequest contactRequest){
        return contactService.updateContact(id, contactRequest);
    }

    @Operation(
            summary = "Delete a contact",
            description = "Deletes an existing contact by its ID."
    )
    @APIResponses({
            @APIResponse( responseCode = "204", description = "Contact deleted successfully!" ),
            @APIResponse( responseCode = "404", description = "Contact not found :(" )
    })
    @DELETE
    @Path("/{id}")
    public Response deleteContact(@PathParam("id") Long id){
        contactService.deleteContact(id);
        return Response.noContent().build();
    }



}
