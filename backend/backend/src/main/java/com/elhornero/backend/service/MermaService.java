package com.elhornero.backend.service;

import com.elhornero.backend.dto.MermaRequest;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.entity.Merma;
import com.elhornero.backend.exception.DatoInvalidoException;
import com.elhornero.backend.exception.RecursoNoEncontradoException;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.MermaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MermaService {

	private static final String ESTADO_REGISTRADA = "REGISTRADA";

	private final MermaRepository mermaRepository;
	private final InsumoRepository insumoRepository;
	private final EmpleadoRepository empleadoRepository;
	private final InsumoService insumoService;

	public MermaService(MermaRepository mermaRepository, InsumoRepository insumoRepository,
			EmpleadoRepository empleadoRepository, InsumoService insumoService) {
		this.mermaRepository = mermaRepository;
		this.insumoRepository = insumoRepository;
		this.empleadoRepository = empleadoRepository;
		this.insumoService = insumoService;
	}

	@Transactional(readOnly = true)
	public List<Merma> listar() {
		return mermaRepository.findAllByOrderByFechaRegistroDesc();
	}

	@Transactional(readOnly = true)
	public List<Merma> listarPorInsumo(Integer insumoId) {
		if (insumoId == null) {
			throw new DatoInvalidoException("El id del insumo es obligatorio");
		}
		if (!insumoRepository.existsById(insumoId)) {
			throw new RecursoNoEncontradoException("Insumo no encontrado con id " + insumoId);
		}
		return mermaRepository.findByInsumoIdOrderByFechaRegistroDesc(insumoId);
	}

	@Transactional(readOnly = true)
	public List<Merma> listarPorEmpleado(Integer empleadoId) {
		if (empleadoId == null) {
			throw new DatoInvalidoException("El id del empleado es obligatorio");
		}
		if (!empleadoRepository.existsById(empleadoId)) {
			throw new RecursoNoEncontradoException("Empleado no encontrado con id " + empleadoId);
		}
		return mermaRepository.findByEmpleadoIdOrderByFechaRegistroDesc(empleadoId);
	}

	@Transactional(readOnly = true)
	public Merma obtener(Integer id) {
		return buscarMerma(id);
	}

	@Transactional
	public Merma registrar(MermaRequest request) {
		if (request == null) {
			throw new DatoInvalidoException("Los datos de la merma son obligatorios");
		}
		if (request.cantidad() == null || request.cantidad() <= 0) {
			throw new DatoInvalidoException("La cantidad de la merma debe ser mayor a 0");
		}
		if (request.motivo() == null || request.motivo().isBlank()) {
			throw new DatoInvalidoException("El motivo de la merma es obligatorio");
		}
		Insumo insumo = buscarInsumo(request.insumoId());
		Empleado empleado = buscarEmpleado(request.empleadoId());

		insumoService.descontarStock(insumo.getId(), request.cantidad());

		Merma merma = new Merma();
		merma.setInsumo(insumo);
		merma.setCantidad(request.cantidad());
		merma.setMotivo(request.motivo().trim());
		merma.setObservacion(request.observacion());
		merma.setEmpleado(empleado);
		merma.setFechaRegistro(LocalDateTime.now());
		merma.setEstado(ESTADO_REGISTRADA);
		return mermaRepository.save(merma);
	}

	private Insumo buscarInsumo(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El insumo de la merma es obligatorio");
		}
		return insumoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado con id " + id));
	}

	private Empleado buscarEmpleado(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El empleado que registra la merma es obligatorio");
		}
		return empleadoRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"Empleado no encontrado con id " + id));
	}

	private Merma buscarMerma(Integer id) {
		if (id == null) {
			throw new DatoInvalidoException("El id de la merma es obligatorio");
		}
		return mermaRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Merma no encontrada con id " + id));
	}

}
