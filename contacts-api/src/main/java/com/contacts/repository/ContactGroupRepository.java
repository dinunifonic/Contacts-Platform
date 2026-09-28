package com.contacts.repository;

import com.contacts.entity.ContactGroup;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContactGroupRepository implements PanacheRepository<ContactGroup> {
        public PanacheQuery<ContactGroup> findByName(String name){
            return find("name", name);
        }
}
