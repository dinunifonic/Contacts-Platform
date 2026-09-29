package com.contacts.service;

import com.contacts.dto.ContactRequest;
import com.contacts.dto.ContactResponse;
import com.contacts.dto.PaginatedResponse;
import com.contacts.entity.Contact;
import com.contacts.entity.ContactGroup;
import com.contacts.exception.NotFoundException;
import com.contacts.repository.ContactGroupRepository;
import com.contacts.repository.ContactRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactService {
    @Inject
    ContactRepository contactRepository;

    @Inject
    ContactGroupRepository contactGroupRepository;

    @Transactional
    public ContactResponse createContact(ContactRequest contactRequest){
        ContactGroup contactGroup= null;
        if (contactRequest.getGroupId()!=null)
            contactGroup= contactGroupRepository.findById(contactRequest.getGroupId());
        Contact contact= new Contact();
        contact.setFirstName(contactRequest.getFirstName());
        contact.setLastName(contactRequest.getLastName());
        contact.setEmail(contactRequest.getEmail());
        contact.setPhone(contactRequest.getPhone());
        contact.setContactGroup(contactGroup);
        contactRepository.persist(contact);

        ContactResponse contactResponse= new ContactResponse();
        contactResponse.setId(contact.getId());
        contactResponse.setFirstName(contact.getFirstName());
        contactResponse.setLastName(contact.getLastName());
        contactResponse.setEmail(contact.getEmail());
        contactResponse.setPhone(contact.getPhone());
        contactResponse.setGroupId((contactGroup != null ? contactGroup.getId() : null));
        contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());

        return contactResponse;
    }

    public ContactResponse getContactById(Long id){
        Contact contact= contactRepository.findById(id);
        if (contact == null) {
            throw new NotFoundException("Contact with id:" + id + " not found");
        }

        ContactResponse contactResponse=new ContactResponse();
        contactResponse.setId(contact.getId());
        contactResponse.setFirstName(contact.getFirstName());
        contactResponse.setLastName(contact.getLastName());
        contactResponse.setEmail(contact.getEmail());
        contactResponse.setPhone(contact.getPhone());
        contactResponse.setGroupId(contact.getContactGroup() != null ? contact.getContactGroup().getId() : null);
        contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());

        return contactResponse;
    }

    public PaginatedResponse<ContactResponse> getAllContacts(int page, int size, Long groupId){
        PanacheQuery<Contact> query;
        if (groupId == null) {
            query = contactRepository.findAll();
        } else {
            query = contactRepository.findByGroupId(groupId);
        }
        List<Contact> contactsList = query.page(page, size).list();
        long totalElements = query.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<ContactResponse> contactResponseList=new ArrayList<ContactResponse>();
        for(Contact contact: contactsList){
            ContactResponse contactResponse=new ContactResponse();
            contactResponse.setId(contact.getId());
            contactResponse.setFirstName(contact.getFirstName());
            contactResponse.setLastName(contact.getLastName());
            contactResponse.setEmail(contact.getEmail());
            contactResponse.setPhone(contact.getPhone());
            contactResponse.setGroupId(contact.getContactGroup() != null ? contact.getContactGroup().getId() : null);
            contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());
            contactResponseList.add(contactResponse);
        }

        PaginatedResponse<ContactResponse> response = new PaginatedResponse<>();
        response.setContent(contactResponseList);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }

    @Transactional
    public ContactResponse updateContact(Long id, ContactRequest contactRequest){
        Contact contact=contactRepository.findById(id);
        if (contact == null) {
            throw new NotFoundException("Contact with id:" + id + " not found");
        }
        contact.setFirstName(contactRequest.getFirstName());
        contact.setLastName(contactRequest.getLastName());
        contact.setEmail(contactRequest.getEmail());
        contact.setPhone(contactRequest.getPhone());
        ContactGroup contactGroup=null;
        if (contactRequest.getGroupId()!=null)
            contactGroup= contactGroupRepository.findById(contactRequest.getGroupId());
        contact.setContactGroup(contactGroup);

        ContactResponse contactResponse=new ContactResponse();
        contactResponse.setId(contact.getId());
        contactResponse.setFirstName(contact.getFirstName());
        contactResponse.setLastName(contact.getLastName());
        contactResponse.setEmail(contact.getEmail());
        contactResponse.setPhone(contact.getPhone());
        contactResponse.setGroupId(contact.getContactGroup() != null ? contact.getContactGroup().getId() : null);
        contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());

        return contactResponse;
    }

    @Transactional
    public void deleteContact(Long id){
        Contact contact= contactRepository.findById(id);
        if (contact == null) {
            throw new NotFoundException("Contact with id:" + id + " not found");
        }

        contactRepository.delete(contact);
    }

    public PaginatedResponse<ContactResponse> getContactsByGroup(int page, int size, Long groupId){
        ContactGroup contactGroup = contactGroupRepository.findById(groupId);
        if (contactGroup == null) {
            throw new NotFoundException("Contact Group with id:" + groupId + " not found");
        }

        PanacheQuery<Contact> query= contactRepository.findByGroupId(groupId);
        List<Contact> contactsList= query.page(page, size).list();
        long totalElements = query.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<ContactResponse> contactResponseList = new ArrayList<ContactResponse>();
        for(Contact contact:contactsList){
            ContactResponse contactResponse=new ContactResponse();
            contactResponse.setId(contact.getId());
            contactResponse.setFirstName(contact.getFirstName());
            contactResponse.setLastName(contact.getLastName());
            contactResponse.setEmail(contact.getEmail());
            contactResponse.setPhone(contact.getPhone());
            contactResponse.setGroupId(contact.getContactGroup() != null ? contact.getContactGroup().getId() : null);
            contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());
            contactResponseList.add(contactResponse);
        }

        PaginatedResponse<ContactResponse> response=new PaginatedResponse<>();
        response.setContent(contactResponseList);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }

}
