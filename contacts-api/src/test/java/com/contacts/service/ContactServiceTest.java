package com.contacts.service;

import com.contacts.dto.ContactRequest;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.entity.Contact;
import com.contacts.entity.ContactGroup;
import com.contacts.event.ContactCreatedEvent;
import com.contacts.event.ContactEventPublisher;
import com.contacts.exception.NotFoundException;
import com.contacts.repository.ContactGroupRepository;
import com.contacts.repository.ContactRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.security.identity.SecurityIdentity;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {
    @Mock
    ContactRepository contactRepository;

    @Mock
    ContactGroupRepository contactGroupRepository;

    @Mock
    JsonWebToken jwt;

    @Mock
    SecurityIdentity securityIdentity;

    @Mock
    ContactEventPublisher contactEventPublisher;

    @InjectMocks
    ContactService contactService;

    @ParameterizedTest
    @ValueSource(strings = {"Dina", "Ali", "Ahmed"})
    void createContact_shouldSaveContactAndReturnResponse(String firstName){
        ContactRequest contactRequest= new ContactRequest();
        contactRequest.setFirstName(firstName);
        contactRequest.setLastName("Ismail");
        contactRequest.setEmail("dismail@unifonic.com");
        contactRequest.setPhone("01118350721");
        contactRequest.setGroupId(1L);
        when(jwt.getSubject()).thenReturn("user-123");
        ContactGroup contactGroup=new ContactGroup();
        contactGroup.setId(1L);
        when(contactGroupRepository.findById(1L)).thenReturn(contactGroup);

        ContactResponse contactResponse= contactService.createContact(contactRequest);

        assertEquals(firstName, contactResponse.getFirstName());
        assertEquals("Ismail", contactResponse.getLastName());
        assertEquals("dismail@unifonic.com", contactResponse.getEmail());
        assertEquals("01118350721", contactResponse.getPhone());
        assertEquals(1L, contactResponse.getGroupId());
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).persist(captor.capture());
        Contact savedContact= captor.getValue();
        assertEquals("user-123", savedContact.getOwner());
        assertEquals(1L, savedContact.getContactGroup().getId());

    }

    @Test
    void createContact_shouldFailWhenGroupDoesNotExist(){
        ContactRequest contactRequest= new ContactRequest();
        contactRequest.setFirstName("Dina");
        contactRequest.setLastName("Ismail");
        contactRequest.setEmail("dismail@unifonic.com");
        contactRequest.setPhone("01118350721");
        contactRequest.setGroupId(99L);
        when(jwt.getSubject()).thenReturn("user-123");
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows( NotFoundException.class,
                () -> contactService.createContact(contactRequest));

        verify(contactRepository, never()).persist(any(Contact.class));
    }

    @Test
    void createContact_shouldSucceedWithoutGroup() {
        ContactRequest contactRequest= new ContactRequest();
        contactRequest.setFirstName("Dina");
        contactRequest.setLastName("Ismail");
        contactRequest.setEmail("dismail@test.com");
        contactRequest.setPhone("01209836228");
        when(jwt.getSubject()).thenReturn("user-123");

        ContactResponse response = contactService.createContact(contactRequest);

        assertEquals("Dina", response.getFirstName());
        assertNull(response.getGroupId());
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepository).persist(captor.capture());
        assertNull(captor.getValue().getContactGroup());
    }

    @Test
    void createContact_shouldPublishContactCreatedEventForTheNewContact() {
        ContactRequest contactRequest= new ContactRequest();
        contactRequest.setFirstName("Dina");
        contactRequest.setLastName("Ismail");
        contactRequest.setEmail("dismail@test.com");
        contactRequest.setPhone("01209836228");
        when(jwt.getSubject()).thenReturn("user-123");
        doAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(1L);
            return null;
        }).when(contactRepository).persist(any(Contact.class));

        contactService.createContact(contactRequest);

        ArgumentCaptor<ContactCreatedEvent> captor = ArgumentCaptor.forClass(ContactCreatedEvent.class);
        verify(contactEventPublisher).publish(captor.capture());
        assertEquals(1L, captor.getValue().getContactId());
        assertNotNull(captor.getValue().getEventId());
    }

    @Test
    void getContactById_shouldReturnContactOwnedByUser() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01028398230");
        contact.setOwner("user-123");
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find("id = ?1 and owner = ?2", 1L, "user-123")).thenReturn(query);
        when(query.firstResult()).thenReturn(contact);

        ContactResponse response = contactService.getContactById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Dina", response.getFirstName());
        assertEquals("dina@test.com", response.getEmail());
        verify(contactRepository).find("id = ?1 and owner = ?2", 1L, "user-123");
    }

    @Test
    void getContactById_shouldFailWhenContactDoesNotBelongToUser() {
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);

        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find("id = ?1 and owner = ?2", 1L, "user-123")).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.getContactById(1L));
    }

    @Test
    void getContactById_shouldAllowAdminToRetrieveContact() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118380988");
        contact.setOwner("another-user");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        when(contactRepository.findById(1L)).thenReturn(contact);

        ContactResponse response = contactService.getContactById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Dina", response.getFirstName());
        verify(contactRepository).findById(1L);
        verify(contactRepository, never()).find("id = ?1 and owner = ?2", 1L, jwt.getSubject());
    }

    @Test
    void getContactById_shouldThrowNotFoundForAdmin() {
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        when(contactRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.getContactById(99L));

        verify(contactRepository).findById(99L);
    }

    @Test
    void getAllContacts_shouldReturnOnlyCurrentUsersContacts() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01116574328");
        contact.setOwner("user-123");
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find("owner", "user-123")).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Dina", response.getContent().get(0).getFirstName());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        verify(contactRepository).find("owner", "user-123");
    }

    @Test
    void getAllContacts_shouldAllowAdminToSeeAllContacts() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01117628253");
        contact.setOwner("another-user");
        when(jwt.getSubject()).thenReturn("admin-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.findAll()).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Dina", response.getContent().get(0).getFirstName());
        verify(contactRepository).findAll();
    }

    @Test
    void getAllContacts_shouldAllowAdminToFilterByGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(5L);
        when(contactGroupRepository.findById(5L)).thenReturn(group);
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01117543544");
        contact.setOwner("another-user");
        contact.setContactGroup(group);
        when(jwt.getSubject()).thenReturn("admin-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.findByGroupId(5L)).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(0, 10, 5L);

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());
        assertEquals(5L, response.getContent().get(0).getGroupId());
        assertEquals(1L, response.getTotalElements());
        verify(contactRepository).findByGroupId(5L);
    }

    @Test
    void getAllContacts_shouldThrowNotFoundWhenGroupDoesNotExist() {
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.getAllContacts(0, 10, 99L));
        verify(contactGroupRepository).findById(99L);
        verifyNoInteractions(contactRepository);
    }

    @Test
    void updateContact_shouldUpdateContactOwnedByUser() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118743622");
        contact.setOwner("user-123");
        ContactRequest request = new ContactRequest();
        request.setFirstName("Donia");
        request.setLastName("Ismail");
        request.setEmail("donia@test.com");
        request.setPhone("01118743611");
        request.setGroupId(null);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(contact);

        ContactResponse response = contactService.updateContact(1L, request);

        assertEquals("Donia", response.getFirstName());
        assertEquals("donia@test.com", response.getEmail());
        assertEquals("01118743611", response.getPhone());
        assertNull(response.getGroupId());
        assertEquals("Donia", contact.getFirstName());
        assertEquals("donia@test.com", contact.getEmail());
        verify(contactRepository).find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        );
    }

    @Test
    void updateContact_shouldFailWhenContactDoesNotBelongToUser() {
        ContactRequest request = new ContactRequest();
        request.setFirstName("Donia");
        request.setLastName("Ismail");
        request.setEmail("donia@test.com");
        request.setPhone("01118743611");
        request.setGroupId(null);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.updateContact(1L, request));
        verify(contactRepository).find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        );
    }

    @Test
    void updateContact_shouldFailWhenGroupDoesNotExist() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setOwner("user-123");
        ContactRequest request = new ContactRequest();
        request.setFirstName("Dina");
        request.setLastName("Ismail");
        request.setEmail("dina@test.com");
        request.setPhone("0111835622");
        request.setGroupId(99L);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(contact);
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.updateContact(1L, request));
        verify(contactGroupRepository).findById(99L);
    }

    @Test
    void updateContact_shouldAllowAdminToUpdateAnyContact() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118743622");
        contact.setOwner("another-user");
        ContactRequest request = new ContactRequest();
        request.setFirstName("Donia");
        request.setLastName("Ismail");
        request.setEmail("donia@test.com");
        request.setPhone("01118743611");
        request.setGroupId(null);
        when(jwt.getSubject()).thenReturn("admin-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        when(contactRepository.findById(1L)).thenReturn(contact);

        ContactResponse response = contactService.updateContact(1L, request);

        assertEquals("Donia", response.getFirstName());
        assertEquals("donia@test.com", response.getEmail());
        assertEquals("another-user", contact.getOwner());
        verify(contactRepository).findById(1L);
    }

    @Test
    void deleteContact_shouldDeleteContactOwnedByUser() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setOwner("user-123");
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(contact);

        contactService.deleteContact(1L);

        verify(contactRepository).delete(contact);
    }

    @Test
    void deleteContact_shouldFailWhenContactDoesNotBelongToUser() {
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.deleteContact(1L));
        verify(contactRepository, never()).delete(any(Contact.class));
    }

    @Test
    void deleteContact_shouldAllowAdminToDeleteAnyContact() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setOwner("another-user");
        when(jwt.getSubject()).thenReturn("admin-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        when(contactRepository.findById(1L)).thenReturn(contact);

        contactService.deleteContact(1L);

        verify(contactRepository).findById(1L);
        verify(contactRepository).delete(contact);
    }

    @Test
    void deleteContact_shouldThrowNotFoundWhenContactDoesNotExist() {
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        when(contactRepository.findById(99L)).thenReturn(null);

        assertThrows(
                NotFoundException.class,
                () -> contactService.deleteContact(99L)
        );
        verify(contactRepository, never()).delete(any(Contact.class));
    }

    @Test
    void getContactsByGroup_shouldReturnOnlyCurrentUsersContactsInGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(5L);
        when(contactGroupRepository.findById(5L)).thenReturn(group);
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118364022");
        contact.setOwner("user-123");
        contact.setContactGroup(group);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "contactGroup.id = ?1 and owner = ?2",
                5L,
                "user-123"
        )).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getContactsByGroup(0, 10, 5L);

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());
        assertEquals(5L, response.getContent().get(0).getGroupId());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        verify(contactGroupRepository).findById(5L);
        verify(contactRepository).find(
                "contactGroup.id = ?1 and owner = ?2",
                5L,
                "user-123"
        );
    }

    @Test
    void getContactsByGroup_shouldThrowNotFoundWhenGroupDoesNotExist() {
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactService.getContactsByGroup(0, 10, 99L));
        verify(contactGroupRepository).findById(99L);
        verifyNoInteractions(contactRepository);
    }

    @Test
    void getContactsByGroup_shouldAllowAdminToSeeAllContactsInGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(5L);
        when(contactGroupRepository.findById(5L)).thenReturn(group);
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118653922");
        contact.setOwner("another-user");
        contact.setContactGroup(group);
        when(jwt.getSubject()).thenReturn("admin-123");
        when(securityIdentity.hasRole("admin")).thenReturn(true);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.findByGroupId(5L)).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getContactsByGroup(0, 10, 5L);

        assertEquals(1, response.getContent().size());
        assertEquals("Dina", response.getContent().get(0).getFirstName());
        assertEquals(5L, response.getContent().get(0).getGroupId());
        verify(contactRepository).findByGroupId(5L);
    }

    @Test
    void getAllContacts_shouldAllowReadContactsRoleToSeeAllContacts() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118654233");
        contact.setOwner("user-123");
        when(jwt.getSubject()).thenReturn("service-account-contacts-reporter");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        when(securityIdentity.hasRole("read-contacts")).thenReturn(true);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.findAll()).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Dina", response.getContent().get(0).getFirstName());
        verify(contactRepository).findAll();
        verify(contactRepository, never()).find("owner", "service-account-contacts-reporter");
    }

    @Test
    void getAllContacts_shouldFilterCurrentUsersContactsByGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(5L);
        when(contactGroupRepository.findById(5L)).thenReturn(group);
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01117543544");
        contact.setOwner("user-123");
        contact.setContactGroup(group);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "owner = ?1 and contactGroup.id = ?2",
                "user-123",
                5L
        )).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(contact));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(0, 10, 5L);

        assertEquals(1, response.getContent().size());
        assertEquals(5L, response.getContent().get(0).getGroupId());
        verify(contactRepository).find(
                "owner = ?1 and contactGroup.id = ?2",
                "user-123",
                5L
        );
        verify(contactRepository, never()).findByGroupId(5L);
    }

    @Test
    void getAllContacts_shouldRoundTotalPagesUpWhenLastPageIsPartial() {
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find("owner", "user-123")).thenReturn(query);
        when(query.page(1, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of());
        when(query.count()).thenReturn(25L);

        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(1, 10, null);

        assertEquals(3, response.getTotalPages());
    }

    @Test
    void updateContact_shouldAssignExistingGroup() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Dina");
        contact.setLastName("Ismail");
        contact.setEmail("dina@test.com");
        contact.setPhone("01118743622");
        contact.setOwner("user-123");
        ContactGroup group = new ContactGroup();
        group.setId(5L);
        ContactRequest request = new ContactRequest();
        request.setFirstName("Dina");
        request.setLastName("Ismail");
        request.setEmail("dina@test.com");
        request.setPhone("01118743622");
        request.setGroupId(5L);
        when(jwt.getSubject()).thenReturn("user-123");
        when(securityIdentity.hasRole("admin")).thenReturn(false);
        PanacheQuery<Contact> query = mock(PanacheQuery.class);
        when(contactRepository.find(
                "id = ?1 and owner = ?2",
                1L,
                "user-123"
        )).thenReturn(query);
        when(query.firstResult()).thenReturn(contact);
        when(contactGroupRepository.findById(5L)).thenReturn(group);

        ContactResponse response = contactService.updateContact(1L, request);

        assertEquals(5L, response.getGroupId());
        assertEquals(5L, contact.getContactGroup().getId());
    }
}
