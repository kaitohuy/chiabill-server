package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse loginAnonymous();

    AuthResponse getCurrentUser(Long userId);

    AuthResponse loginGoogle(String idToken, Long currentUserId);
}