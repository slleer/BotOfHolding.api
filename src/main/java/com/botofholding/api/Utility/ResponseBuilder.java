package com.botofholding.api.Utility;

public interface ResponseBuilder {
    String buildSuccessCreationMessage(String entityName, String identifier);

    String buildSuccessUpdateMessage(String entityName, String identifier);

    String buildSuccessDeleteMessage(String entityName, String identifier);

    String buildCustomSuccessMessage(String message); // For more general cases

    String buildSuccessFoundMessage(String entityName, String identifier);

    String buildSuccessActivateMessage(String container);

    String buildSuccessItemAddedMessage(String container, String item);

    String buildSuccessItemDroppedMessage(String containerName, String name);

    String buildSuccessContainerItemModificationMessage(String itemName, String containerName);
}
