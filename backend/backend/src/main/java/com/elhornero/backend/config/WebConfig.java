package com.elhornero.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion CORS: permite al frontend Angular en desarrollo
 * (http://localhost:4200) consumir la API REST en http://localhost:8080.
 * El bean es consumido por Spring Security via http.cors(...).
 */
@Configuration
public class WebConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuracion.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", configuracion);
		return fuente;
	}

}
