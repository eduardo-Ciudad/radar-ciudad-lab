package com.eduar.radarciudadlab.adapter.in.web;

import com.eduar.radarciudadlab.domain.exception.DuplicateImportException;
import com.eduar.radarciudadlab.domain.exception.InvalidLeadFileException;
import com.eduar.radarciudadlab.domain.exception.LeadNotFoundException;
import com.eduar.radarciudadlab.domain.exception.SiteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.naming.AuthenticationException;
import java.time.temporal.Temporal;
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

    /** Ex.: ?from=24/09/2026 em vez de ?from=2026-09-24, ou ?status=novo em vez de NOVO */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> type = e.getRequiredType();
        String hint;
        if (type != null && Temporal.class.isAssignableFrom(type)) {
            hint = " Datas devem ser AAAA-MM-DD.";
        } else if (type != null && type.isEnum()) {
            hint = " Valores aceitos: " + java.util.Arrays.toString(type.getEnumConstants()) + ".";
        } else {
            hint = "";
        }
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O parâmetro '" + e.getName() + "' está em formato inválido." + hint);
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

    // ---- Leads ----

    @ExceptionHandler(LeadNotFoundException.class)
    public ProblemDetail leadNotFound(LeadNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Lead não encontrado", e.getMessage());
    }

    /** JSON malformado ou valor que não cabe no tipo (ex.: "status": "GANHO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException e) {
        String detail = "JSON inválido ou com valor fora do esperado.";
        if (String.valueOf(e.getMessage()).contains("LeadStatus")) {
            detail += " Status aceitos: NOVO, CONTATADO, REUNIAO, PROPOSTA, FECHADO, PERDIDO, DESCARTADO.";
        }
        return problem(HttpStatus.BAD_REQUEST, "Corpo inválido", detail);
    }

    // ---- Importação de leads ----

    /** Mesmo arquivo de novo. batchId permite ao front linkar para a importação original. */
    @ExceptionHandler(DuplicateImportException.class)
    public ProblemDetail duplicateImport(DuplicateImportException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Arquivo já importado", e.getMessage());
        problem.setProperty("batchId", e.getBatchId());
        return problem;
    }

    @ExceptionHandler(InvalidLeadFileException.class)
    public ProblemDetail invalidLeadFile(InvalidLeadFileException e) {
        return problem(HttpStatus.BAD_REQUEST, "Arquivo inválido", e.getMessage());
    }

    /** Requisição multipart sem o campo "file". */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail missingFile(MissingServletRequestPartException e) {
        return problem(HttpStatus.BAD_REQUEST, "Arquivo ausente",
                "Envie o CSV no campo '" + e.getRequestPartName() + "' (multipart/form-data)");
    }

    /** Acima de spring.servlet.multipart.max-file-size. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail fileTooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.valueOf(413), "Arquivo grande demais", "O CSV pode ter no máximo 2 MB");
    }

    /** Ex.: POST sem multipart/form-data. */
    @ExceptionHandler(MultipartException.class)
    public ProblemDetail invalidMultipart(MultipartException e) {
        return problem(HttpStatus.BAD_REQUEST, "Upload inválido",
                "Envie o arquivo como multipart/form-data no campo 'file'");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
