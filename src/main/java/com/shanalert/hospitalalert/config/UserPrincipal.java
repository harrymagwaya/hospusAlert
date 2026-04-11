package com.shanalert.hospitalalert.config;


import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.model.UserRole;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String password;
    private final String email;
    private final UserRole userRole;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String username, String email, String password, UserRole role,UserStatus status, Collection<? extends GrantedAuthority> authorities){
        this.id = id;
        this.password  = password;
        this.email = email;
        this.userRole = role;
        this.authorities = authorities;
        this.status = status;

    }


    public static UserPrincipal createUser(User user){
        List <GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getUserStatus(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    public UUID getId() {
        return id;
    }

    public String getEmail(){
        return email;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
