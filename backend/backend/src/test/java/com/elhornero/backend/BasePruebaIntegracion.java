package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.entity.ClienteJuridico;
import com.elhornero.backend.entity.ClienteNatural;
import com.elhornero.backend.entity.Empleado;
import com.elhornero.backend.entity.Mesa;
import com.elhornero.backend.entity.Proveedor;
import com.elhornero.backend.repository.BoletaRepository;
import com.elhornero.backend.repository.ClienteJuridicoRepository;
import com.elhornero.backend.repository.ClienteNaturalRepository;
import com.elhornero.backend.repository.ComprobanteRepository;
import com.elhornero.backend.repository.DetalleIngresoRepository;
import com.elhornero.backend.repository.DetallePedidoRepository;
import com.elhornero.backend.repository.EmpleadoRepository;
import com.elhornero.backend.repository.FacturaRepository;
import com.elhornero.backend.repository.IngredientePlatoRepository;
import com.elhornero.backend.repository.IngresoInsumoRepository;
import com.elhornero.backend.repository.InsumoRepository;
import com.elhornero.backend.repository.MermaRepository;
import com.elhornero.backend.repository.MesaRepository;
import com.elhornero.backend.repository.PedidoRepository;
import com.elhornero.backend.repository.PlatoRepository;
import com.elhornero.backend.repository.ProveedorRepository;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base de todas las pruebas de integracion: arranca la aplicacion completa contra
 * la base de datos aislada bd_Restaurant_test, desactiva los filtros de seguridad
 * solo a nivel de MockMvc (sin tocar la configuracion de Spring Security) y limpia
 * los datos de prueba despues de cada caso.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public abstract class BasePruebaIntegracion {

	protected static final String JSON = "application/json";
	protected static final String BASE_DE_PRUEBA = "bd_restaurant_test";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	private Environment environment;

	@Autowired
	protected PlatoRepository platoRepository;

	@Autowired
	protected InsumoRepository insumoRepository;

	@Autowired
	protected MesaRepository mesaRepository;

	@Autowired
	protected EmpleadoRepository empleadoRepository;

	@Autowired
	protected ProveedorRepository proveedorRepository;

	@Autowired
	protected ClienteNaturalRepository clienteNaturalRepository;

	@Autowired
	protected ClienteJuridicoRepository clienteJuridicoRepository;

	@Autowired
	protected PedidoRepository pedidoRepository;

	@Autowired
	protected DetallePedidoRepository detallePedidoRepository;

	@Autowired
	protected ComprobanteRepository comprobanteRepository;

	@Autowired
	protected BoletaRepository boletaRepository;

	@Autowired
	protected FacturaRepository facturaRepository;

	@Autowired
	protected IngredientePlatoRepository ingredientePlatoRepository;

	@Autowired
	protected IngresoInsumoRepository ingresoInsumoRepository;

	@Autowired
	protected DetalleIngresoRepository detalleIngresoRepository;

	@Autowired
	protected MermaRepository mermaRepository;

	@BeforeAll
	static void crearBaseDeDatosDePruebaSiNoExiste() throws Exception {
		try (Connection conexion = DriverManager.getConnection(
				"jdbc:postgresql://localhost:5432/postgres", "postgres", "root");
				Statement sentencia = conexion.createStatement()) {
			// PostgreSQL guarda los nombres de base de datos en minusculas
			ResultSet resultado = sentencia.executeQuery(
					"SELECT 1 FROM pg_database WHERE lower(datname) = lower('"
							+ BASE_DE_PRUEBA + "')");
			if (!resultado.next()) {
				sentencia.executeUpdate("CREATE DATABASE " + BASE_DE_PRUEBA);
			}
		}
	}

	@BeforeEach
	void verificarQueSeUsaLaBaseDePrueba() {
		String url = environment.getRequiredProperty("spring.datasource.url");
		assertThat(url).as("Las pruebas solo pueden ejecutarse contra la base de prueba")
				.contains(BASE_DE_PRUEBA);
	}

	@AfterEach
	void limpiarDatosDePrueba() {
		facturaRepository.deleteAll();
		boletaRepository.deleteAll();
		comprobanteRepository.deleteAll();
		detallePedidoRepository.deleteAll();
		pedidoRepository.deleteAll();
		detalleIngresoRepository.deleteAll();
		ingresoInsumoRepository.deleteAll();
		mermaRepository.deleteAll();
		ingredientePlatoRepository.deleteAll();
		platoRepository.deleteAll();
		insumoRepository.deleteAll();
		mesaRepository.deleteAll();
		clienteNaturalRepository.deleteAll();
		clienteJuridicoRepository.deleteAll();
		proveedorRepository.deleteAll();
		empleadoRepository.deleteAll();
	}

	protected int leerId(MvcResult resultado) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	protected int leerIdDeLista(MvcResult resultado) throws Exception {
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].id");
	}

	protected String cuerpoPlato(String nombre, String categoria, double precio, boolean activo) {
		return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Plato de prueba\","
				+ "\"imagen\":\"plato.png\",\"activo\":" + activo + ",\"categoria\":\"" + categoria
				+ "\",\"precio\":" + precio + "}";
	}

	protected String cuerpoInsumo(String nombre, Double stockActual, double stockMinimo,
			String unidad) {
		return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Insumo de prueba\","
				+ "\"stockActual\":" + (stockActual == null ? "null" : stockActual)
				+ ",\"stockMinimo\":" + stockMinimo + ",\"unidadMedida\":\"" + unidad
				+ "\",\"imagen\":\"insumo.png\"}";
	}

	protected int crearPlato(String nombre, String categoria, double precio, boolean activo)
			throws Exception {
		MvcResult resultado = mockMvc
				.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoPlato(nombre, categoria, precio, activo)))
				.andExpect(status().isCreated()).andReturn();
		return leerId(resultado);
	}

	protected int crearInsumo(String nombre, Double stockActual, double stockMinimo)
			throws Exception {
		MvcResult resultado = mockMvc
				.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoInsumo(nombre, stockActual, stockMinimo, "KG")))
				.andExpect(status().isCreated()).andReturn();
		return leerId(resultado);
	}

	protected int crearMesa(int numero) throws Exception {
		Mesa mesa = new Mesa();
		mesa.setNumero(numero);
		mesa.setCapacidad(4);
		mesa.setEstado("LIBRE");
		return mesaRepository.save(mesa).getId();
	}

	protected Empleado crearEmpleado(int id, String especialidad) {
		Empleado empleado = new Empleado();
		empleado.setId(id);
		empleado.setEspecialidad(especialidad);
		empleado.setTurno("MANANA");
		empleado.setTipoContrato("PERMANENTE");
		empleado.setFechaIngreso(LocalDate.now());
		return empleadoRepository.save(empleado);
	}

	protected Proveedor crearProveedor(int id) {
		Proveedor proveedor = new Proveedor();
		proveedor.setId(id);
		proveedor.setFechaRegistro(LocalDate.now());
		return proveedorRepository.save(proveedor);
	}

	protected ClienteNatural crearClienteNatural(int id) {
		ClienteNatural cliente = new ClienteNatural();
		cliente.setId(id);
		cliente.setFechaRegistro(LocalDate.now());
		return clienteNaturalRepository.save(cliente);
	}

	protected ClienteJuridico crearClienteJuridico(int id) {
		ClienteJuridico cliente = new ClienteJuridico();
		cliente.setId(id);
		cliente.setFechaRegistro(LocalDate.now());
		return clienteJuridicoRepository.save(cliente);
	}

}
