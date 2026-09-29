package com.contacts.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response containing a list of resources and pagination information")
public class PaginatedResponse<T> {
    @Schema(description = "Resources returned for the current page")
    private List<T> content;
    @Schema(description = "Zero-based page number", example = "0")
    private int page;
    @Schema(description = "Number of resources requested per page", example = "20")
    private int size;
    @Schema(description = "Total number of resources matching the request", example = "45")
    private long totalElements;
    @Schema(description = "Total number of available pages", example = "3")
    private int totalPages;

    public List<T> getContent() {
        return content;
    }
    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
