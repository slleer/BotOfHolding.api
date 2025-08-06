package com.botofholding.api.Config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import com.botofholding.api.Domain.DTO.Seed.ItemSeedDto;
import com.botofholding.api.Domain.Entity.Item;
import com.botofholding.api.Domain.Entity.Owner;
import com.botofholding.api.Domain.Entity.SystemOwner;
import com.botofholding.api.Mapper.ItemMapper;
import com.botofholding.api.Repository.ItemRepository;
import com.botofholding.api.Repository.OwnerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Initializes item data in the database from a JSON file if the item table is empty.
 * This component runs after the {@link SystemOwnerDataInitializer} due to its {@code @Order(2)}.
 */
@Component
@Order(2)
public class ItemDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ItemDataInitializer.class);
    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;
    private final OwnerRepository ownerRepository;
    private final ItemMapper itemMapper;

    public ItemDataInitializer(ItemRepository itemRepository, ObjectMapper objectMapper,
                               OwnerRepository ownerRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.objectMapper = objectMapper;
        this.ownerRepository = ownerRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if(itemRepository.count() > 0) {
            logger.info("Items already exist in database, skipping seeding.");
            return;
        }

        // [WORLD-CLASS] Define the loggers we want to control
        final String HIBERNATE_SQL_LOGGER = "org.hibernate.SQL";
        final String HIBERNATE_BIND_LOGGER = "org.hibernate.orm.jdbc.bind"; // The logger for parameter binding

        // Store the original log levels so we can restore them
        Level originalSqlLogLevel = LogManager.getLogger(HIBERNATE_SQL_LOGGER).getLevel();
        Level originalBindLogLevel = LogManager.getLogger(HIBERNATE_BIND_LOGGER).getLevel();

        // Temporarily set the log levels to OFF
        Configurator.setLevel(HIBERNATE_SQL_LOGGER, Level.OFF);
        Configurator.setLevel(HIBERNATE_BIND_LOGGER, Level.OFF);
        logger.info("Temporarily suppressing Hibernate SQL and parameter binding logging for item seeding.");


        try {
            logger.info("Item table is empty, Seeding items from file...");
            Owner systemOwner = ownerRepository.findByDiscordId(SystemOwner.SYSTEM_OWNER_DISCORD_ID)
                    .orElseThrow(() -> new RuntimeException("SystemOwner not found. Initialization order might be incorrect."));

            runAs(systemOwner, () -> {
                try (InputStream inputStream = TypeReference.class.getResourceAsStream("/Data/itemList.json")) {
                    if (inputStream == null) {
                        logger.error("FATAL: Cannot find itemList.json in resources/Data. Aborting seed.");
                        throw new RuntimeException("itemList.json not found");
                    }

                    TypeReference<List<ItemSeedDto>> typeReference = new TypeReference<>() {
                    };
                    List<ItemSeedDto> itemDtos = objectMapper.readValue(inputStream, typeReference);

                    List<Item> itemsToCreate = itemDtos.stream()
                            .map(itemMapper::toEntity)
                            .collect(Collectors.toList());

                    itemRepository.saveAll(itemsToCreate);
                    logger.info("Successfully seeded {} items to the database.", itemsToCreate.size());

                } catch (Exception e) {
                    logger.error("Failed to seed items from file. Halting app execution.", e);
                    throw new RuntimeException("Failed to initialize seed data", e);
                }
            });
        } finally {
            // This block ensures that the log levels are ALWAYS restored, even if an error occurs.
            Configurator.setLevel(HIBERNATE_SQL_LOGGER, originalSqlLogLevel);
            Configurator.setLevel(HIBERNATE_BIND_LOGGER, originalBindLogLevel);
            logger.info("Restored Hibernate SQL and parameter binding logging to original levels.");
        }

    }
    /**
     * A private helper to execute a block of code within a specific security context.
     * This ensures that auditing works automatically for data initializers.
     * @param owner The user to run the code as.
     * @param task The code to execute.
     */
    private void runAs(Owner owner, Runnable task) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                owner, null, owner.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            task.run();
        } finally {
            // Always clear the context afterwards
            SecurityContextHolder.clearContext();
        }
    }
}
