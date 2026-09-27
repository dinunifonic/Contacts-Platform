package com.contacts.repository;

import com.contacts.entity.Contact;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ContactRepository implements PanacheRepository<Contact> {
    public List<Contact> findByGroupId(Long groupId) {
        return find("contactGroup.id", groupId).list();
    }
}
