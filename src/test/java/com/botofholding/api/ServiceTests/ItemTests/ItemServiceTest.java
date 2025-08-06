package com.botofholding.api.ServiceTests.ItemTests;

import com.botofholding.api.Domain.DTO.Response.AutoCompleteDto;
import com.botofholding.api.Domain.DTO.Response.ItemSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.Domain.Entity.Guild;
import com.botofholding.api.Domain.Entity.Item;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Domain.Entity.SystemOwner;
import com.botofholding.api.Repository.ItemRepository;
import com.botofholding.api.Repository.OwnerRepository;
import com.botofholding.api.Service.Interfaces.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // Ensures each test runs in its own transaction and is rolled back.
public class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private BohUser testUser;
    private Guild testGuild;
    private SystemOwner systemOwner;

    @BeforeEach
    void setUp() {
        // The SystemOwnerDataInitializer should have already created this, so orElseGet is a fallback.
        systemOwner = (SystemOwner) ownerRepository.findByDiscordId(SystemOwner.SYSTEM_OWNER_DISCORD_ID).orElseGet(() -> {
            SystemOwner so = SystemOwner.createInstance();
            return ownerRepository.save(so);
        });
        // Create and save owners for a realistic test scenario
        testUser = BohUser.builder().discordId(1L).bohUserName("TestUser").bohGlobalUserName("TestUserGlobal").build();
        testGuild = Guild.builder().discordId(2L).guildName("TestGuild").build();
        testGuild.setCreatedBy(systemOwner);
        testGuild.setLastModifiedBy(systemOwner);



        ownerRepository.save(testUser);
        ownerRepository.save(testGuild);

        // Create and save items owned by different entities
        Item userItem = new Item();
        userItem.setItemName("Iron Sword");
        userItem.setCreatedBy(testUser);

        Item guildItem = new Item();
        guildItem.setItemName("Guild Banner");
        guildItem.setCreatedBy(testGuild);

        Item systemItem = new Item();
        systemItem.setItemName("Health Potion");
        systemItem.setCreatedBy(systemOwner);

        Item anotherSystemItem = new Item();
        anotherSystemItem.setItemName("Iron Shield");
        anotherSystemItem.setCreatedBy(systemOwner);

        itemRepository.saveAll(List.of(userItem, guildItem, systemItem, anotherSystemItem));
    }

    @Test
    @DisplayName("Find Items: Should find an item owned by the user (actor)")
    void findItemsForPrincipalAndActor_findsUserItem() {
        // Act: Search for an item owned by the user
        List<ItemSummaryDto> results = itemService.findItemsForPrincipalAndActor("Iron Sword", testUser, testGuild);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getItemName()).isEqualTo("Iron Sword");
        assertThat(results.get(0).getOwnerDisplayName()).isEqualTo(testUser.getDisplayName());
    }

    @Test
    @DisplayName("Find Items: Should find an item owned by the guild (principal)")
    void findItemsForPrincipalAndActor_findsGuildItem() {
        // Act: Search for an item owned by the guild
        List<ItemSummaryDto> results = itemService.findItemsForPrincipalAndActor("Guild Banner", testUser, testGuild);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getItemName()).isEqualTo("Guild Banner");
        assertThat(results.get(0).getOwnerDisplayName()).isEqualTo(testGuild.getDisplayName());
    }

    @Test
    @DisplayName("Find Items: Should return an empty list for a non-existent item")
    void findItemsForPrincipalAndActor_returnsEmptyForNoMatch() {
        // Act: Search for an item that does not exist
        List<ItemSummaryDto> results = itemService.findItemsForPrincipalAndActor("Mythical Armor", testUser, testGuild);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Autocomplete: Should find items owned by user and system with same prefix")
    void autocompleteItemsForPrincipalAndActor_findsUserAndSystemItems() {
        // Act: Search for a prefix that matches items from different owners
        List<AutoCompleteDto> results = itemService.autocompleteItemsForPrincipalAndActor("Iron", testUser, testGuild);

        // Assert
        assertThat(results).hasSizeGreaterThan(1);
        assertThat(results).extracting(AutoCompleteDto::getLabel).contains("Iron Sword", "Iron Shield");
    }

    @Test
    @DisplayName("Autocomplete: Should return an empty list for a non-matching prefix")
    void autocompleteItemsForPrincipalAndActor_returnsEmptyForNoMatch() {
        // Act: Search for a prefix that matches nothing
        List<AutoCompleteDto> results = itemService.autocompleteItemsForPrincipalAndActor("Xyz", testUser, testGuild);

        // Assert
        assertThat(results).isEmpty();
    }
}