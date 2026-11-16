package com.elhornero.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	public record ApiError(LocalDateTime timestamp, int status, String error, String mensaje,
			String ruta) {
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ApiError> manejarRecursoNoEncontrado(
			RecursoNoEncontradoException ex, HttpServletRequest request) {
		return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(DatoInvalidoException.class)
	public ResponseEntity<ApiError> manejarDatoInvalido(DatoInvalidoException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(OperacionNoPermitidaException.class)
	public ResponseEntity<ApiError> manejarOperacionNoPermitida(
			OperacionNoPermitidaException ex, HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(StockInsuficienteException.class)
	public ResponseEntity<ApiError> manejarStockInsuficiente(StockInsuficienteException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> manejarIntegridad(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.CONFLICT,
				"No se puede completar la operacion porque el registro esta relacionado con otros datos",
				request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> manejarCuerpoNoLegible(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST,
				"El cuerpo de la solicitud no tiene un formato valido", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> manejarTipoDeArgumentoInvalido(
			MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return responder(HttpStatus.BAD_REQUEST,
				"El parametro '" + ex.getName() + "' tiene un valor invalido", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> manejarExcepcionGeneral(Exception ex,
			HttpServletRequest request) {
		if (ex instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			HttpStatus resumen = HttpStatus.resolve(status.value());
			return ResponseEntity.status(status)
					.body(new ApiError(LocalDateTime.now(), status.value(),
							resumen != null ? resumen.getReasonPhrase() : "Error",
							ex.getMessage(), request.getRequestURI()));
		}
		log.error("Error no controlado en {}", request.getRequestURI(), ex);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR,
				"Error interno al procesar la solicitud", request);
	}

	private ResponseEntity<ApiError> responder(HttpStatus status, String mensaje,
			HttpServletRequest request) {
		ApiError error = new ApiError(LocalDateTime.now(), status.value(),
				status.getReasonPhrase(), mensaje, request.getRequestURI());
		return ResponseEntity.status(status).body(error);
	}

}
