package com.selimhorri.app.unit.service;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;
import com.selimhorri.app.unit.util.UserUtil;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    private UserDto userDto;
    private User user;

    @BeforeEach
    void setUp() {
        userDto = UserUtil.getSampleUserDto();
        user = UserMappingHelper.map(userDto);
    }

    @Test
    void testFindAll() {
        // Given
        List<User> users = Arrays.asList(user);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto> result = userServiceImpl.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userDto.getFirstName(), result.get(0).getFirstName());
        assertEquals(userDto.getEmail(), result.get(0).getEmail());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testFindAllWhenEmpty() {
        // Given
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<UserDto> result = userServiceImpl.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testFindById() {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        UserDto result = userServiceImpl.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserObjectNotFoundException.class, () -> userServiceImpl.findById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testSave() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userServiceImpl.save(userDto);

        // Then
        assertNotNull(result);
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdate() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userServiceImpl.update(userDto);

        // Then
        assertNotNull(result);
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testDeleteById() {
        // Given
        Integer userId = 1;

        // When
        userServiceImpl.deleteById(userId);

        // Then
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void testFindByUsername() {
        // Given
        String username = "johndoe";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.of(user));

        // When
        UserDto result = userServiceImpl.findByUsername(username);

        // Then
        assertNotNull(result);
        assertEquals(userDto.getFirstName(), result.getFirstName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository, times(1)).findByCredentialUsername(username);
    }

    @Test
    void testFindByUsernameNotFound() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByCredentialUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserObjectNotFoundException.class, () -> userServiceImpl.findByUsername(username));
        verify(userRepository, times(1)).findByCredentialUsername(username);
    }

    @Test
    void testSaveAdminUser() {
        // Given
        UserDto adminDto = UserUtil.getSampleAdminUserDto();
        User adminUser = UserMappingHelper.map(adminDto);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // When
        UserDto result = userServiceImpl.save(adminDto);

        // Then
        assertNotNull(result);
        assertEquals(adminDto.getFirstName(), result.getFirstName());
        assertEquals(adminDto.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
}
