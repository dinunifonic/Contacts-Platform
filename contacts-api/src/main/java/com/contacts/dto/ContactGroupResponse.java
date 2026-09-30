package com.contacts.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response containing contact group information")
public class ContactGroupResponse {
    @Schema(description = "Unique identifier of the contact group", example = "1")
    private Long id;
    @Schema(description = "Unique name of the contact group", example = "Friends")
    private String name;
    @Schema(description = "Description of the contact group", example = "These are my friends")
    private String description;
    @Schema(description = "Timestamp when the contact group was created", example = "2026-09-28T14:30:00")
    private LocalDateTime createdTimestamp;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
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

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
