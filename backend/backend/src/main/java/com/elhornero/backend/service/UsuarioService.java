package com.elhornero.backend.service;

import com.elhornero.backend.dto.ActualizarUsuarioDTO;
import com.elhornero.backend.dto.CambioPasswordDTO;
import com.elhornero.backend.dto.RegistroUsuarioDTO;
import com.elhornero.backend.dto.UsuarioResponseDTO;
import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.UsuarioRepository;
import com.elhornero.backend.security.RolUsuario;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion de usuarios del sistema. Todo el alta y la edicion ocurren desde
 * el modulo de administracion; no existe registro publico. Las contrasenas
 * se guardan siempre codificadas con BCrypt.
 */
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

	// ------------------------------------------------------------------
	// Consulta
	// ------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<UsuarioResponseDTO> listar() {
		return usuarioRepository.findAll().stream().map(UsuarioResponseDTO::from).toList();
	}

	/** Busca por correo o por nombre (coincidencia parcial, sin distinguir mayusculas). */
	@Transactional(readOnly = true)
	public List<UsuarioResponseDTO> buscar(String termino) {
		String texto = termino == null ? "" : termino.trim();
		if (texto.isEmpty()) {
			return listar();
		}
		return usuarioRepository
				.findByPersonaEmailContainingIgnoreCaseOrPersonaNombreContainingIgnoreCase(texto,
						texto)
				.stream().map(UsuarioResponseDTO::from).toList();
	}

	@Transactional(readOnly = true)
	public UsuarioResponseDTO buscarPorId(Integer id) {
		return UsuarioResponseDTO.from(obtener(id));
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

	// ------------------------------------------------------------------
	// Alta
	// ------------------------------------------------------------------

	/**
	 * Crea un usuario con cualquiera de los roles soportados
	 * (ADMINISTRADOR, MOZO o CAJERO). Solo lo invoca el controlador
	 * administrativo, ya protegido con hasRole("ADMINISTRADOR").
	 */
	@Transactional
	public UsuarioResponseDTO registrar(RegistroUsuarioDTO request) {
		validarRequest(request);
		String email = request.email().trim();
		if (usuarioRepository.existsByPersonaEmailIgnoreCase(email)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un usuario con el correo '" + email + "'");
		}
		RolUsuario rol = RolUsuario.desde(request.rol());

		Persona persona = new Persona();
		persona.setEmail(email);
		persona.setNombre(normalizar(request.nombre()));
		persona.setTelefono(normalizar(request.telefono()));
		persona.setDireccion(normalizar(request.direccion()));
		persona.setRol(rol.name());

		Usuario usuario = new Usuario();
		usuario.setPersona(persona);
		persona.setUsuario(usuario);
		usuario.setPassword(codificarPassword(request.password()));

		return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
	}

	// ------------------------------------------------------------------
	// Edicion
	// ------------------------------------------------------------------

	@Transactional
	public UsuarioResponseDTO actualizar(Integer id, ActualizarUsuarioDTO request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del usuario son obligatorios");
		}
		Usuario usuario = obtener(id);
		Persona persona = usuario.getPersona();

		String email = normalizar(request.email());
		if (email == null) {
			throw new DatoInvalidoException("El correo es obligatorio");
		}
		if (email.length() > 150) {
			throw new DatoInvalidoException("El correo no puede superar 150 caracteres");
		}
		if (!CORREO.matcher(email).matches()) {
			throw new DatoInvalidoException("El correo no tiene un formato valido");
		}
		if (!email.equalsIgnoreCase(persona.getEmail())
				&& usuarioRepository.existsByPersonaEmailIgnoreCase(email)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un usuario con el correo '" + email + "'");
		}
		String nombre = normalizar(request.nombre());
		if (nombre != null && nombre.length() > 100) {
			throw new DatoInvalidoException("El nombre no puede superar 100 caracteres");
		}
		String telefono = normalizar(request.telefono());
		if (telefono != null && telefono.length() > 20) {
			throw new DatoInvalidoException("El telefono no puede superar 20 caracteres");
		}
		String direccion = normalizar(request.direccion());
		if (direccion != null && direccion.length() > 255) {
			throw new DatoInvalidoException("La direccion no puede superar 255 caracteres");
		}

		persona.setNombre(nombre);
		persona.setEmail(email);
		persona.setTelefono(telefono);
		persona.setDireccion(direccion);
		return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
	}

	/**
	 * Cambia el rol de un usuario. Un administrador no puede modificar su
	 * propio rol para no perder por accidente el ultimo acceso con esa
	 * capacidad.
	 */
	@Transactional
	public UsuarioResponseDTO cambiarRol(Integer id, String rol, String emailSolicitante) {
		RolUsuario nuevoRol = RolUsuario.desde(rol);
		Usuario usuario = obtener(id);
		Persona persona = usuario.getPersona();

		if (emailSolicitante != null
				&& emailSolicitante.trim().equalsIgnoreCase(persona.getEmail())) {
			throw new OperacionNoPermitidaException(
					"No puedes cambiar tu propio rol desde este modulo");
		}

		persona.setRol(nuevoRol.name());
		return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponseDTO cambiarPassword(Integer id, CambioPasswordDTO request) {
		if (request == null || request.password() == null || request.password().isBlank()) {
			throw new DatoInvalidoException("La contrasena es obligatoria");
		}
		if (request.password().length() < 6 || request.password().length() > 255) {
			throw new DatoInvalidoException("La contrasena debe tener entre 6 y 255 caracteres");
		}
		Usuario usuario = obtener(id);
		usuario.setPassword(codificarPassword(request.password()));
		return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
	}

	// ------------------------------------------------------------------
	// Internos
	// ------------------------------------------------------------------

	private Usuario obtener(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El identificador del usuario es obligatorio");
		}
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"No existe un usuario con el id " + id));
	}

	private String codificarPassword(String password) {
		PasswordEncoder encoder = passwordEncoder.getIfAvailable();
		if (encoder == null) {
			throw new IllegalStateException(
					"Falta configurar un PasswordEncoder (BCrypt) para poder guardar contrasenas");
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
