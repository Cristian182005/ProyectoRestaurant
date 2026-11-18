package com.elhornero.backend.security;

import com.elhornero.backend.exception.GlobalExceptionHandler.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Devuelve los errores de seguridad (401 sin token / 403 sin permiso) con el
 * mismo JSON ApiError que usa GlobalExceptionHandler.
 */
@Component
public class SeguridadRespuestaHandler
		implements AuthenticationEntryPoint, AccessDeniedHandler {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		responder(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion para acceder a este recurso",
				request, response);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		responder(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta operacion",
				request, response);
	}

	private void responder(HttpStatus status, String mensaje, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		ApiError cuerpo = new ApiError(LocalDateTime.now(), status.value(),
				status.getReasonPhrase(), mensaje, request.getRequestURI());
		response.getWriter().write(
				"{\"timestamp\":\"" + cuerpo.timestamp() + "\",\"status\":" + cuerpo.status()
						+ ",\"error\":\"" + cuerpo.error() + "\",\"mensaje\":\""
						+ cuerpo.mensaje() + "\",\"ruta\":\"" + cuerpo.ruta() + "\"}");
	}

}
