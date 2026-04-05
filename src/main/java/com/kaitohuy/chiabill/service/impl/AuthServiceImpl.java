package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.*;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.UserMapper;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.security.GoogleTokenService;
import com.kaitohuy.chiabill.security.JwtService;
import com.kaitohuy.chiabill.service.interfaces.AuthService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final GoogleTokenService googleTokenService;

    @Override
    public AuthResponse loginAnonymous() {

        User user = User.builder()
                .name("Anonymous")
                .isAnonymous(true)
                .provider("ANONYMOUS")
                .providerId("ANON_" + System.currentTimeMillis())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    public AuthResponse getCurrentUser(Long userId) {

        User user = userRepository.findById(userId).orElseThrow();

        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginGoogle(String idToken, Long currentUserId) {

        var payload = googleTokenService.verify(idToken);

        String email = payload.getEmail();
        String name = Optional.ofNullable((String) payload.get("name"))
                .orElse("Google User");
        String providerId = payload.getSubject();

        Optional<User> existingByProvider =
                userRepository.findByProviderAndProviderId("GOOGLE", providerId);

        Optional<User> existingByEmail =
                userRepository.findByEmail(email);

        User user;

        if (existingByProvider.isPresent()) {
            user = existingByProvider.get();

        } else if (existingByEmail.isPresent()) {
            // 🔥 merge theo email
            user = existingByEmail.get();
            user.setProvider("GOOGLE");
            user.setProviderId(providerId);
            user.setIsAnonymous(false);

        } else if (currentUserId != null) {
            // 🔥 merge anonymous
            user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new BusinessException("User not found"));

            if (Boolean.TRUE.equals(user.getIsAnonymous())) {
                user.setEmail(email);
                user.setName(name);
                user.setProvider("GOOGLE");
                user.setProviderId(providerId);
                user.setIsAnonymous(false);
            } else {
                user = createNewGoogleUser(email, name, providerId);
            }

        } else {
            user = createNewGoogleUser(email, name, providerId);
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .build();
    }

    private User createNewGoogleUser(String email, String name, String providerId) {

        return User.builder()
                .email(email)
                .name(name)
                .provider("GOOGLE")
                .providerId(providerId)
                .isAnonymous(false)
                .build();
    }
}