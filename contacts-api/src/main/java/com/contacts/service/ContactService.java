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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ContactService {
    @Inject
    ContactRepository contactRepository;

    @Inject
    ContactGroupRepository contactGroupRepository;

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    ContactEventPublisher contactEventPublisher;

    @Transactional
    public ContactResponse createContact(ContactRequest contactRequest){
        String owner = jwt.getSubject();
        ContactGroup contactGroup= null;
        if (contactRequest.getGroupId()!=null) {
            contactGroup = contactGroupRepository.findById(contactRequest.getGroupId());
            if (contactGroup == null) {
                throw new NotFoundException("Contact group with id:" + contactRequest.getGroupId() + " not found");
            }
        }
        Contact contact= new Contact();
        contact.setFirstName(contactRequest.getFirstName());
        contact.setLastName(contactRequest.getLastName());
        contact.setEmail(contactRequest.getEmail());
        contact.setPhone(contactRequest.getPhone());
        contact.setContactGroup(contactGroup);
        contact.setOwner(owner);
        contactRepository.persist(contact);

        String correlationId = UUID.randomUUID().toString();
        ContactCreatedEvent event = new ContactCreatedEvent(
                UUID.randomUUID(),
                correlationId,
                contact.getId(),
                Instant.now()
        );
        contactEventPublisher.publish(event);

        return toResponse(contact);
    }

    public ContactResponse getContactById(Long id){
        Contact contact;
        String owner= jwt.getSubject();
        if(securityIdentity.hasRole("admin")) {
            contact=contactRepository.findById(id);
        }else{
            contact= contactRepository.find(
                    "id = ?1 and owner = ?2",
                    id,
                    owner
            ).firstResult();
        }
        if (contact == null) {
            throw new NotFoundException("Contact with id:" + id + " not found");
        }

        return toResponse(contact);
    }

    public PaginatedResponse<ContactResponse> getAllContacts(int page, int size, Long groupId){
        if (groupId != null && contactGroupRepository.findById(groupId) == null) {
            throw new NotFoundException(
                    "Contact Group with id:" + groupId + " not found"
            );
        }
        PanacheQuery<Contact> query;
        String owner= jwt.getSubject();
        //Administrators and the reporting service account (read-contacts) may read all contacts
        if(securityIdentity.hasRole("admin") || securityIdentity.hasRole("read-contacts")) {
            if (groupId == null) {
                query = contactRepository.findAll();
            } else {
                query = contactRepository.findByGroupId(groupId);
            }
        }else{
            if (groupId == null){
                query= contactRepository.find("owner", owner);
            }else{
                query = contactRepository.find(
                        "owner = ?1 and contactGroup.id = ?2",
                        owner,
                        groupId
                );
            }
        }

        return toPaginatedResponse(query, page, size);
    }

    @Transactional
    public ContactResponse updateContact(Long id, ContactRequest contactRequest){
        Contact contact;
        String owner= jwt.getSubject();
        if(securityIdentity.hasRole("admin")) {
            contact = contactRepository.findById(id);
        }else{
            contact = contactRepository.find(
                    "id = ?1 and owner = ?2",
                    id,
                    owner
            ).firstResult();
        }
        if (contact == null) {
            throw new NotFoundException("Contact with id:" + id + " not found");
        }
        contact.setFirstName(contactRequest.getFirstName());
        contact.setLastName(contactRequest.getLastName());
        contact.setEmail(contactRequest.getEmail());
        contact.setPhone(contactRequest.getPhone());
        ContactGroup contactGroup=null;
        if (contactRequest.getGroupId()!=null) {
            contactGroup = contactGroupRepository.findById(contactRequest.getGroupId());
            if (contactGroup == null) {
                throw new NotFoundException("Contact group with id:" + contactRequest.getGroupId() + " not found");
            }
        }
        contact.setContactGroup(contactGroup);

        return toResponse(contact);
    }

    @Transactional
    public void deleteContact(Long id) {
        Contact contact;
        String owner = jwt.getSubject();

        if (securityIdentity.hasRole("admin")) {
            contact = contactRepository.findById(id);
        } else {
            contact = contactRepository.find(
                    "id = ?1 and owner = ?2",
                    id,
                    owner
            ).firstResult();
        }
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

        String owner= jwt.getSubject();
        PanacheQuery<Contact> query;
        if(securityIdentity.hasRole("admin")) {
            query= contactRepository.findByGroupId(groupId);
        }else{
            query = contactRepository.find(
                    "contactGroup.id = ?1 and owner = ?2",
                    groupId,
                    owner
            );
        }

        return toPaginatedResponse(query, page, size);
    }

    private PaginatedResponse<ContactResponse> toPaginatedResponse(PanacheQuery<Contact> query, int page, int size){
        List<Contact> contactsList = query.page(page, size).list();
        long totalElements = query.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<ContactResponse> contactResponseList = new ArrayList<ContactResponse>();
        for(Contact contact: contactsList){
            contactResponseList.add(toResponse(contact));
        }

        PaginatedResponse<ContactResponse> response = new PaginatedResponse<>();
        response.setContent(contactResponseList);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }

    private ContactResponse toResponse(Contact contact){
        ContactResponse contactResponse = new ContactResponse();
        contactResponse.setId(contact.getId());
        contactResponse.setFirstName(contact.getFirstName());
        contactResponse.setLastName(contact.getLastName());
        contactResponse.setEmail(contact.getEmail());
        contactResponse.setPhone(contact.getPhone());
        contactResponse.setGroupId(contact.getContactGroup() != null ? contact.getContactGroup().getId() : null);
        contactResponse.setCreatedTimestamp(contact.getCreatedTimestamp());

        return contactResponse;
    }

}
