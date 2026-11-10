package br.com.colheitacompartilhada.usuario;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final UUID id;
    private final String email;
    private final Papel papel;
    private final boolean ativo;
    private String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.papel = usuario.getPapel();
        this.ativo = usuario.isAtivo();
        this.senhaHash = usuario.getSenhaHash();
    }

    public UUID id() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    // O Spring chama isto depois de conferir a senha: o hash não fica guardado na sessão.
    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }
}
