package com.botofholding.api.Utility;

import org.springframework.stereotype.Component;

@Component // Make it a Spring bean so it can be injected
public class StandardResponseBuilder implements ResponseBuilder {

    @Override
    public String buildSuccessCreationMessage(String entityName, String identifier) {
        return String.format("%s (%s) created successfully.", entityName, identifier);
    }

    @Override
    public String buildSuccessUpdateMessage(String entityName, String identifier) {
        return String.format("%s (%s) updated successfully.", entityName, identifier);
    }

    @Override
    public String buildSuccessDeleteMessage(String entityName, String identifier) {
        return String.format("%s (%s) deleted successfully.", entityName, identifier);
    }

    @Override
    public String buildSuccessFoundMessage(String entityName, String identifier) {
        return String.format("%s (%s) found successfully.\n", entityName, identifier);
    }

    @Override
    public String buildSuccessActivateMessage(String containerName) {
        return String.format("Container (%s) activated successfully.\n", containerName);
    }

    @Override
    public String buildSuccessItemAddedMessage(String container, String item) {
        return String.format("Item (%s) added to container (%s) successfully.\n", item, container);
    }

    @Override
    public String buildSuccessItemDroppedMessage(String containerName, String name) {
        return String.format("Item (%s) dropped from container (%s) successfully.\n", name, containerName);
    }

    @Override
    public String buildSuccessContainerItemModificationMessage(String itemName, String containerName) {
        return String.format("Item (%s) modified in container (%s) successfully.\n", itemName, containerName);
    }

    @Override
    public String buildCustomSuccessMessage(String message) {
        return message;
    }
}
    