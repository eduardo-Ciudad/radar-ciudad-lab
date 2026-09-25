package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.naming.AuthenticationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SiteNotFoundException.class)
    public ProblemDetail siteNotFound(SiteNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Site não encontrado", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    /** Ex.: ?from=24/09/2026 em vez de ?from=2026-09-24 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O parâmetro '" + e.getName() + "' está em formato inválido. Datas devem ser AAAA-MM-DD.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail authenticationFailed(AuthenticationException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Falha no login", "E-mail ou senha inválidos");
    }

    /** Corpo da requisição inválido (ex.: e-mail sem @, senha em branco). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}