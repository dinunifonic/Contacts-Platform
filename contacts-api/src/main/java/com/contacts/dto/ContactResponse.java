package com.contacts.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response containing contact information")
public class ContactResponse {
    @Schema(description = "Unique identifier of the contact", example = "1")
    Long id;
    @Schema(description = "Contact's first name", example = "Dina")
    String firstName;
    @Schema(description = "Contact's last name", example = "Ismail")
    String lastName;
    @Schema(description = "Contact's email address", example = "dina@unifonic.com")
    String email;
    @Schema(description = "Contact's phone number", example = "0108357102")
    String phone;
    @Schema(description = "ID of the contact group", example = "1", nullable = true)
    Long groupId;
    @Schema(description = "Timestamp when the contact was created", example = "2026-09-28T14:30:00")
    private LocalDateTime createdTimestamp;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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

    public Long getGroupId() {
        return groupId;
    }
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
