package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.config.security.JwtTokenService;
import com.eduar.radarciudadlab.config.security.LoginAttemptLimiter;
import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtTokenService tokens;
    private final LoginAttemptLimiter loginLimiter;

    public AuthController(AuthenticationManager authenticationManager, UserRepository users, JwtTokenService tokens,
                          LoginAttemptLimiter loginLimiter) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokens = tokens;
        this.loginLimiter = loginLimiter;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // Antes da senha: durante o bloqueio, nem a senha certa entra (senão daria para continuar adivinhando)
        loginLimiter.checkAllowed(request.email());
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException e) {
            loginLimiter.recordFailure(request.email());
            throw e;
        }
        loginLimiter.recordSuccess(request.email());

        AppUser user = users.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        users.recordLogin(user.id());

        JwtTokenService.IssuedToken token = tokens.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(), user.email(), user.role());
    }

    /**
     * Logout de verdade: revoga todos os tokens do usuário (todas as sessões), não só o cookie do navegador.
     * O uid sempre existe aqui, porque o ActiveUserTokenValidator já recusou token sem ele.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        Number userId = jwt.getClaim("uid");
        users.revokeTokens(userId.longValue());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsStringList("roles"), jwt.getExpiresAt());
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, String email, UserRole role) {}

    public record MeResponse(String email, List<String> roles, Instant expiresAt) {}
}