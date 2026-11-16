package com.elhornero.backend.service;

import com.elhornero.backend.dto.DetalleIngresoRequest;
import com.elhornero.backend.dto.IngresoRequest;
import com.elhornero.backend.entity.DetalleIngreso;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.IngresoInsumo;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.entity.Proveedor;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.DetalleIngresoRepository;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.IngresoInsumoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.ProveedorRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngresoInsumoService {

	private final IngresoInsumoRepository ingresoInsumoRepository;
	private final DetalleIngresoRepository detalleIngresoRepository;
	private final ProveedorRepository proveedorRepository;
	private final EmpleadoRepository empleadoRepository;
	private final InsumoRepository insumoRepository;
	private final InsumoService insumoService;

	public IngresoInsumoService(IngresoInsumoRepository ingresoInsumoRepository,
			DetalleIngresoRepository detalleIngresoRepository, ProveedorRepository proveedorRepository,
			EmpleadoRepository empleadoRepository, InsumoRepository insumoRepository,
			InsumoService insumoService) {
		this.ingresoInsumoRepository = ingresoInsumoRepository;
		this.detalleIngresoRepository = detalleIngresoRepository;
		this.proveedorRepository = proveedorRepository;
		this.empleadoRepository = empleadoRepository;
		this.insumoRepository = insumoRepository;
		this.insumoService = insumoService;
	}

	@Transactional(readOnly = true)
	public List<IngresoInsumo> listar() {
		return ingresoInsumoRepository.listarConDetalles();
	}

	@Transactional(readOnly = true)
	public IngresoInsumo obtener(Integer id) {
		return buscarIngreso(id);
	}

	@Transactional
	public IngresoInsumo crear(IngresoRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos del ingreso son obligatorios");
		}
		if (request.detalles() == null || request.detalles().isEmpty()) {
			throw new DatoInvalidoException("El ingreso debe tener al menos un detalle");
		}
		Proveedor proveedor = buscarProveedor(request.proveedorId());
		Empleado empleado = buscarEmpleado(request.empleadoId());

		IngresoInsumo ingreso = new IngresoInsumo();
		ingreso.setFechaIngreso(request.fechaIngreso() == null ? LocalDate.now() : request.fechaIngreso());
		ingreso.setProveedor(proveedor);
		ingreso.setEmpleado(empleado);

		for (DetalleIngresoRequest item : request.detalles()) {
			if (item == null || item.insumoId() == null) {
				throw new DatoInvalidoException("Cada detalle del ingreso debe indicar el insumo");
			}
			if (item.cantidad() == null || item.cantidad() <= 0) {
				throw new DatoInvalidoException(
						"La cantidad del insumo '" + item.insumoId() + "' debe ser mayor a 0");
			}
			Insumo insumo = buscarInsumo(item.insumoId());
			DetalleIngreso detalle = new DetalleIngreso();
			detalle.setInsumo(insumo);
			detalle.setCantidad(item.cantidad());
			ingreso.agregarDetalle(detalle);
		}

		IngresoInsumo guardado = ingresoInsumoRepository.save(ingreso);
		for (DetalleIngreso detalle : guardado.getDetalles()) {
			insumoService.aumentarStock(detalle.getInsumo().getId(), detalle.getCantidad());
		}
		return guardado;
	}

	@Transactional(readOnly = true)
	public List<DetalleIngreso> listarDetalles(Integer ingresoId) {
		if (ingresoId == null) {
			throw new DatoInvalidoException("El id del ingreso es obligatorio");
		}
		if (!ingresoInsumoRepository.existsById(ingresoId)) {
			throw new RecursoNoEncontradoException("Ingreso no encontrado con id " + ingresoId);
		}
		return detalleIngresoRepository.findByIngresoInsumoId(ingresoId);
	}

	private Proveedor buscarProveedor(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El proveedor del ingreso es obligatorio");
		}
		return proveedorRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Proveedor no encontrado con id " + id));
	}

	private Empleado buscarEmpleado(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El empleado que registra el ingreso es obligatorio");
		}
		return empleadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Empleado no encontrado con id " + id));
	}

	private Insumo buscarInsumo(Integer id) {
		return insumoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con id " + id));
	}

	private IngresoInsumo buscarIngreso(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id del ingreso es obligatorio");
		}
		IngresoInsumo ingreso = ingresoInsumoRepository.findByIdConDetalles(id);
		if (ingreso == null) {
			throw new RecursoNoEncontradoException("Ingreso no encontrado con id " + id);
		}
		return ingreso;
	}

}
