package com.elhornero.backend.service;

import com.elhornero.backend.dto.ProveedorRequest;
import com.elhornero.backend.dto.ProveedorResponse;
import com.elhornero.backend.entity.Proveedor;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.OperacionNoPermitidaException;
import com.elhornero.backend.repository.ProveedorRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

	private static final Pattern CORREO = Pattern
			.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final ProveedorRepository proveedorRepository;

	public ProveedorService(ProveedorRepository proveedorRepository) {
		this.proveedorRepository = proveedorRepository;
	}

	@Transactional(readOnly = true)
	public List<ProveedorResponse> listar() {
		return proveedorRepository.findAll().stream().map(ProveedorResponse::from).toList();
	}

	@Transactional
	public ProveedorResponse crear(ProveedorRequest request) {
		validar(request);
		String ruc = request.ruc().trim();
		if (proveedorRepository.existsByRucIgnoreCase(ruc)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un proveedor con el RUC '" + ruc + "'");
		}
		String email = normalizar(request.email());
		if (email != null && proveedorRepository.existsByEmailIgnoreCase(email)) {
			throw new OperacionNoPermitidaException(
					"Ya existe un proveedor con el correo '" + email + "'");
		}

		Proveedor proveedor = new Proveedor();
		proveedor.setNombre(request.nombre().trim());
		proveedor.setRuc(ruc);
		proveedor.setEmail(email);
		proveedor.setTelefono(normalizar(request.telefono()));
		proveedor.setDireccion(normalizar(request.direccion()));
		proveedor.setFechaRegistro(LocalDate.now());
		return ProveedorResponse.from(proveedorRepository.save(proveedor));
	}

	private void validar(ProveedorRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del proveedor son obligatorios");
		}
		if (request.nombre() == null || request.nombre().isBlank()) {
			throw new DatoInvalidoException("El nombre del proveedor es obligatorio");
		}
		if (request.nombre().trim().length() > 150) {
			throw new DatoInvalidoException("El nombre del proveedor no puede superar 150 caracteres");
		}
		if (request.ruc() == null || request.ruc().isBlank()) {
			throw new DatoInvalidoException("El RUC del proveedor es obligatorio");
		}
		if (request.ruc().trim().length() > 20) {
			throw new DatoInvalidoException("El RUC del proveedor no puede superar 20 caracteres");
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
