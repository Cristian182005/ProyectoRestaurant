package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elhornero.backend.entity.Persona;
import com.elhornero.backend.entity.Usuario;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Cubre el flujo de autenticacion (login con emision de JWT) y la matriz de
 * autorizacion por rol. El alta de usuarios ya no es publica: vive en
 * UsuarioGestionTest bajo /api/usuarios y solo para ADMINISTRADOR. A
 * diferencia del resto de pruebas, aqui la cadena de seguridad SI esta
 * activa (addFilters = true).
 */
@AutoConfigureMockMvc(addFilters = true)
class LoginSeguridadTest extends BasePruebaIntegracion {

	private static final String CONTRASENA = "secreta123";

	@Autowired
	private PasswordEncoder passwordEncoder;

	// ------------------------------------------------------------------
	// Login
	// ------------------------------------------------------------------

	@Test
	void loginCorrectoEmiteTokenBearer() throws Exception {
		crearUsuarioDirecto("login.ok@elhornero.pe", "MOZO");

		MvcResult login = mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin("login.ok@elhornero.pe", CONTRASENA)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.email").value("login.ok@elhornero.pe"))
				.andExpect(jsonPath("$.rol").value("MOZO"))
				.andExpect(jsonPath("$.expiraEn").isNotEmpty()).andExpect(jsonPath("$.id").isNumber())
				.andReturn();

		String cuerpo = login.getResponse().getContentAsString();
		String token = JsonPath.read(cuerpo, "$.token");
		int id = JsonPath.read(cuerpo, "$.id");
		assertThat(token.split("\\.")).hasSize(3);

		crearEmpleado(id, "Atencion al cliente");
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("login.ok@elhornero.pe", CONTRASENA)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.empleadoId").value(id));
	}

	@Test
	void loginConPasswordIncorrectaDevuelve401() throws Exception {
		crearUsuarioDirecto("login.mala@elhornero.pe", "MOZO");

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("login.mala@elhornero.pe", "otra.password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Correo o contrasena incorrectos"));
	}

	@Test
	void loginConCorreoInexistenteDevuelve401() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("nadie@elhornero.pe", CONTRASENA)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginSinPasswordEsRechazadoPorValidacion() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sin.password.login@elhornero.pe\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contrasena es obligatoria"));
	}

	@Test
	void loginConCorreoInvalidoDevuelve400() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("correo-invalido", CONTRASENA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El correo no tiene un formato valido"));
	}

	// ------------------------------------------------------------------
	// Proteccion de endpoints y matriz de roles
	// ------------------------------------------------------------------

	@Test
	void endpointsProtegidosSinTokenDevuelven401() throws Exception {
		mockMvc.perform(get("/api/platos")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
		mockMvc.perform(get("/api/insumos")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isUnauthorized());
	}

	@Test
	void registroPublicoYaNoExiste() throws Exception {
		// La ruta /registro ya no es publica: sin token la seguridad la
		// rechaza antes de llegar al controlador (que tampoco la expone).
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nuevo@elhornero.pe\",\"password\":\"" + CONTRASENA
						+ "\",\"rol\":\"MOZO\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void lecturaDeCartaPermitidaParaCualquierAutenticado() throws Exception {
		crearUsuarioDirecto("mozo.carta@elhornero.pe", "MOZO");
		String token = login("mozo.carta@elhornero.pe");

		mockMvc.perform(get("/api/platos").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/categorias").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	void inventarioSoloAdministrador() throws Exception {
		crearUsuarioDirecto("mozo.inventario@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.inventario@elhornero.pe", "CAJERO");
		crearUsuarioDirecto("admin.inventario@elhornero.pe", "ADMINISTRADOR");
		String tokenMozo = login("mozo.inventario@elhornero.pe");
		String tokenCajero = login("cajero.inventario@elhornero.pe");
		String tokenAdmin = login("admin.inventario@elhornero.pe");

		mockMvc.perform(get("/api/insumos").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
		mockMvc.perform(get("/api/insumos").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/insumos").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/mermas").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
	}

	@Test
	void salonYPedidosSoloAdministradorOMozo() throws Exception {
		crearUsuarioDirecto("mozo.salon@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.salon@elhornero.pe", "CAJERO");
		String tokenMozo = login("mozo.salon@elhornero.pe");
		String tokenCajero = login("cajero.salon@elhornero.pe");

		mockMvc.perform(get("/api/mesas").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mesas").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
		// El cajero puede consultar pedidos para saber cuales estan entregados
		mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
				.content("{}").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
	}

	@Test
	void comprobantesSoloAdministradorOCajero() throws Exception {
		crearUsuarioDirecto("mozo.caja@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.caja@elhornero.pe", "CAJERO");
		String tokenMozo = login("mozo.caja@elhornero.pe");
		String tokenCajero = login("cajero.caja@elhornero.pe");

		mockMvc.perform(get("/api/comprobantes").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/comprobantes").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
		// el mozo tampoco puede emitir (POST)
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{}").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
		// el cajero pasa la autorizacion y cae en la validacion del servicio (400)
		mockMvc.perform(post("/api/comprobantes").contentType(MediaType.APPLICATION_JSON)
				.content("{}").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void clientesYProveedoresMatrizDeRoles() throws Exception {
		crearUsuarioDirecto("mozo.clientes@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.clientes@elhornero.pe", "CAJERO");
		crearUsuarioDirecto("admin.clientes@elhornero.pe", "ADMINISTRADOR");
		String tokenMozo = login("mozo.clientes@elhornero.pe");
		String tokenCajero = login("cajero.clientes@elhornero.pe");
		String tokenAdmin = login("admin.clientes@elhornero.pe");

		mockMvc.perform(get("/api/clientes/naturales").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/clientes/juridicos").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/clientes/naturales").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/proveedores").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/proveedores").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"X\",\"ruc\":\"1\"}")
				.header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
	}

	@Test
	void escrituraDePlatosSoloAdministrador() throws Exception {
		crearUsuarioDirecto("mozo.platos@elhornero.pe", "MOZO");
		crearUsuarioDirecto("admin.platos@elhornero.pe", "ADMINISTRADOR");
		String tokenMozo = login("mozo.platos@elhornero.pe");
		String tokenAdmin = login("admin.platos@elhornero.pe");

		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Plato Nuevo", "Platos", 10.0, true))
				.header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/platos").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoPlato("Plato Nuevo", "Platos", 10.0, true))
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isCreated());
	}

	@Test
	void tokenFalsoDevuelve401() throws Exception {
		mockMvc.perform(get("/api/platos").header("Authorization",
				bearer("token.totalmente.falso")))
				.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private String login(String email) throws Exception {
		MvcResult resultado = mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin(email, CONTRASENA)))
				.andExpect(status().isOk()).andReturn();
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	/**
	 * Crea un usuario directamente en la BD (via repositorio): el registro
	 * publico ya no existe y el alta real esta en UsuarioGestionTest.
	 */
	private void crearUsuarioDirecto(String email, String rol) {
		Persona persona = new Persona();
		persona.setEmail(email);
		persona.setRol(rol);
		Usuario usuario = new Usuario();
		usuario.setPersona(persona);
		persona.setUsuario(usuario);
		usuario.setPassword(passwordEncoder.encode(CONTRASENA));
		usuarioRepository.save(usuario);
	}

	private String cuerpoLogin(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

}
