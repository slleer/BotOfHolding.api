package com.botofholding.api.Service.Interfaces;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ItemSummaryDto;
import com.botofholding.api.Domain.Entity.Owner;

import java.util.List;

public interface ItemService {

    ItemSummaryDto findItemById(Long id);
    List<ItemSummaryDto> findItemsForPrincipalAndActor(String name, Owner actor, Owner principal);
    List<AutoCompleteDto> autocompleteItemsForPrincipalAndActor(String prefix, Owner actor, Owner principal);
}
