package com.contacts.service;

import com.contacts.client.ContactsApiClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ContactReporterService {
    @Inject
    @RestClient
    ContactsApiClient contactsApiClient;

    public long getTotalContacts() {
        return contactsApiClient.getContacts(0, 1).getTotalElements();
    }
}
