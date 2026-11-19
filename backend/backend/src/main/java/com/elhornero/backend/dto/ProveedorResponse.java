package com.elhornero.backend.dto;

import com.elhornero.backend.entity.Proveedor;

public record ProveedorResponse(Integer id, String nombre, String ruc, String email,
		String telefono, String direccion) {

	public static ProveedorResponse from(Proveedor proveedor) {
		return new ProveedorResponse(proveedor.getId(), proveedor.getNombre(), proveedor.getRuc(),
				proveedor.getEmail(), proveedor.getTelefono(), proveedor.getDireccion());
	}

}
