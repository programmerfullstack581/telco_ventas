package com.telco.ventas.config;

import com.telco.ventas.entity.Permiso;
import com.telco.ventas.entity.Rol;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.RolRepository;
import com.telco.ventas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
            usuario.setAuthoritiesCache(cargarAutoridades(usuario));
            return usuario;
        };
    }

    private List<GrantedAuthority> cargarAutoridades(Usuario usuario) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (usuario.getRolId() == null) {
            return authorities;
        }
        Rol rol = rolRepository.findById(usuario.getRolId()).orElse(null);
        if (rol == null || !Boolean.TRUE.equals(rol.getActivo())) {
            return authorities;
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
        for (Permiso permiso : rol.getPermisos()) {
            authorities.add(new SimpleGrantedAuthority(permiso.getCodigo()));
        }
        return authorities;
    }
}
