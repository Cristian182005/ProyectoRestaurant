package com.elhornero.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Cubre la gestion de usuarios desde el modulo de administracion:
 * registro, listado, busqueda, edicion, cambio de rol y cambio de
 * contrasena, siempre bajo /api/usuarios y exclusivamente para el rol
 * ADMINISTRADOR. La cadena de seguridad esta activa (addFilters = true).
 */
@AutoConfigureMockMvc(addFilters = true)
class UsuarioGestionTest extends BasePruebaIntegracion {

	private static final String CONTRASENA = "secreta123";

	@Autowired
	private PasswordEncoder passwordEncoder;

	// ------------------------------------------------------------------
	// Proteccion de la ruta
	// ------------------------------------------------------------------

	@Test
	void gestionDeUsuariosSinTokenDevuelve401() throws Exception {
		mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoAlta("sin.token@elhornero.pe", "MOZO")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void soloAdministradorPuedeListarUsuarios() throws Exception {
		crearUsuarioDirecto("admin.lista@elhornero.pe", "ADMINISTRADOR");
		crearUsuarioDirecto("mozo.lista@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.lista@elhornero.pe", "CAJERO");

		mockMvc.perform(get("/api/usuarios").header("Authorization",
				bearer(login("mozo.lista@elhornero.pe"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
		mockMvc.perform(get("/api/usuarios").header("Authorization",
				bearer(login("cajero.lista@elhornero.pe"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/usuarios").header("Authorization",
				bearer(login("admin.lista@elhornero.pe"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void mozoNiCajeroPuedenCrearOEditarUsuarios() throws Exception {
		crearUsuarioDirecto("mozo.gestion@elhornero.pe", "MOZO");
		crearUsuarioDirecto("cajero.gestion@elhornero.pe", "CAJERO");
		crearUsuarioDirecto("admin.gestion@elhornero.pe", "ADMINISTRADOR");
		int victima = crearUsuarioDirecto("usuario.gestion@elhornero.pe", "MOZO");

		String tokenMozo = login("mozo.gestion@elhornero.pe");
		String tokenCajero = login("cajero.gestion@elhornero.pe");

		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoAlta("intruso@elhornero.pe", "ADMINISTRADOR"))
				.header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/usuarios/" + victima + "/rol")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rol\":\"ADMINISTRADOR\"}")
				.header("Authorization", bearer(tokenCajero)))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/usuarios/" + victima + "/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"hackeada123\"}")
				.header("Authorization", bearer(tokenMozo)))
				.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------
	// Alta
	// ------------------------------------------------------------------

	@Test
	void administradorPuedeCrearUsuariosConLosTresRoles() throws Exception {
		crearUsuarioDirecto("admin.alta@elhornero.pe", "ADMINISTRADOR");
		String tokenAdmin = login("admin.alta@elhornero.pe");

		for (String rol : new String[] { "ADMINISTRADOR", "MOZO", "CAJERO" }) {
			String email = "nuevo." + rol.toLowerCase() + "@elhornero.pe";
			mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
					.content(cuerpoAlta(email, rol)).header("Authorization", bearer(tokenAdmin)))
					.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
					.andExpect(jsonPath("$.email").value(email))
					.andExpect(jsonPath("$.rol").value(rol));
		}

		mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void altaDeUsuarioGuardaPasswordConBcryptYNuncaLaDevuelve() throws Exception {
		crearUsuarioDirecto("admin.bcrypt@elhornero.pe", "ADMINISTRADOR");
		String tokenAdmin = login("admin.bcrypt@elhornero.pe");

		MvcResult creacion = mockMvc
				.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoAlta("bcrypt@elhornero.pe", "CAJERO"))
						.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();
		int id = leerId(creacion);

		Usuario usuario = usuarioRepository.findById(id).orElseThrow();
		org.assertj.core.api.Assertions.assertThat(usuario.getPassword()).isNotEqualTo(CONTRASENA);
		org.assertj.core.api.Assertions
				.assertThat(passwordEncoder.matches(CONTRASENA, usuario.getPassword())).isTrue();
	}

	@Test
	void altaConRolNoValidoDevuelve400() throws Exception {
		crearUsuarioDirecto("admin.rolmalo@elhornero.pe", "ADMINISTRADOR");
		String tokenAdmin = login("admin.rolmalo@elhornero.pe");

		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoAlta("rol.malo@elhornero.pe", "SUPERVISOR"))
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void altaSinPasswordDevuelve400() throws Exception {
		crearUsuarioDirecto("admin.sinpw@elhornero.pe", "ADMINISTRADOR");
		String tokenAdmin = login("admin.sinpw@elhornero.pe");

		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"sin.password@elhornero.pe\",\"rol\":\"MOZO\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La contrasena es obligatoria"));
	}

	@Test
	void altaConCorreoDuplicadoDevuelve409() throws Exception {
		crearUsuarioDirecto("admin.duplicado@elhornero.pe", "ADMINISTRADOR");
		crearUsuarioDirecto("duplicado@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.duplicado@elhornero.pe");

		mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoAlta("DUPLICADO@elhornero.pe", "CAJERO"))
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("Ya existe un usuario con el correo 'DUPLICADO@elhornero.pe'"));
	}

	// ------------------------------------------------------------------
	// Listado y busqueda
	// ------------------------------------------------------------------

	@Test
	void buscarUsuariosPorCorreoONombre() throws Exception {
		crearUsuarioDirecto("admin.buscar@elhornero.pe", "ADMINISTRADOR");
		Persona persona = new Persona();
		persona.setEmail("lucia.mendoza@elhornero.pe");
		persona.setNombre("Lucia Mendoza");
		persona.setRol("MOZO");
		Usuario usuario = new Usuario();
		usuario.setPersona(persona);
		persona.setUsuario(usuario);
		usuario.setPassword(passwordEncoder.encode(CONTRASENA));
		usuarioRepository.save(usuario);
		crearUsuarioDirecto("otro@elhornero.pe", "CAJERO");

		String tokenAdmin = login("admin.buscar@elhornero.pe");

		mockMvc.perform(get("/api/usuarios").param("buscar", "lucia")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Lucia Mendoza"));

		mockMvc.perform(get("/api/usuarios").param("buscar", "OTRO@")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].email").value("otro@elhornero.pe"));
	}

	@Test
	void obtenerUnUsuarioPorId() throws Exception {
		crearUsuarioDirecto("admin.obtener@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("consulta@elhornero.pe", "CAJERO");
		String tokenAdmin = login("admin.obtener@elhornero.pe");

		mockMvc.perform(get("/api/usuarios/" + id).header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value("consulta@elhornero.pe"))
				.andExpect(jsonPath("$.rol").value("CAJERO"));

		mockMvc.perform(get("/api/usuarios/999999").header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------
	// Edicion
	// ------------------------------------------------------------------

	@Test
	void actualizarInformacionDelUsuario() throws Exception {
		crearUsuarioDirecto("admin.editar@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("editar@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.editar@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana Torres\",\"email\":\"ana.torres@elhornero.pe\","
						+ "\"telefono\":\"999888777\",\"direccion\":\"Av. Lima 123\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nombre").value("Ana Torres"))
				.andExpect(jsonPath("$.email").value("ana.torres@elhornero.pe"))
				.andExpect(jsonPath("$.telefono").value("999888777"))
				.andExpect(jsonPath("$.direccion").value("Av. Lima 123"))
				.andExpect(jsonPath("$.rol").value("MOZO"));

		mockMvc.perform(get("/api/usuarios").param("buscar", "ana.torres")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void actualizarAUnCorreoYaUsadoDevuelve409() throws Exception {
		crearUsuarioDirecto("admin.editar2@elhornero.pe", "ADMINISTRADOR");
		crearUsuarioDirecto("ocupado@elhornero.pe", "CAJERO");
		int id = crearUsuarioDirecto("libre@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.editar2@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ocupado@elhornero.pe\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isConflict());
	}

	@Test
	void actualizarConCorreoInvalidoDevuelve400() throws Exception {
		crearUsuarioDirecto("admin.editar3@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("edit3@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.editar3@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"no-es-un-correo\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El correo no tiene un formato valido"));
	}

	// ------------------------------------------------------------------
	// Roles
	// ------------------------------------------------------------------

	@Test
	void administradorPuedeCambiarElRolDeOtroUsuario() throws Exception {
		crearUsuarioDirecto("admin.rol@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("cambiar.rol@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.rol@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id + "/rol")
				.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"CAJERO\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.rol").value("CAJERO"));

		mockMvc.perform(put("/api/usuarios/" + id + "/rol")
				.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMINISTRADOR\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
	}

	@Test
	void nadiePuedeCambiarSuPropioRol() throws Exception {
		int idAdmin = crearUsuarioDirecto("admin.propio@elhornero.pe", "ADMINISTRADOR");
		String tokenAdmin = login("admin.propio@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + idAdmin + "/rol")
				.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"MOZO\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje")
						.value("No puedes cambiar tu propio rol desde este modulo"));

		mockMvc.perform(get("/api/usuarios/" + idAdmin).header("Authorization",
				bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
	}

	@Test
	void cambiarRolAUnValorNoValidoDevuelve400() throws Exception {
		crearUsuarioDirecto("admin.rolmalo2@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("rol2@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.rolmalo2@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id + "/rol")
				.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"SUPERVISOR\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------
	// Contrasenas
	// ------------------------------------------------------------------

	@Test
	void cambiarPasswordPermiteEntrarConLaNueva() throws Exception {
		crearUsuarioDirecto("admin.password@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("cambiar.pw@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.password@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id + "/password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"nuevaClave123\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("cambiar.pw@elhornero.pe", "secreta123")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoLogin("cambiar.pw@elhornero.pe", "nuevaClave123")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void cambiarPasswordCortaDevuelve400() throws Exception {
		crearUsuarioDirecto("admin.password2@elhornero.pe", "ADMINISTRADOR");
		int id = crearUsuarioDirecto("pw.corta@elhornero.pe", "MOZO");
		String tokenAdmin = login("admin.password2@elhornero.pe");

		mockMvc.perform(put("/api/usuarios/" + id + "/password")
				.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"123\"}")
				.header("Authorization", bearer(tokenAdmin)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje")
						.value("La contrasena debe tener entre 6 y 255 caracteres"));
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private int crearUsuarioDirecto(String email, String rol) {
		Persona persona = new Persona();
		persona.setEmail(email);
		persona.setRol(rol);
		Usuario usuario = new Usuario();
		usuario.setPersona(persona);
		persona.setUsuario(usuario);
		usuario.setPassword(passwordEncoder.encode(CONTRASENA));
		return usuarioRepository.save(usuario).getId();
	}

	private String login(String email) throws Exception {
		MvcResult resultado = mockMvc
				.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoLogin(email, CONTRASENA)))
				.andExpect(status().isOk()).andReturn();
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private String cuerpoAlta(String email, String rol) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + CONTRASENA
				+ "\",\"nombre\":\"Usuario de prueba\",\"telefono\":\"999888777\","
				+ "\"direccion\":\"Av. Test 123\",\"rol\":\"" + rol + "\"}";
	}

	private String cuerpoLogin(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

}
