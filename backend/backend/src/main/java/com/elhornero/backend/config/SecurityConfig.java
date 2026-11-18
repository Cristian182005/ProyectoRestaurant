package com.elhornero.backend.config;

import com.elhornero.backend.security.JwtFiltroAutenticacion;
import com.elhornero.backend.security.SeguridadRespuestaHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad del backend: estadoless (JWT), BCrypt para
 * contrasenas y la matriz de autorizacion por rol alineada con los
 * PERMISOS_BASE del frontend (usuarios-roles.ts).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtFiltroAutenticacion jwtFiltro;
	private final SeguridadRespuestaHandler seguridadRespuestaHandler;

	public SecurityConfig(JwtFiltroAutenticacion jwtFiltro,
			SeguridadRespuestaHandler seguridadRespuestaHandler) {
		this.jwtFiltro = jwtFiltro;
		this.seguridadRespuestaHandler = seguridadRespuestaHandler;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				// CORS: usa el CorsConfigurationSource definido en WebConfig
				.cors(Customizer.withDefaults())
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(seguridadRespuestaHandler)
						.accessDeniedHandler(seguridadRespuestaHandler))
				.authorizeHttpRequests(auth -> auth
						// Autenticacion publica
						.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
						.requestMatchers("/error").permitAll()
						// Inventario y recetas: solo administrador
						.requestMatchers("/api/insumos/**", "/api/ingresos/**",
								"/api/mermas/**", "/api/ingredientes/**")
						.hasRole("ADMINISTRADOR")
						// Escritura de platos/categorias: solo administrador
						.requestMatchers(HttpMethod.POST, "/api/platos/**",
								"/api/categorias/**")
						.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.PUT, "/api/platos/**",
								"/api/categorias/**")
						.hasRole("ADMINISTRADOR")
						.requestMatchers(HttpMethod.DELETE, "/api/platos/**",
								"/api/categorias/**")
						.hasRole("ADMINISTRADOR")
						// Salon y comandas: administrador o mozo
						.requestMatchers("/api/mesas/**", "/api/pedidos/**",
								"/api/detalles-pedido/**")
						.hasAnyRole("ADMINISTRADOR", "MOZO")
						// Caja y comprobantes: administrador o cajero
						.requestMatchers("/api/comprobantes/**")
						.hasAnyRole("ADMINISTRADOR", "CAJERO")
						// Lectura de carta: cualquier autenticado (GET platos/categorias)
						.requestMatchers(HttpMethod.GET, "/api/platos/**",
								"/api/categorias/**")
						.authenticated()
						// Todo lo demas: autenticado
						.anyRequest().authenticated())
				.addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}
