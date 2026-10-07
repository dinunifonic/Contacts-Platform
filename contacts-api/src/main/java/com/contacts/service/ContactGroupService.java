package com.contacts.service;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.dto.PaginatedResponse;
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
public class ContactGroupService {
    @Inject
    ContactGroupRepository contactGroupRepository;
    @Inject
    ContactRepository contactRepository;

    @Transactional
    public ContactGroupResponse createContactGroup(ContactGroupRequest contactGroupRequest){
        ContactGroup contactGroup=new ContactGroup();
        contactGroup.setName(contactGroupRequest.getName());
        contactGroup.setDescription(contactGroupRequest.getDescription());
        contactGroupRepository.persist(contactGroup);

        ContactGroupResponse contactGroupResponse=new ContactGroupResponse();
        contactGroupResponse.setId(contactGroup.getId());
        contactGroupResponse.setName(contactGroup.getName());
        contactGroupResponse.setDescription(contactGroup.getDescription());
        contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());

        return contactGroupResponse;
    }

    public ContactGroupResponse getContactGroupById(Long id){
        ContactGroup contactGroup= contactGroupRepository.findById(id);
        if (contactGroup == null) {
            throw new NotFoundException("Group with id:" + id + " not found");
        }

        ContactGroupResponse contactGroupResponse=new ContactGroupResponse();
        contactGroupResponse.setId(contactGroup.getId());
        contactGroupResponse.setName(contactGroup.getName());
        contactGroupResponse.setDescription(contactGroup.getDescription());
        contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());

        return contactGroupResponse;
    }

    public PaginatedResponse<ContactGroupResponse> getAllContactGroups(int page, int size, String name){
        PanacheQuery<ContactGroup> query;
        if (name == null) {
            query = contactGroupRepository.findAll();
        } else {
            query = contactGroupRepository.findByName(name);
        }
        List<ContactGroup> contactGroupsList = query.page(page, size).list();
        long totalElements = query.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<ContactGroupResponse> contactGroupResponseList=new ArrayList<ContactGroupResponse>();
        for(ContactGroup contactGroup: contactGroupsList){
            ContactGroupResponse contactGroupResponse= new ContactGroupResponse();
            contactGroupResponse.setId(contactGroup.getId());
            contactGroupResponse.setName(contactGroup.getName());
            contactGroupResponse.setDescription(contactGroup.getDescription());
            contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());
            contactGroupResponseList.add(contactGroupResponse);
        }

        PaginatedResponse<ContactGroupResponse> response=new PaginatedResponse<>();
        response.setContent(contactGroupResponseList);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }

    @Transactional
    public ContactGroupResponse updateContactGroup(Long id, ContactGroupRequest contactGroupRequest){
        ContactGroup contactGroup= contactGroupRepository.findById(id);
        if (contactGroup == null) {
            throw new NotFoundException("Group with id:" + id + " not found");
        }
        contactGroup.setName(contactGroupRequest.getName());
        contactGroup.setDescription(contactGroupRequest.getDescription());

        ContactGroupResponse contactGroupResponse=new ContactGroupResponse();
        contactGroupResponse.setId(contactGroup.getId());
        contactGroupResponse.setName(contactGroup.getName());
        contactGroupResponse.setDescription(contactGroup.getDescription());
        contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());

        return contactGroupResponse;
    }

    @Transactional
    public void deleteContactGroup(Long id){
        ContactGroup contactGroup= contactGroupRepository.findById(id);
        if (contactGroup == null) {
            throw new NotFoundException("Group with id:" + id + " not found");
        }
        contactRepository.update(
                "contactGroup = null where contactGroup.id = ?1",
                id
        );

        contactGroupRepository.delete(contactGroup);
    }
}
