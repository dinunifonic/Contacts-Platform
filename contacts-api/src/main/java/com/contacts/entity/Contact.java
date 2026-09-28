package com.contacts.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
public class Contact extends PanacheEntity {
    @NotBlank
    String firstName;

    @NotBlank
    String lastName;

    @NotBlank
    @Email
    @Column(unique = true)
    String email;

    @NotBlank
    String phone;

    @ManyToOne
    ContactGroup contactGroup;

    @CreationTimestamp
    LocalDateTime createdTimestamp;

    public Long getId(){
        return id;
    }
    public void setId(){
        this.id=id;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ContactGroup getContactGroup() {
        return contactGroup;
    }
    public void setContactGroup(ContactGroup contactGroup) {
        this.contactGroup = contactGroup;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
