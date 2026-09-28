package com.contacts.resource;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaginationValidator {
    public void validate(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
