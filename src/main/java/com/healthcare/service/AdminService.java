package com.healthcare.service;

import com.healthcare.dto.request.AdminRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public ApiResponse.PagedResponse<ApiResponse.UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        List<ApiResponse.UserResponse> content = page.getContent().stream()
                .map(authService::mapToUserResponse).toList();
        return new ApiResponse.PagedResponse<>(
                content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public ApiResponse.PagedResponse<ApiResponse.UserResponse> getUsersByRole(User.Role role, Pageable pageable) {
        Page<User> page = userRepository.findByRole(role, pageable);
        List<ApiResponse.UserResponse> content = page.getContent().stream()
                .map(authService::mapToUserResponse).toList();
        return new ApiResponse.PagedResponse<>(
                content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public ApiResponse.UserResponse getUserById(UUID id) {
        User user = findById(id);
        return authService.mapToUserResponse(user);
    }

    @Transactional
    public ApiResponse.UserResponse updateUserRole(UUID id, AdminRequest.UpdateUserRoleRequest request) {
        User user = findById(id);
        user.setRole(request.role());
        user = userRepository.save(user);
        log.info("Admin updated role for user [{}] to [{}]", id, request.role());
        return authService.mapToUserResponse(user);
    }

    @Transactional
    public ApiResponse.UserResponse updateUserStatus(UUID id, AdminRequest.UpdateUserStatusRequest request) {
        User user = findById(id);
        user.setIsActive(request.isActive());
        if (!request.isActive()) {
            user.setRefreshToken(null); // revoke sessions on deactivate
        }
        user = userRepository.save(user);
        log.info("Admin updated status for user [{}] to active={}", id, request.isActive());
        return authService.mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
        log.info("Admin deleted user [{}]", id);
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
