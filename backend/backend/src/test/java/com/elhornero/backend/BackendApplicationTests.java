package com.elhornero.backend;

import org.junit.jupiter.api.Test;

/**
 * Verifica que el contexto de la aplicacion arranca completo contra la base de
 * pruebas aislada (bd_restaurant_test). Extiende la base de pruebas para que se
 * cree la BD de prueba si no existe y para usar el perfil "test", evitando que
 * el contexto toque la base de produccion bd_Restaurant.
 */
class BackendApplicationTests extends BasePruebaIntegracion {

	@Test
	void contextLoads() {
	}

}
