package com.contacts.service;

import com.contacts.dto.ContactRequest;
import com.contacts.dto.ContactResponse;
import com.contacts.entity.Contact;
import com.contacts.entity.ContactGroup;
import com.contacts.repository.ContactGroupRepository;
import com.contacts.repository.ContactRepository;
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

    public List<ContactResponse> getAllContacts(){
        List<Contact> contactsList = contactRepository.findAll().list();
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

        return contactResponseList;
    }

    @Transactional
    public ContactResponse updateContact(Long id, ContactRequest contactRequest){
        Contact contact=contactRepository.findById(id);
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
        contactRepository.delete(contact);
    }

    public List<ContactResponse> getContactsByGroup(Long groupId){
        List<ContactResponse> contactResponseList = new ArrayList<ContactResponse>();
        List<Contact> contactsList= contactRepository.findByGroupId(groupId);
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
        return contactResponseList;
    }

}
