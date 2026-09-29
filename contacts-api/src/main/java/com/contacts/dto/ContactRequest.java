package com.contacts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request body used to create or update a contact")
public class ContactRequest {
    @Schema(description = "Contact's first name", example = "Dina")
    @NotBlank
    String firstName;
    @Schema(description = "Contact's last name", example = "Ismail")
    @NotBlank
    String lastName;
    @Schema(description = "Contact's email address", example = "dina@unifonic.com")
    @NotBlank
    @Email
    String email;
    @Schema(description = "Contact's phone number", example = "0108357102")
    @NotBlank
    String phone;
    @Schema(description = "ID of the contact group", example = "1", nullable = true)
    Long groupId;


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

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public Long getGroupId() {
        return groupId;
    }
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
