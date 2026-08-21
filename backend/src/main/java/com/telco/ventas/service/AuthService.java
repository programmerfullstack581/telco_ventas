package com.telco.ventas.service;

import com.telco.ventas.dto.LoginRequest;
import com.telco.ventas.dto.LoginResponse;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String rol = usuarioRepository.findRolNombreByUsuarioId(usuario.getId())
                .orElse("SIN_ROL");
        String token = jwtService.generateToken(usuario, usuario.getId(), rol);

        auditoriaService.registrar("LOGIN", "USUARIO", usuario.getId(),
                "Inicio de sesión exitoso", usuario);

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .username(usuario.getUsername())
                .rol(rol)
                .userId(usuario.getId())
                .build();
    }
}
