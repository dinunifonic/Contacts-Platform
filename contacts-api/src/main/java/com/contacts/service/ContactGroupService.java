package com.contacts.service;

import com.contacts.dto.ContactGroupRequest;
import com.contacts.dto.ContactGroupResponse;
import com.contacts.entity.ContactGroup;
import com.contacts.repository.ContactGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ContactGroupService {
    @Inject
    ContactGroupRepository contactGroupRepository;

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
        ContactGroupResponse contactGroupResponse=new ContactGroupResponse();
        contactGroupResponse.setId(contactGroup.getId());
        contactGroupResponse.setName(contactGroup.getName());
        contactGroupResponse.setDescription(contactGroup.getDescription());
        contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());

        return contactGroupResponse;
    }

    public List<ContactGroupResponse> getAllContactGroups(){
        List<ContactGroupResponse> contactGroupResponseList=new ArrayList<ContactGroupResponse>();
        List<ContactGroup> contactGroupsList = contactGroupRepository.findAll().list();
        for(ContactGroup contactGroup: contactGroupsList){
            ContactGroupResponse contactGroupResponse= new ContactGroupResponse();
            contactGroupResponse.setId(contactGroup.getId());
            contactGroupResponse.setName(contactGroup.getName());
            contactGroupResponse.setDescription(contactGroup.getDescription());
            contactGroupResponse.setCreatedTimestamp(contactGroup.getCreatedTimestamp());
            contactGroupResponseList.add(contactGroupResponse);
        }

        return contactGroupResponseList;
    }

    @Transactional
    public ContactGroupResponse updateContactGroup(Long id, ContactGroupRequest contactGroupRequest){
        ContactGroup contactGroup= contactGroupRepository.findById(id);
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
        contactGroupRepository.delete(contactGroup);
    }
}
