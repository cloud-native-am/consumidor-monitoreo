package com.musabeli.consumidor_monitoreo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "LOG_UBICACION_VEHICULO")
public class LogUbicacionVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PATENTE", nullable = false, length = 10)
    private String patente;

    @Column(name = "LATITUD", nullable = false, precision = 10, scale = 6)
    private BigDecimal latitud;

    @Column(name = "LONGITUD", nullable = false, precision = 10, scale = 6)
    private BigDecimal longitud;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;
}
