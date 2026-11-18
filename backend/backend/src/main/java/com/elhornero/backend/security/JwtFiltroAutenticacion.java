package com.elhornero.backend.security;

import com.elhornero.backend.entity.Usuario;
import com.elhornero.backend.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que lee el header "Authorization: Bearer &lt;token&gt;", valida el
 * JWT y, si es correcto, autentica la peticion con el rol del usuario. Si no
 * hay header o el token es invalido, la peticion sigue sin autenticar (el
 * EntryPoint devolvera 401 si el recurso lo exige).
 */
@Component
public class JwtFiltroAutenticacion extends OncePerRequestFilter {

	private static final String PREFIJO_BEARER = "Bearer ";

	private final JwtServicio jwtServicio;
	private final UsuarioRepository usuarioRepository;

	public JwtFiltroAutenticacion(JwtServicio jwtServicio,
			UsuarioRepository usuarioRepository) {
		this.jwtServicio = jwtServicio;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith(PREFIJO_BEARER)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(PREFIJO_BEARER.length());
		try {
			Claims claims = jwtServicio.validarToken(token);
			String email = jwtServicio.extraerEmail(claims);
			if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				Usuario usuario = usuarioRepository.findByPersonaEmailIgnoreCase(email)
						.orElse(null);
				if (usuario != null) {
					String rol = usuario.getPersona().getRol();
					var autoridad = new SimpleGrantedAuthority("ROLE_" + rol);
					var autenticacion = new UsernamePasswordAuthenticationToken(email, null,
							List.of(autoridad));
					autenticacion.setDetails(
							new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(autenticacion);
				}
			}
		} catch (Exception ex) {
			// Token invalido o expirado: se deja la peticion sin autenticar.
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

}
