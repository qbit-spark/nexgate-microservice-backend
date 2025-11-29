package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.RegistrationTokenEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.GenerateRegistrationTokenRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegistrationTokenResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.RegistrationTokenService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.RegistrationTokenMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-event/check-in/tokens")
@RequiredArgsConstructor
public class RegistrationTokenController {

    private final RegistrationTokenService registrationTokenService;
    private final RegistrationTokenMapper registrationTokenMapper;

    @PostMapping("/generate")
    public ResponseEntity<GlobeSuccessResponseBuilder> generateToken(
            @Valid @RequestBody GenerateRegistrationTokenRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        log.info("Generating registration token for event: {}", request.getEventId());

        RegistrationTokenEntity token = registrationTokenService.generateRegistrationToken(request);
        RegistrationTokenResponse response = registrationTokenMapper.toResponse(token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Registration token generated successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<GlobeSuccessResponseBuilder> validateToken(@PathVariable String token)
            throws ItemNotFoundException {

        log.info("Validating token: {}", token);

        RegistrationTokenEntity tokenString = registrationTokenService.findByToken(token);
        RegistrationTokenResponse response = registrationTokenMapper.toResponse(tokenString);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Token is valid")
                        .data(response)
                        .build());
    }
}