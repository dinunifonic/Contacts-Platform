package com.contacts.client;

import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@OidcClientFilter
@RegisterRestClient(configKey = "contacts-api")
@Path("/api/v1/contacts")
public interface ContactsApiClient {

    @GET
    PaginatedResponse<ContactResponse> getContacts(
            @QueryParam("page") int page,
            @QueryParam("size") int size
    );
}
