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
 * Cubre el flujo completo de autenticacion: registro publico (solo MOZO y
 * CAJERO), login con emision de JWT y la matriz de autorizacion por rol. A
 * diferencia del resto de pruebas, aqui la cadena de seguridad SI esta
 * activa (addFilters = true).
 */
@AutoConfigureMockMvc(addFilters = true)
class RegistroLoginSeguridadTest extends BasePruebaIntegracion {

	private static final String CONTRASENA = "secreta123";

	@Autowired
	private PasswordEncoder passwordEncoder;

	// ------------------------------------------------------------------
	// Registro
	// ------------------------------------------------------------------

	@Test
	void registrarUsuarioMozoGuardaPasswordConBcrypt() throws Exception {
		MvcResult creacion = mockMvc
				.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoRegistro("mozo.registro@elhornero.pe", "MOZO")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("mozo.registro@elhornero.pe"))
				.andExpect(jsonPath("$.rol").value("MOZO")).andReturn();
		int id = leerId(creacion);

		assertThat(usuarioRepository.findById(id)).hasValueSatisfying(usuario -> {
			// la password no se guarda en claro
			assertThat(usuario.getPassword()).isNotEqualTo(CONTRASENA);
			assertThat(passwordEncoder.matches(CONTRASENA, usuario.getPassword())).isTrue();
		});
	}

	@Test
	void registrarUsuarioCajeroEsPermitido() throws Exception {
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoRegistro("cajero.registro@elhornero.pe", "CAJERO")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CAJERO"));
	}

	@Test
	void registrarUsuarioConRolAdministradorEsRechazado() throws Exception {
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoRegistro("admin.fake@elhornero.pe", "ADMINISTRADOR")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(
						"No se puede registrar un usuario con rol ADMINISTRADOR"));
	}

	@Test
	void registrarUsuarioConRolDesconocidoEsRechazado() throws Exception {
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoRegistro("rol.malo@elhornero.pe", "SUPERVISOR")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registrarSinPasswordEsRechazadoPorValidacion() throws Exception {
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sin.password@elhornero.pe\",\"rol\":\"MOZO\"}"))
				.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------
	// Login
	// ------------------------------------------------------------------

	@Test
	void loginCorrectoEmiteTokenBearer() throws Exception {
		registrar("login.ok@elhornero.pe", "MOZO");

		MvcResult login = mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin("login.ok@elhornero.pe", CONTRASENA)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.email").value("login.ok@elhornero.pe"))
				.andExpect(jsonPath("$.rol").value("MOZO"))
				.andExpect(jsonPath("$.expiraEn").isNotEmpty()).andReturn();

		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
		assertThat(token.split("\\.")).hasSize(3);
	}

	@Test
	void loginConPasswordIncorrectaDevuelve401() throws Exception {
		registrar("login.mala@elhornero.pe", "MOZO");

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
	void lecturaDeCartaPermitidaParaCualquierAutenticado() throws Exception {
		registrar("mozo.carta@elhornero.pe", "MOZO");
		String token = login("mozo.carta@elhornero.pe");

		mockMvc.perform(get("/api/platos").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/categorias").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	void inventarioSoloAdministrador() throws Exception {
		registrar("mozo.inventario@elhornero.pe", "MOZO");
		registrar("cajero.inventario@elhornero.pe", "CAJERO");
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
		registrar("mozo.salon@elhornero.pe", "MOZO");
		registrar("cajero.salon@elhornero.pe", "CAJERO");
		String tokenMozo = login("mozo.salon@elhornero.pe");
		String tokenCajero = login("cajero.salon@elhornero.pe");

		mockMvc.perform(get("/api/mesas").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/mesas").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/pedidos").header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
	}

	@Test
	void comprobantesSoloAdministradorOCajero() throws Exception {
		registrar("mozo.caja@elhornero.pe", "MOZO");
		registrar("cajero.caja@elhornero.pe", "CAJERO");
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
	void escrituraDePlatosSoloAdministrador() throws Exception {
		registrar("mozo.platos@elhornero.pe", "MOZO");
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

	private void registrar(String email, String rol) throws Exception {
		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoRegistro(email, rol))).andExpect(status().isCreated());
	}

	private String login(String email) throws Exception {
		MvcResult resultado = mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin(email, CONTRASENA)))
				.andExpect(status().isOk()).andReturn();
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	/**
	 * Crea un usuario directamente en la BD (via repositorio) para poder
	 * obtener tokens de ADMINISTRADOR, que el registro publico no acepta.
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

	private String cuerpoRegistro(String email, String rol) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + CONTRASENA
				+ "\",\"telefono\":\"999888777\",\"direccion\":\"Av. Test 123\",\"rol\":\""
				+ rol + "\"}";
	}

	private String cuerpoLogin(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

}
