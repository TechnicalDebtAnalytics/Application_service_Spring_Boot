package com.debtlens.backend.controller;

import com.debtlens.backend.dto.request.RegistrationRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "http://localhost:5173")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(
            RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @RequestBody RegistrationRequest request
    ) {

        RegistrationResponse response =
                registrationService.register(request);

        return ResponseEntity.ok(response);
    }
}
