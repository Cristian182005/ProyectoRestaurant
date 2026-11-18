package com.elhornero.backend.security;

import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA. El subject del token es
 * el correo del usuario y el claim "rol" guarda su rol.
 */
@Service
public class JwtServicio {

	private final SecretKey clave;
	private final long expiracionMinutos;

	public JwtServicio(@Value("${jwt.secret}") String secreto,
			@Value("${jwt.expiracion-minutos}") long expiracionMinutos) {
		this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
		this.expiracionMinutos = expiracionMinutos;
	}

	public String generarToken(Usuario usuario) {
		Persona persona = usuario.getPersona();
		LocalDateTime expira = fechaExpiracion();
		return Jwts.builder()
				.subject(persona.getEmail())
				.claim("rol", persona.getRol())
				.claim("id", usuario.getId())
				.issuedAt(new Date())
				.expiration(Date.from(expira.atZone(ZoneId.systemDefault()).toInstant()))
				.signWith(clave)
				.compact();
	}

	/**
	 * Devuelve la fecha de expiracion del token recien generado (mismo calculo
	 * que usa generarToken), util para armar la respuesta del login.
	 */
	public LocalDateTime fechaExpiracion() {
		return LocalDateTime.now().plusMinutes(expiracionMinutos);
	}

	/**
	 * Valida firma y expiracion del token. Lanza JwtException (de jjwt) si el
	 * token es invalido, esta expirado o la firma no coincide.
	 */
	public Claims validarToken(String token) {
		return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
	}

	public String extraerEmail(Claims claims) {
		return claims.getSubject();
	}

	public String extraerRol(Claims claims) {
		return claims.get("rol", String.class);
	}

}
