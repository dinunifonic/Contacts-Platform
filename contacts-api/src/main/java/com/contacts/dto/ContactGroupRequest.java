package com.contacts.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request body used to create or update a contact group")
public class ContactGroupRequest {
    @Schema(description = "Unique name of the contact group", example = "Friends")
    @NotBlank
    private String name;
    @Schema(description = "Description of the contact group", example = "These are my friends")
    private String description;

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

}
