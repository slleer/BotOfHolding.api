package com.botofholding.api.Config;

import com.botofholding.api.Domain.Entity.SystemOwner;
import com.botofholding.api.Repository.OwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class SystemOwnerDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SystemOwnerDataInitializer.class);
    private final OwnerRepository ownerRepository;

    public SystemOwnerDataInitializer(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if(ownerRepository.findByDiscordId(SystemOwner.SYSTEM_OWNER_DISCORD_ID).isEmpty()) {
            logger.info("SystemOwner not found, creating new singleton instance");
            ownerRepository.save(SystemOwner.createInstance());
            logger.info("SystemOwner created successfully");
        } else {
            logger.info("SystemOwner already exists");
        }
    }
}
