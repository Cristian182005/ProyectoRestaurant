package com.elhornero.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ClienteProveedorApiTest extends BasePruebaIntegracion {

	@Test
	void listarClientesNaturalesSinDatosDevuelveListaVacia() throws Exception {
		mockMvc.perform(get("/api/clientes/naturales")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/clientes/juridicos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/proveedores")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void crearClienteNaturalGuardaPersonaYCliente() throws Exception {
		MvcResult respuesta = mockMvc
				.perform(post("/api/clientes/naturales").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoCliente("Ana Torres", "DNI", "45678912",
								"ana@correo.pe", "987654321", "Av. Lima 123")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Ana Torres"))
				.andExpect(jsonPath("$.tipoDocumento").value("DNI"))
				.andExpect(jsonPath("$.numeroDocumento").value("45678912"))
				.andExpect(jsonPath("$.email").value("ana@correo.pe"))
				.andExpect(jsonPath("$.telefono").value("987654321"))
				.andExpect(jsonPath("$.direccion").value("Av. Lima 123")).andReturn();
		int id = leerId(respuesta);

		assertThat(personaRepository.findById(id)).hasValueSatisfying(persona -> {
			assertThat(persona.getNombre()).isEqualTo("Ana Torres");
			assertThat(persona.getTipoDocumento()).isEqualTo("DNI");
			assertThat(persona.getNumeroDocumento()).isEqualTo("45678912");
			assertThat(persona.getRol()).isEqualTo("CLIENTE_NATURAL");
		});
		assertThat(clienteNaturalRepository.findById(id)).hasValueSatisfying(
				cliente -> assertThat(cliente.getFechaRegistro()).isEqualTo(LocalDate.now()));
		assertThat(clienteJuridicoRepository.findById(id)).isEmpty();

		mockMvc.perform(get("/api/clientes/naturales")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].numeroDocumento").value("45678912"));
		mockMvc.perform(get("/api/clientes/juridicos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void crearClienteJuridicoGuardaPersonaYCliente() throws Exception {
		MvcResult respuesta = mockMvc
				.perform(post("/api/clientes/juridicos").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoCliente("Comercial Sur SAC", "RUC", "20601234567",
								"comercial@sur.pe", "987111222", "Av. Comercio 456")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Comercial Sur SAC"))
				.andExpect(jsonPath("$.tipoDocumento").value("RUC"))
				.andExpect(jsonPath("$.numeroDocumento").value("20601234567"))
				.andExpect(jsonPath("$.email").value("comercial@sur.pe")).andReturn();
		int id = leerId(respuesta);

		assertThat(personaRepository.findById(id))
				.hasValueSatisfying(persona -> assertThat(persona.getRol())
						.isEqualTo("CLIENTE_JURIDICO"));
		assertThat(clienteJuridicoRepository.findById(id)).hasValueSatisfying(
				cliente -> assertThat(cliente.getFechaRegistro()).isEqualTo(LocalDate.now()));
		assertThat(clienteNaturalRepository.findById(id)).isEmpty();

		mockMvc.perform(get("/api/clientes/juridicos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].numeroDocumento").value("20601234567"));
	}

	@Test
	void crearClienteRechazaDatosInvalidosYDatosDuplicados() throws Exception {
		mockMvc.perform(post("/api/clientes/naturales").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoCliente("", "DNI", "11111111", "sin.nombre@correo.pe", null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre del cliente es obligatorio"));

		mockMvc.perform(post("/api/clientes/naturales").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoCliente("Luis Perez", "DNI", "22222222", "luis@correo.pe", null, null)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/clientes/juridicos").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoCliente("Otra Empresa SAC", "RUC", "22222222",
						"otra@correo.pe", null, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("Ya existe un cliente con el documento '22222222'"));

		mockMvc.perform(post("/api/clientes/naturales").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoCliente("Maria Lopez", "DNI", "33333333", "luis@correo.pe", null,
						null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("Ya existe un cliente con el correo 'luis@correo.pe'"));

		assertThat(personaRepository.count()).isEqualTo(1);
	}

	@Test
	void crearYListarProveedor() throws Exception {
		MvcResult respuesta = mockMvc
				.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoProveedor("Distribuidora Norte SAC", "20654321987",
								"ventas@norte.pe", "999222333", "Av. Industrial 789")))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nombre").value("Distribuidora Norte SAC"))
				.andExpect(jsonPath("$.ruc").value("20654321987"))
				.andExpect(jsonPath("$.email").value("ventas@norte.pe"))
				.andExpect(jsonPath("$.telefono").value("999222333"))
				.andExpect(jsonPath("$.direccion").value("Av. Industrial 789")).andReturn();
		int id = leerId(respuesta);

		assertThat(proveedorRepository.findById(id)).hasValueSatisfying(proveedor -> {
			assertThat(proveedor.getNombre()).isEqualTo("Distribuidora Norte SAC");
			assertThat(proveedor.getRuc()).isEqualTo("20654321987");
			assertThat(proveedor.getEmail()).isEqualTo("ventas@norte.pe");
			assertThat(proveedor.getFechaRegistro()).isEqualTo(LocalDate.now());
		});

		mockMvc.perform(get("/api/proveedores")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].ruc").value("20654321987"));
	}

	@Test
	void crearProveedorRechazaDatosInvalidosYDatosDuplicados() throws Exception {
		mockMvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Sin RUC\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El RUC del proveedor es obligatorio"));

		mockMvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoProveedor("Proveedor Uno", "20123456789", "uno@proveedor.pe", null,
						null)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoProveedor("Proveedor Dos", "20123456789", "dos@proveedor.pe", null,
						null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("Ya existe un proveedor con el RUC '20123456789'"));

		mockMvc.perform(post("/api/proveedores").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoProveedor("Proveedor Tres", "20999999999", "uno@proveedor.pe", null,
						null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje")
						.value("Ya existe un proveedor con el correo 'uno@proveedor.pe'"));

		assertThat(proveedorRepository.count()).isEqualTo(1);
	}

	private String cuerpoCliente(String nombre, String tipoDocumento, String numeroDocumento,
			String email, String telefono, String direccion) {
		return "{\"nombre\":\"" + nombre + "\",\"tipoDocumento\":\"" + tipoDocumento
				+ "\",\"numeroDocumento\":\"" + numeroDocumento + "\",\"email\":\"" + email
				+ "\",\"telefono\":" + valor(telefono) + ",\"direccion\":" + valor(direccion)
				+ "}";
	}

	private String cuerpoProveedor(String nombre, String ruc, String email, String telefono,
			String direccion) {
		return "{\"nombre\":\"" + nombre + "\",\"ruc\":\"" + ruc + "\",\"email\":\"" + email
				+ "\",\"telefono\":" + valor(telefono) + ",\"direccion\":" + valor(direccion) + "}";
	}

	private String valor(String texto) {
		return texto == null ? "null" : "\"" + texto + "\"";
	}

}
