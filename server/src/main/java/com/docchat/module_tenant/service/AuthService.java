package com.docchat.module_tenant.service;

import com.docchat.module_tenant.dto.AuthResponse;
import com.docchat.module_tenant.dto.LoginRequest;
import com.docchat.module_tenant.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
