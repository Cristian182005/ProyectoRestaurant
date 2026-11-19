package com.elhornero.backend.config;

import com.elhornero.backend.entity.ClienteJuridico;
import com.elhornero.backend.entity.ClienteNatural;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.IngredientePlato;
import com.elhornero.backend.entity.Insumo;
import com.elhornero.backend.entity.Mesa;
import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Plato;
import com.elhornero.backend.entity.Proveedor;
import com.elhornero.backend.entity.Usuario;
import com.elhornero.backend.repository.ClienteJuridicoRepository;
import com.elhornero.backend.repository.ClienteNaturalRepository;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.MesaRepository;
import com.elhornero.backend.repository.PersonaRepository;
import com.elhornero.backend.repository.PlatoRepository;
import com.elhornero.backend.repository.ProveedorRepository;
import com.elhornero.backend.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

	private record UsuarioSemilla(String email, String password, String nombre, String rol,
			String especialidad, String turno) {
	}

	private record PlatoSemilla(String nombre, String categoria, double precio, String imagen) {
	}

	private record InsumoSemilla(String nombre, double stockActual, double stockMinimo,
			String unidad) {
	}

	private record RecetaSemilla(String plato, String insumo, double cantidad) {
	}

	private record ClienteSemilla(String nombre, String tipoDocumento, String numeroDocumento,
			String email, String telefono, String direccion) {
	}

	private static final List<UsuarioSemilla> USUARIOS = List.of(
			new UsuarioSemilla("admin@elhornero.pe", "Admin123!", "Administrador", "ADMINISTRADOR",
					"Gerente", "TARDE"),
			new UsuarioSemilla("mozo@elhornero.pe", "Mozo123!", "Mozo", "MOZO", "Atencion de salon",
					"MANANA"),
			new UsuarioSemilla("cajero@elhornero.pe", "Cajero123!", "Cajero", "CAJERO", "Caja",
					"TARDE"));

	private static final List<PlatoSemilla> PLATOS = List.of(
			new PlatoSemilla("Lomo Saltado", "Platos principales", 38.0, "lomo-saltado.png"),
			new PlatoSemilla("Ceviche Clasico", "Entradas", 32.0, "ceviche.png"),
			new PlatoSemilla("Aji de Gallina", "Platos principales", 30.0, "aji-gallina.png"),
			new PlatoSemilla("Anticuchos", "Entradas", 28.0, "anticuchos.png"),
			new PlatoSemilla("Tres Leches", "Postres", 16.0, "tres-leches.png"),
			new PlatoSemilla("Chicha Morada", "Bebidas", 9.0, "chicha-morada.png"));

	private static final List<InsumoSemilla> INSUMOS = List.of(
			new InsumoSemilla("Lomo de res", 25.0, 5.0, "KG"),
			new InsumoSemilla("Pescado fresco", 18.0, 4.0, "KG"),
			new InsumoSemilla("Pollo", 20.0, 5.0, "KG"),
			new InsumoSemilla("Papas", 40.0, 10.0, "KG"),
			new InsumoSemilla("Cebolla", 30.0, 8.0, "KG"),
			new InsumoSemilla("Limones", 15.0, 3.0, "KG"));

	private static final List<RecetaSemilla> RECETAS = List.of(
			new RecetaSemilla("Lomo Saltado", "Lomo de res", 0.4),
			new RecetaSemilla("Lomo Saltado", "Papas", 0.3),
			new RecetaSemilla("Lomo Saltado", "Cebolla", 0.2),
			new RecetaSemilla("Ceviche Clasico", "Pescado fresco", 0.5),
			new RecetaSemilla("Ceviche Clasico", "Limones", 0.3),
			new RecetaSemilla("Aji de Gallina", "Pollo", 0.4),
			new RecetaSemilla("Anticuchos", "Lomo de res", 0.3),
			new RecetaSemilla("Tres Leches", "Limones", 0.1),
			new RecetaSemilla("Chicha Morada", "Limones", 0.1));

	private static final List<ClienteSemilla> CLIENTES = List.of(
			new ClienteSemilla("Ana Torres", "DNI", "45678912", "ana.torres@correo.pe",
					"987654321", "Av. Lima 123"),
			new ClienteSemilla("Carlos Ruiz", "DNI", "78945612", "carlos.ruiz@correo.pe",
					"987654322", "Jr. Piura 456"),
			new ClienteSemilla("Distribuidora Sur SAC", "RUC", "20601234567",
					"contacto@distribuidorasur.pe", "987654323", "Av. Industrial 789"));

	private final UsuarioRepository usuarioRepository;
	private final PersonaRepository personaRepository;
	private final EmpleadoRepository empleadoRepository;
	private final MesaRepository mesaRepository;
	private final PlatoRepository platoRepository;
	private final InsumoRepository insumoRepository;
	private final IngredientePlatoRepository ingredientePlatoRepository;
	private final ProveedorRepository proveedorRepository;
	private final ClienteNaturalRepository clienteNaturalRepository;
	private final ClienteJuridicoRepository clienteJuridicoRepository;
	private final PasswordEncoder passwordEncoder;

	public DevDataSeeder(UsuarioRepository usuarioRepository, PersonaRepository personaRepository,
			EmpleadoRepository empleadoRepository, MesaRepository mesaRepository,
			PlatoRepository platoRepository, InsumoRepository insumoRepository,
			IngredientePlatoRepository ingredientePlatoRepository,
			ProveedorRepository proveedorRepository,
			ClienteNaturalRepository clienteNaturalRepository,
			ClienteJuridicoRepository clienteJuridicoRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.personaRepository = personaRepository;
		this.empleadoRepository = empleadoRepository;
		this.mesaRepository = mesaRepository;
		this.platoRepository = platoRepository;
		this.insumoRepository = insumoRepository;
		this.ingredientePlatoRepository = ingredientePlatoRepository;
		this.proveedorRepository = proveedorRepository;
		this.clienteNaturalRepository = clienteNaturalRepository;
		this.clienteJuridicoRepository = clienteJuridicoRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		sembrarUsuarios();
		sembrarMesas();
		Map<String, Plato> platos = sembrarPlatos();
		Map<String, Insumo> insumos = sembrarInsumos();
		sembrarRecetas(platos, insumos);
		sembrarProveedor();
		sembrarClientes();
	}

	private void sembrarUsuarios() {
		for (UsuarioSemilla semilla : USUARIOS) {
			Usuario usuario = usuarioRepository.findByPersonaEmailIgnoreCase(semilla.email())
					.orElse(null);
			if (usuario == null) {
				Persona persona = new Persona();
				persona.setEmail(semilla.email());
				persona.setNombre(semilla.nombre());
				persona.setRol(semilla.rol());
				persona.setTelefono("999888777");
				persona.setDireccion("Av. El Hornero 123");
				usuario = new Usuario();
				usuario.setPersona(persona);
				persona.setUsuario(usuario);
				usuario.setPassword(passwordEncoder.encode(semilla.password()));
				usuario = usuarioRepository.save(usuario);
			} else if (!passwordEncoder.matches(semilla.password(), usuario.getPassword())) {
				usuario.setPassword(passwordEncoder.encode(semilla.password()));
				usuario = usuarioRepository.save(usuario);
			}
			Persona persona = usuario.getPersona();
			persona.setNombre(semilla.nombre());
			persona.setRol(semilla.rol());
			Integer personaId = persona.getId();
			if (empleadoRepository.findById(personaId).isEmpty()) {
				Empleado empleado = new Empleado();
				empleado.setId(personaId);
				empleado.setEspecialidad(semilla.especialidad());
				empleado.setTurno(semilla.turno());
				empleado.setTipoContrato("PERMANENTE");
				empleado.setFechaIngreso(LocalDate.now());
				empleadoRepository.save(empleado);
			}
		}
	}

	private void sembrarMesas() {
		for (int numero = 1; numero <= 4; numero++) {
			if (mesaRepository.existsByNumero(numero)) {
				continue;
			}
			Mesa mesa = new Mesa();
			mesa.setNumero(numero);
			mesa.setCapacidad(4);
			mesa.setEstado("LIBRE");
			mesaRepository.save(mesa);
		}
	}

	private Map<String, Plato> sembrarPlatos() {
		Map<String, Plato> platos = new LinkedHashMap<>();
		for (PlatoSemilla semilla : PLATOS) {
			Plato plato = platoRepository.findByNombreIgnoreCase(semilla.nombre()).orElse(null);
			if (plato == null) {
				plato = new Plato();
				plato.setNombre(semilla.nombre());
				plato.setDescripcion("Plato de la carta de El Hornero");
				plato.setCategoria(semilla.categoria());
				plato.setPrecio(semilla.precio());
				plato.setImagen(semilla.imagen());
				plato.setActivo(true);
				plato = platoRepository.save(plato);
			} else if (!Boolean.TRUE.equals(plato.getActivo())) {
				plato.setActivo(true);
				plato = platoRepository.save(plato);
			}
			platos.put(semilla.nombre(), plato);
		}
		return platos;
	}

	private Map<String, Insumo> sembrarInsumos() {
		Map<String, Insumo> insumos = new LinkedHashMap<>();
		for (InsumoSemilla semilla : INSUMOS) {
			Insumo insumo = insumoRepository.findByNombreIgnoreCase(semilla.nombre()).orElse(null);
			if (insumo == null) {
				insumo = new Insumo();
				insumo.setNombre(semilla.nombre());
				insumo.setDescripcion("Insumo para la carta de El Hornero");
				insumo.setStockActual(semilla.stockActual());
				insumo.setStockMinimo(semilla.stockMinimo());
				insumo.setUnidadMedida(semilla.unidad());
				insumo.setImagen("insumo.png");
				insumo = insumoRepository.save(insumo);
			}
			insumos.put(semilla.nombre(), insumo);
		}
		return insumos;
	}

	private void sembrarRecetas(Map<String, Plato> platos, Map<String, Insumo> insumos) {
		for (RecetaSemilla semilla : RECETAS) {
			Plato plato = platos.get(semilla.plato());
			Insumo insumo = insumos.get(semilla.insumo());
			if (ingredientePlatoRepository.findByPlatoIdAndInsumoId(plato.getId(), insumo.getId())
					.isPresent()) {
				continue;
			}
			IngredientePlato ingrediente = new IngredientePlato();
			ingrediente.setPlato(plato);
			ingrediente.setInsumo(insumo);
			ingrediente.setCantidadRequerida(semilla.cantidad());
			ingredientePlatoRepository.save(ingrediente);
		}
	}

	private void sembrarProveedor() {
		if (proveedorRepository.findByRucIgnoreCase("20601234567").isPresent()) {
			return;
		}
		Proveedor proveedor = new Proveedor();
		proveedor.setNombre("Distribuidora El Hornero SAC");
		proveedor.setRuc("20601234567");
		proveedor.setEmail("ventas@distribuidoraelhornero.pe");
		proveedor.setTelefono("999111222");
		proveedor.setDireccion("Av. Mayorista 456");
		proveedor.setFechaRegistro(LocalDate.now());
		proveedorRepository.save(proveedor);
	}

	private void sembrarClientes() {
		for (ClienteSemilla semilla : CLIENTES) {
			if (personaRepository.existsByNumeroDocumentoIgnoreCase(semilla.numeroDocumento())) {
				continue;
			}
			Persona persona = new Persona();
			persona.setNombre(semilla.nombre());
			persona.setTipoDocumento(semilla.tipoDocumento());
			persona.setNumeroDocumento(semilla.numeroDocumento());
			persona.setEmail(semilla.email());
			persona.setTelefono(semilla.telefono());
			persona.setDireccion(semilla.direccion());
			persona.setRol("RUC".equals(semilla.tipoDocumento()) ? "CLIENTE_JURIDICO"
					: "CLIENTE_NATURAL");
			Persona guardada = personaRepository.save(persona);
			if ("RUC".equals(semilla.tipoDocumento())) {
				ClienteJuridico cliente = new ClienteJuridico();
				cliente.setId(guardada.getId());
				cliente.setFechaRegistro(LocalDate.now());
				clienteJuridicoRepository.save(cliente);
			} else {
				ClienteNatural cliente = new ClienteNatural();
				cliente.setId(guardada.getId());
				cliente.setFechaRegistro(LocalDate.now());
				clienteNaturalRepository.save(cliente);
			}
		}
	}

}
