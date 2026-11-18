package com.elhornero.backend.service;

import com.elhornero.backend.dto.RegistroUsuarioDTO;
import com.elhornero.backend.dto.UsuarioResponseDTO;
import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.repository.UsuarioRepository;
import com.elhornero.backend.security.RolUsuario;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private static final Pattern CORREO = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final UsuarioRepository usuarioRepository;
	private final ObjectProvider<PasswordEncoder> passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public Optional<Usuario> buscarPorEmail(String email) {
		String correo = normalizar(email);
		if (correo == null) {
			throw new DatoInvalidoException("El correo es obligatorio");
		}
		return usuarioRepository.findByPersonaEmailIgnoreCase(correo);
	}

	@Transactional(readOnly = true)
	public boolean existeEmail(String email) {
		String correo = normalizar(email);
		if (correo == null) {
			throw new DatoInvalidoException("El correo es obligatorio");
		}
		return usuarioRepository.existsByPersonaEmailIgnoreCase(correo);
	}

	@Transactional
	public UsuarioResponseDTO registrar(RegistroUsuarioDTO request) {
		validarRequest(request);
		String email = request.email().trim();
		if (usuarioRepository.existsByPersonaEmailIgnoreCase(email)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un usuario con el correo '" + email + "'");
		}
		// El registro publico solo admite roles de operacion; los
		// administradores se crean por otro medio.
		RolUsuario rol = RolUsuario.desde(request.rol());
		if (rol == RolUsuario.ADMINISTRADOR) {
			throw new DatoInvalidoException(
					"No se puede registrar un usuario con rol ADMINISTRADOR");
		}

		Persona persona = new Persona();
		persona.setEmail(email);
		persona.setTelefono(normalizar(request.telefono()));
		persona.setDireccion(normalizar(request.direccion()));
		persona.setRol(rol.name());

		Usuario usuario = new Usuario();
		usuario.setPersona(persona);
		persona.setUsuario(usuario);
		usuario.setPassword(codificarPassword(request.password()));

		return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
	}

	private String codificarPassword(String password) {
		PasswordEncoder encoder = passwordEncoder.getIfAvailable();
		if (encoder == null) {
			throw new IllegalStateException(
					"Falta configurar un PasswordEncoder (BCrypt) para poder registrar usuarios");
		}
		return encoder.encode(password);
	}

	private void validarRequest(RegistroUsuarioDTO request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del usuario son obligatorios");
		}
		if (request.email() == null || request.email().isBlank()) {
			throw new DatoInvalidoException("El correo es obligatorio");
		}
		String email = request.email().trim();
		if (email.length() > 150) {
			throw new DatoInvalidoException("El correo no puede superar 150 caracteres");
		}
		if (!CORREO.matcher(email).matches()) {
			throw new DatoInvalidoException("El correo no tiene un formato valido");
		}
		if (request.password() == null || request.password().isBlank()) {
			throw new DatoInvalidoException("La contrasena es obligatoria");
		}
		if (request.password().length() < 6 || request.password().length() > 255) {
			throw new DatoInvalidoException("La contrasena debe tener entre 6 y 255 caracteres");
		}
		if (normalizar(request.telefono()) != null
				&& normalizar(request.telefono()).length() > 20) {
			throw new DatoInvalidoException("El telefono no puede superar 20 caracteres");
		}
		if (normalizar(request.direccion()) != null
				&& normalizar(request.direccion()).length() > 255) {
			throw new DatoInvalidoException("La direccion no puede superar 255 caracteres");
		}
		if (normalizar(request.rol()) != null && normalizar(request.rol()).length() > 50) {
			throw new DatoInvalidoException("El rol no puede superar 50 caracteres");
		}
	}

	private String normalizar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}

}
