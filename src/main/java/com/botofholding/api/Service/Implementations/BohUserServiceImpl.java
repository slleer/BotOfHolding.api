package com.botofholding.api.Service.Implementations;

import com.botofholding.api.Domain.DTO.Request.BohUserRequestDto;
import com.botofholding.api.Domain.DTO.Response.BohUserSummaryDto;
import com.botofholding.api.Domain.Entity.BohUser;
import com.botofholding.api.ExceptionHandling.ResourceNotFoundException;
import com.botofholding.api.Mapper.BohUserMapper;
import com.botofholding.api.Repository.BohUserRepository;
import com.botofholding.api.Service.Interfaces.BohUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BohUserServiceImpl implements BohUserService {

    private static final Logger logger = LoggerFactory.getLogger(BohUserServiceImpl.class);

    private final BohUserRepository userRepository;
    private final BohUserMapper bohUserMapper;

    public BohUserServiceImpl(BohUserRepository userRepository, BohUserMapper bohUserMapper) {
        this.userRepository = userRepository;
        this.bohUserMapper = bohUserMapper;
    }

    @Override
    @Transactional
    public BohUserSummaryDto updateUserProfile(BohUser userToUpdate, BohUserRequestDto dto) {
        logger.debug("Updating profile for user: {}", userToUpdate.getDisplayName());
        bohUserMapper.updateEntityFromUpdateRequest(dto, userToUpdate);
        BohUser savedUser = userRepository.save(userToUpdate);
        return bohUserMapper.toSummaryDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public BohUserSummaryDto getUserProfile(BohUser user) {
        logger.debug("Fetching profile for user: {}", user.getDisplayName());
        // The user object is already loaded, so we just need to map it.
        return bohUserMapper.toSummaryDto(user);
    }

    @Override
    @Transactional
    public void deleteBohUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
        userRepository.deleteById(userId);
    }

    // The private getAuthenticatedUser() method has been removed.
    // The service layer is now decoupled from the web security context.
}