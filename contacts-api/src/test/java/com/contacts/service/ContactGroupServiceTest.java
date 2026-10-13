package com.contacts.service;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.entity.ContactGroup;
import com.contacts.exception.NotFoundException;
import com.contacts.repository.ContactGroupRepository;
import com.contacts.repository.ContactRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactGroupServiceTest {
    @Mock
    ContactGroupRepository contactGroupRepository;

    @Mock
    ContactRepository contactRepository;

    @InjectMocks
    ContactGroupService contactGroupService;

    @Test
    void createContactGroup_shouldReturnCreatedGroup() {
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Friends");
        request.setDescription("My friends");
        doAnswer(invocation -> {
            ContactGroup group = invocation.getArgument(0);
            group.setId(1L);
            return null;
        }).when(contactGroupRepository).persist(any(ContactGroup.class));

        ContactGroupResponse response = contactGroupService.createContactGroup(request);

        assertEquals(1L, response.getId());
        assertEquals("Friends", response.getName());
        assertEquals("My friends", response.getDescription());
        verify(contactGroupRepository).persist(any(ContactGroup.class));
    }

    @Test
    void getContactGroupById_shouldReturnGroup_whenGroupExists() {
        ContactGroup group = new ContactGroup();
        group.setId(1L);
        group.setName("Friends");
        group.setDescription("My friends");
        when(contactGroupRepository.findById(1L)).thenReturn(group);

        ContactGroupResponse response = contactGroupService.getContactGroupById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Friends", response.getName());
        assertEquals("My friends", response.getDescription());
    }

    @Test
    void getContactGroupById_shouldThrowNotFound_whenGroupDoesNotExist() {
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactGroupService.getContactGroupById(99L));
    }

    @Test
    void getAllContactGroups_shouldReturnAllGroups() {
        ContactGroup group = new ContactGroup();
        group.setId(1L);
        group.setName("Friends");
        group.setDescription("My friends");
        @SuppressWarnings("unchecked")
        PanacheQuery<ContactGroup> query = mock(PanacheQuery.class);
        when(contactGroupRepository.findAll()).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(group));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactGroupResponse> response = contactGroupService.getAllContactGroups(0, 10, null);

        assertEquals(1, response.getContent().size());
        assertEquals("Friends", response.getContent().get(0).getName());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }

    @Test
    void getAllContactGroups_shouldFilterByName() {
        ContactGroup group = new ContactGroup();
        group.setId(2L);
        group.setName("Family");
        group.setDescription("My family");
        @SuppressWarnings("unchecked")
        PanacheQuery<ContactGroup> query = mock(PanacheQuery.class);
        when(contactGroupRepository.findByName("Family")).thenReturn(query);
        when(query.page(0, 10)).thenReturn(query);
        when(query.list()).thenReturn(List.of(group));
        when(query.count()).thenReturn(1L);

        PaginatedResponse<ContactGroupResponse> response = contactGroupService.getAllContactGroups(0, 10, "Family");

        assertEquals(1, response.getContent().size());
        assertEquals("Family", response.getContent().get(0).getName());
        verify(contactGroupRepository).findByName("Family");
    }

    @Test
    void updateContactGroup_shouldUpdateGroup_whenGroupExists() {
        ContactGroup group = new ContactGroup();
        group.setId(1L);
        group.setName("Friends");
        group.setDescription("My friends");
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Best Friends");
        request.setDescription("My bestfriends");
        when(contactGroupRepository.findById(1L)).thenReturn(group);

        ContactGroupResponse response = contactGroupService.updateContactGroup(1L, request);

        assertEquals("Best Friends", response.getName());
        assertEquals("My bestfriends", response.getDescription());
        assertEquals("Best Friends", group.getName());
        assertEquals("My bestfriends", group.getDescription());
    }

    @Test
    void updateContactGroup_shouldThrowNotFound_whenGroupDoesNotExist() {
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Friends");
        request.setDescription("My friends");
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactGroupService.updateContactGroup(99L, request));
    }

    @Test
    void deleteContactGroup_shouldUnassignContactsAndDeleteGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(1L);
        group.setName("Friends");
        when(contactGroupRepository.findById(1L)).thenReturn(group);

        contactGroupService.deleteContactGroup(1L);

        verify(contactRepository).update(
                "contactGroup = null where contactGroup.id = ?1",
                1L
        );
        verify(contactGroupRepository).delete(group);
    }

    @Test
    void deleteContactGroup_shouldThrowNotFound_whenGroupDoesNotExist() {
        when(contactGroupRepository.findById(99L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> contactGroupService.deleteContactGroup(99L));
        verify(contactGroupRepository, never()).delete(any(ContactGroup.class));
        verifyNoInteractions(contactRepository);
    }

    @Test
    void deleteContactGroup_shouldUnassignContactsBeforeDeletingGroup() {
        ContactGroup group = new ContactGroup();
        group.setId(1L);
        group.setName("Friends");
        when(contactGroupRepository.findById(1L)).thenReturn(group);

        contactGroupService.deleteContactGroup(1L);

        InOrder inOrder = inOrder(contactRepository, contactGroupRepository);
        inOrder.verify(contactRepository).update(
                "contactGroup = null where contactGroup.id = ?1",
                1L
        );
        inOrder.verify(contactGroupRepository).delete(group);
    }
}

