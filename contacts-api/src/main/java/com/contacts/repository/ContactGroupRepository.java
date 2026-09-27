package com.contacts.repository;

import com.contacts.entity.ContactGroup;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContactGroupRepository implements PanacheRepository<ContactGroup> {

}
