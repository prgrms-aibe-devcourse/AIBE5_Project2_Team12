package com.generic4.itda.dto.security;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.member.UserRole;
import com.generic4.itda.domain.member.UserStatus;
import com.generic4.itda.domain.member.UserType;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ItDaPrincipal implements UserDetails {

    private String email;

    @ToString.Exclude
    private String password;
    private String name;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private UserType type;


    public static ItDaPrincipal from(Member member) {
        return new ItDaPrincipal(
                member.getEmail().getValue(),
                member.getHashedPassword(),
                member.getName(),
                member.getPhone().getValue(),
                member.getRole(),
                member.getStatus(),
                member.getType()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }
}
