package com.botofholding.api.Service.Implementations;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ItemSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Item;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.ExceptionHandling.ItemNotFoundException;
import com.botofholding.api.Mapper.ItemMapper;
import com.botofholding.api.Repository.ItemRepository;
import com.botofholding.api.Service.Interfaces.ItemService;
import com.botofholding.api.Utility.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceImpl.class);
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemServiceImpl(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    @Transactional
    public ItemSummaryDto findItemById(Long id) {
        return itemRepository.findById(id)
                .map(itemMapper::toSummaryDto)
                .orElseThrow(() -> new ItemNotFoundException("Item with id " + id + " not found."));
    }

    @Override
    @Transactional
    public List<ItemSummaryDto> findItemsForPrincipalAndActor(String name, Owner actor, Owner principal) {
        logger.info("Searching for items with name '{}' for owners: {} & {}", name, actor.getDisplayName(), principal.getDisplayName());
        Pageable top50 = Pageable.ofSize(50);
        
        return searchAndMapItems(
                () -> itemRepository.findAllByNameForOwners(name, actor, principal, top50),
                name,
                itemMapper::toSummaryDto);
    }

    @Override
    @Transactional
    public List<AutoCompleteDto> autocompleteItemsForPrincipalAndActor(String prefix, Owner actor, Owner principal) {
        logger.info("Searching for items with prefix '{}' for owners: {} & {}", prefix, actor.getDisplayName(), principal.getDisplayName());
        Pageable top25 = Pageable.ofSize(25);

        return searchAndMapItems(
                () -> itemRepository.findAllByNameLikeForOwners(prefix, actor, principal, top25),
                prefix,
                itemMapper::toAutoCompleteDto);
    }


    /**
     * A generic helper method that executes a search function, logs if the result is empty,
     * and then maps the results to a specified DTO.
     *
     * @param searcher A Supplier that provides the list of {@link Item} entities (e.g., a repository call).
     * @param searchTerm The string to search for (e.g., an exact name or a prefix).
     * @param mapper A function to map the found {@link Item} entities to the desired DTO type.
     * @return A list of mapped DTOs.
     * @param <T> The type of the DTO to be returned.
     */
    private <T> List<T> searchAndMapItems(Supplier<List<Item>> searcher, String searchTerm, Function<Item, T> mapper) {
        List<Item> items = searcher.get();
        if (items.isEmpty()) {
            logger.info("No items found for search term '{}'.", searchTerm);
        }

        return items.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
