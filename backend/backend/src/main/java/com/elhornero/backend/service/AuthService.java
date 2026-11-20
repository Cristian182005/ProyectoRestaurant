package com.elhornero.backend.service;

import com.elhornero.backend.dto.AuthResponseDTO;
import com.elhornero.backend.dto.LoginRequest;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.UsuarioRepository;
import com.elhornero.backend.security.JwtServicio;
import com.elhornero.backend.security.RolUsuario;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada de autenticacion: validacion de credenciales para emitir tokens
 * JWT. El alta de usuarios la realiza el administrador desde UsuarioService.
 */
@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final EmpleadoRepository empleadoRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtServicio jwtServicio;

	public AuthService(UsuarioRepository usuarioRepository,
			EmpleadoRepository empleadoRepository, PasswordEncoder passwordEncoder,
			JwtServicio jwtServicio) {
		this.usuarioRepository = usuarioRepository;
		this.empleadoRepository = empleadoRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtServicio = jwtServicio;
	}

	/**
	 * Valida credenciales contra la BD y devuelve un JWT. Lanza
	 * BadCredentialsException (401) si el correo no existe o la contrasena no
	 * coincide.
	 */
	@Transactional(readOnly = true)
	public AuthResponseDTO autenticar(LoginRequest request) {
		Usuario usuario = usuarioRepository
				.findByPersonaEmailIgnoreCase(request.email().trim())
				.orElseThrow(this::credencialesInvalidas);
		if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
			throw credencialesInvalidas();
		}
		Persona persona = usuario.getPersona();
		// El rol debe ser soportado; si el dato esta corrupto no se emite token.
		RolUsuario.desde(persona.getRol());
		String token = jwtServicio.generarToken(usuario);
		Empleado empleado = empleadoRepository.findById(usuario.getId()).orElse(null);
		Integer empleadoId = empleado == null ? null : empleado.getId();
		return AuthResponseDTO.bearer(token, persona.getEmail(), persona.getRol(),
				jwtServicio.fechaExpiracion(), usuario.getId(), empleadoId);
	}

	private BadCredentialsException credencialesInvalidas() {
		return new BadCredentialsException("Correo o contrasena incorrectos");
	}

}
