package com.synapseforge.controller;

import com.synapseforge.dto.*;
import com.synapseforge.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/cadastro")
    public UserResponseDTO cadastro(@RequestBody @Valid UserRequestDTO dto) {
        return service.cadastro(dto);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginDTO dto) {
        String token = service.login(dto);
        return Map.of("access_token", token);
    }
}
