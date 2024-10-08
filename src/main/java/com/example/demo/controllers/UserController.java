package com.example.demo.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserCreationRequest;
import com.example.demo.entity.Users;
import com.example.demo.service.ForgotPasswordService;
import com.example.demo.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(value = "/api/user")
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private ForgotPasswordService forgotPasswordService; // Khai báo ForgotPasswordService

	@PreAuthorize("hasAuthority('SCOPE_ADMIN') or @userRepository.findById(#id)?.get()?.username == authentication.name")
	@GetMapping("/{id}")
	ApiResponse<Users> getUserById(@PathVariable Integer id) {
		ApiResponse<Users> apiResponse = new ApiResponse<>();
		apiResponse.setResult(userService.getUserById(id));
		return apiResponse;
	}

	@GetMapping("/myinfo")
	public ApiResponse<Users> getMyInfo() {
		ApiResponse<Users> apiResponse = new ApiResponse<>();
		apiResponse.setResult(userService.getMyinfo());
		return apiResponse;
	}

	@PostMapping
	ApiResponse<Users> addUser(@Valid @RequestBody UserCreationRequest user) {
		ApiResponse<Users> apiResponse = new ApiResponse<>();
		apiResponse.setResult(userService.saveUser(user));
		return apiResponse;
	}

	@PutMapping("/{id}")
	ApiResponse<Users> updateUserById(@PathVariable Integer id, @Valid @RequestBody Users user) {
		ApiResponse<Users> apiResponse = new ApiResponse<>();
		apiResponse.setResult(userService.updateUser(id, user));
		return apiResponse;
	}

	@DeleteMapping("/{id}")
	ApiResponse<String> deleteUserById(@PathVariable Integer id) {
		userService.deleteUser(id);
		return ApiResponse.<String>builder().result("User has been deleted").build();
	}

	@PostMapping("/{id}/assign-role")
	ApiResponse<String> postMethodName(@PathVariable Integer id, @RequestBody String role) {
		System.out.println(role);
		return ApiResponse.<String>builder().result(userService.assignRole(id, role)).build();
	}

	@PostMapping("/forgot-password")
	public ApiResponse<String> forgotPassword(@RequestBody Map<String, String> requestBody) {
		String email = requestBody.get("email");
		log.info("Request to reset password for email: {}", email);

		if (!userService.emailExists(email)) {
			throw new RuntimeException("Email does not exist!");
		}

		forgotPasswordService.createPasswordResetToken(email);

		return ApiResponse.<String>builder().result("Verification code has been sent to your email").build();
	}
	@PostMapping("/reset-password")
	public ApiResponse<String> resetPassword(@RequestBody Map<String, String> requestBody) {
		String email = requestBody.get("email");
		String verificationCode = requestBody.get("code");
		String newPassword = requestBody.get("newPassword");

		forgotPasswordService.resetPassword(email, verificationCode, newPassword);
		return ApiResponse.<String>builder().result("Password has been reset successfully").build();
	}


}
