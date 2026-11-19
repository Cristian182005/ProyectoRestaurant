package com.elhornero.backend.service;

import com.elhornero.backend.dto.ClienteRequest;
import com.elhornero.backend.dto.ClienteResponse;
import com.elhornero.backend.entity.ClienteJuridico;
import com.elhornero.backend.entity.ClienteNatural;
import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.repository.ClienteJuridicoRepository;
import com.elhornero.backend.repository.ClienteNaturalRepository;
import com.elhornero.backend.repository.PersonaRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

	private static final String ROL_NATURAL = "CLIENTE_NATURAL";
	private static final String ROL_JURIDICO = "CLIENTE_JURIDICO";
	private static final Pattern CORREO = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final PersonaRepository personaRepository;
	private final ClienteNaturalRepository clienteNaturalRepository;
	private final ClienteJuridicoRepository clienteJuridicoRepository;

	public ClienteService(PersonaRepository personaRepository,
			ClienteNaturalRepository clienteNaturalRepository,
			ClienteJuridicoRepository clienteJuridicoRepository) {
		this.personaRepository = personaRepository;
		this.clienteNaturalRepository = clienteNaturalRepository;
		this.clienteJuridicoRepository = clienteJuridicoRepository;
	}

	@Transactional(readOnly = true)
	public List<ClienteResponse> listarNaturales() {
		List<Integer> ids = clienteNaturalRepository.findAll().stream().map(ClienteNatural::getId)
				.toList();
		return listarPersonas(ids);
	}

	@Transactional(readOnly = true)
	public List<ClienteResponse> listarJuridicos() {
		List<Integer> ids = clienteJuridicoRepository.findAll().stream().map(ClienteJuridico::getId)
				.toList();
		return listarPersonas(ids);
	}

	@Transactional
	public ClienteResponse crearNatural(ClienteRequest request) {
		return crear(request, ROL_NATURAL);
	}

	@Transactional
	public ClienteResponse crearJuridico(ClienteRequest request) {
		return crear(request, ROL_JURIDICO);
	}

	private List<ClienteResponse> listarPersonas(List<Integer> ids) {
		return personaRepository.findAllById(ids).stream().map(ClienteResponse::from)
				.sorted(Comparator.comparing(ClienteResponse::id)).toList();
	}

	private ClienteResponse crear(ClienteRequest request, String rol) {
		validar(request);
		String documento = request.numeroDocumento().trim();
		if (personaRepository.existsByNumeroDocumentoIgnoreCase(documento)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un cliente con el documento '" + documento + "'");
		}
		String email = normalizar(request.email());
		if (email != null && personaRepository.existsByEmailIgnoreCase(email)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un cliente con el correo '" + email + "'");
		}

		Persona persona = new Persona();
		persona.setNombre(request.nombre().trim());
		persona.setTipoDocumento(request.tipoDocumento().trim());
		persona.setNumeroDocumento(documento);
		persona.setEmail(email);
		persona.setTelefono(normalizar(request.telefono()));
		persona.setDireccion(normalizar(request.direccion()));
		persona.setRol(rol);

		Persona guardada = personaRepository.save(persona);
		if (ROL_NATURAL.equals(rol)) {
			ClienteNatural cliente = new ClienteNatural();
			cliente.setId(guardada.getId());
			cliente.setFechaRegistro(LocalDate.now());
			clienteNaturalRepository.save(cliente);
		} else {
			ClienteJuridico cliente = new ClienteJuridico();
			cliente.setId(guardada.getId());
			cliente.setFechaRegistro(LocalDate.now());
			clienteJuridicoRepository.save(cliente);
		}
		return ClienteResponse.from(guardada);
	}

	private void validar(ClienteRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del cliente son obligatorios");
		}
		if (request.nombre() == null || request.nombre().isBlank()) {
			throw new DatoInvalidoException("El nombre del cliente es obligatorio");
		}
		if (request.nombre().trim().length() > 100) {
			throw new DatoInvalidoException("El nombre del cliente no puede superar 100 caracteres");
		}
		if (request.tipoDocumento() == null || request.tipoDocumento().isBlank()) {
			throw new DatoInvalidoException("El tipo de documento del cliente es obligatorio");
		}
		if (request.tipoDocumento().trim().length() > 20) {
			throw new DatoInvalidoException(
					"El tipo de documento del cliente no puede superar 20 caracteres");
		}
		if (request.numeroDocumento() == null || request.numeroDocumento().isBlank()) {
			throw new DatoInvalidoException("El número de documento del cliente es obligatorio");
		}
		if (request.numeroDocumento().trim().length() > 30) {
			throw new DatoInvalidoException(
					"El número de documento del cliente no puede superar 30 caracteres");
		}
		String email = normalizar(request.email());
		if (email != null) {
			if (email.length() > 150) {
				throw new DatoInvalidoException("El correo no puede superar 150 caracteres");
			}
			if (!CORREO.matcher(email).matches()) {
				throw new DatoInvalidoException("El correo no tiene un formato valido");
			}
		}
		String telefono = normalizar(request.telefono());
		if (telefono != null && telefono.length() > 20) {
			throw new DatoInvalidoException("El telefono no puede superar 20 caracteres");
		}
		String direccion = normalizar(request.direccion());
		if (direccion != null && direccion.length() > 255) {
			throw new DatoInvalidoException("La direccion no puede superar 255 caracteres");
		}
	}

	private String normalizar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim();
	}

}
