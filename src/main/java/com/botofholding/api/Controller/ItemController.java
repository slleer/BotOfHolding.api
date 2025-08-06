package com.botofholding.api.Controller;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ItemSummaryDto;
import com.botofholding.api.Domain.DTO.Response.StandardApiResponse;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Service.Interfaces.ItemService;
import com.botofholding.api.Utility.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/api/items")
public class ItemController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    private final ItemService itemService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public ItemController(ItemService itemService, ResponseBuilder responseBuilder) {
        this.itemService = itemService;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessItem(#id, principal)")
    public ResponseEntity<StandardApiResponse<ItemSummaryDto>> getItemById(@PathVariable("id") Long id) {
        logger.info("Attempting to find item by id: {}.", id);
        ItemSummaryDto foundItem = itemService.findItemById(id);
        String message = responseBuilder.buildSuccessFoundMessage("Item", foundItem.getItemName());
        StandardApiResponse<ItemSummaryDto> response = new StandardApiResponse<>(true, message, foundItem);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<ItemSummaryDto>>> findItemsByName(@RequestParam(required = false) String name) {
        Owner principal = getAuthenticatedPrincipal();
        Owner actor = getRequestActor();
        logger.info("Attempting to find items for principal '{}' and actor '{}' with name: {}"
                , principal.getDisplayName()
                , actor.getDisplayName()
                , name);
        List<ItemSummaryDto> dtoList = itemService.findItemsForPrincipalAndActor(name, actor, principal);
        String message = responseBuilder.buildSuccessFoundMessage("Items", name);
        StandardApiResponse<List<ItemSummaryDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StandardApiResponse<List<AutoCompleteDto>>> autocompleteItemsByName(@RequestParam String prefix) {
        Owner principal = getAuthenticatedPrincipal();
        Owner actor = getRequestActor();
        logger.info("Performing autocomplete lookup for items with prefix '{}' for principal '{}' and actor '{}'."
                , prefix
                , principal.getDisplayName()
                , actor.getDisplayName());
        List<AutoCompleteDto> dtoList = itemService.autocompleteItemsForPrincipalAndActor(prefix, actor, principal);
        String message = responseBuilder.buildSuccessFoundMessage("Items", prefix);
        StandardApiResponse<List<AutoCompleteDto>> response = new StandardApiResponse<>(true, message, dtoList);
        return ResponseEntity.ok(response);
    }
}
