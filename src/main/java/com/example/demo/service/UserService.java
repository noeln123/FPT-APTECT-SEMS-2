package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserCreationRequest;
import com.example.demo.entity.Users;
import com.example.demo.enums.Role;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;

import java.util.List;


@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or @userRepository.findById(#id)?.get()?.username == authentication.name") 
    public Users getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    public Users getMyinfo(){
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        return userRepository.findByUsername(name).orElseThrow(
            () -> new AppException(ErrorCode.USER_NOT_EXISTED));

    }

    public Users saveUser(UserCreationRequest userDTO ) {
        // Chuyển đổi từ UserDTO sang Users
        Users user = new Users();
        user.setUsername(userDTO.getUsername());
        user.setPassword_hash(userDTO.getPassword_hash());
        user.setEmail(userDTO.getEmail());
        user.setRole(Role.STUDENT.name());
        user.setBalance(0.0);
        user.setFullName(userDTO.getFullName());

        //Validation
        if (userRepository.existsByUsername(user.getUsername())){
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(user.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

		//Hash the password
		user.setPassword_hash(passwordEncoder.encode(user.getPassword_hash()));
        
        return userRepository.save(user);
    }


    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or @userRepository.findById(#id)?.get()?.username == authentication.name")
    public Users updateUser(Long id, Users userUpdateRequest){
        Users user = getUserById(id); //old user

        user.setEmail(userUpdateRequest.getEmail());
        user.setFullName(userUpdateRequest.getFullName());
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword_hash(passwordEncoder.encode(userUpdateRequest.getPassword_hash()));

        return userRepository.save(user);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or @userRepository.findById(#id)?.get()?.username == authentication.name")
    public void deleteUser(Long id){
        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        userRepository.deleteById(id);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or @userRepository.findById(#id)?.get()?.username == authentication.name")
    public String assignRole(Long id, String role){
        List<String> roles = List.of("TEACHER", "STUDENT");
        if (!roles.contains(role)) {
            throw new AppException(ErrorCode.ROLE_NOT_EXISTED);
        }

        Users user = userRepository.findById(id).get();
        user.setRole(role);
        userRepository.save(user);

        return "Change role to " + role + " completed!";
    }
}

