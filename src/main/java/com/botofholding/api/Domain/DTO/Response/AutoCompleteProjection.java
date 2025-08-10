package com.botofholding.api.Domain.DTO.Response;

/**
 * A projection interface for mapping native query results for autocompletion.
 * This is more efficient than mapping to a full entity when only a few fields are needed.
 */
public interface AutoCompleteProjection {
    Long getId();
    String getLabel();
    String getDescription();
}