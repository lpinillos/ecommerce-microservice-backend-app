package com.selimhorri.app.unit.util;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.domain.RoleBasedAuthority;
import java.util.HashSet;

public class UserUtil {

    public static UserDto getSampleUserDto() {
        return UserDto.builder()
                .userId(1)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("https://example.com/image.jpg")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .addressDtos(new HashSet<>())
                .credentialDto(CredentialDto.builder()
                        .credentialId(1)
                        .username("johndoe")
                        .password("password123")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }

    public static UserDto getSampleUserDtoWithAddresses() {
        return UserDto.builder()
                .userId(100)
                .firstName("John")
                .lastName("Doe")
                .imageUrl("https://example.com/image.jpg")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .addressDtos(AddressUtil.getSampleAddressDtoSet())
                .credentialDto(CredentialDto.builder()
                        .credentialId(1)
                        .username("johndoe")
                        .password("password123")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }

    public static UserDto getSampleAdminUserDto() {
        return UserDto.builder()
                .userId(2)
                .firstName("Admin")
                .lastName("User")
                .imageUrl("https://example.com/admin-image.jpg")
                .email("admin@example.com")
                .phone("+0987654321")
                .addressDtos(new HashSet<>())
                .credentialDto(CredentialDto.builder()
                        .credentialId(2)
                        .username("admin")
                        .password("admin123")
                        .roleBasedAuthority(RoleBasedAuthority.ROLE_ADMIN)
                        .isEnabled(true)
                        .isAccountNonExpired(true)
                        .isAccountNonLocked(true)
                        .isCredentialsNonExpired(true)
                        .build())
                .build();
    }
}
