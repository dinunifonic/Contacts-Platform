package com.contacts.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class ContactGroup extends PanacheEntity {
    @NotBlank
    @Column(unique = true)
    String name;
    String description;

    @OneToMany(mappedBy = "contactGroup")
    List<Contact> contactsList;

    @CreationTimestamp
    LocalDateTime createdTimestamp;

    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public List<Contact> getContactsList() {
        return contactsList;
    }
    public void setContactsList(List<Contact> contactsList) {
        this.contactsList = contactsList;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
