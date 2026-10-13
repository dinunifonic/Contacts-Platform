package com.contacts.service;

import com.contacts.client.ContactsApiClient;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactReporterServiceTest {
    @Mock
    ContactsApiClient contactsApiClient;

    @InjectMocks
    ContactReporterService contactReporterService;

    @Test
    void getTotalContacts_shouldReturnTotalElementsFromContactsApi() {
        PaginatedResponse<ContactResponse> response = new PaginatedResponse<>();
        response.setTotalElements(42);
        when(contactsApiClient.getContacts(0, 1)).thenReturn(response);

        long total = contactReporterService.getTotalContacts();

        assertEquals(42, total);
        verify(contactsApiClient).getContacts(0, 1);
    }
}
