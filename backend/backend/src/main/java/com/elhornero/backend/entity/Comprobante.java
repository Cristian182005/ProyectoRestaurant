package com.elhornero.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "comprobante")
public class Comprobante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "fecha_emision")
	private LocalDate fechaEmision;

	@Column(name = "hora_emision")
	private LocalTime horaEmision;

	private Double gravado;

	private Double igv;

	@Column(name = "precio_total")
	private Double precioTotal;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_pago", length = 50)
	private TipoPago tipoPago;

	@Column(name = "vuelto_pago")
	private Double vueltoPago;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cajero_id", nullable = false)
	private Empleado cajero;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pedido_id", nullable = false)
	private Pedido pedido;

	@Column(name = "monto_recibido")
	private Double montoRecibido;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDate getFechaEmision() {
		return fechaEmision;
	}

	public void setFechaEmision(LocalDate fechaEmision) {
		this.fechaEmision = fechaEmision;
	}

	public LocalTime getHoraEmision() {
		return horaEmision;
	}

	public void setHoraEmision(LocalTime horaEmision) {
		this.horaEmision = horaEmision;
	}

	public Double getGravado() {
		return gravado;
	}

	public void setGravado(Double gravado) {
		this.gravado = gravado;
	}

	public Double getIgv() {
		return igv;
	}

	public void setIgv(Double igv) {
		this.igv = igv;
	}

	public Double getPrecioTotal() {
		return precioTotal;
	}

	public void setPrecioTotal(Double precioTotal) {
		this.precioTotal = precioTotal;
	}

	public TipoPago getTipoPago() {
		return tipoPago;
	}

	public void setTipoPago(TipoPago tipoPago) {
		this.tipoPago = tipoPago;
	}

	public Double getVueltoPago() {
		return vueltoPago;
	}

	public void setVueltoPago(Double vueltoPago) {
		this.vueltoPago = vueltoPago;
	}

	public Empleado getCajero() {
		return cajero;
	}

	public void setCajero(Empleado cajero) {
		this.cajero = cajero;
	}

	public Pedido getPedido() {
		return pedido;
	}

	public void setPedido(Pedido pedido) {
		this.pedido = pedido;
	}

	public Double getMontoRecibido() {
		return montoRecibido;
	}

	public void setMontoRecibido(Double montoRecibido) {
		this.montoRecibido = montoRecibido;
	}

}
