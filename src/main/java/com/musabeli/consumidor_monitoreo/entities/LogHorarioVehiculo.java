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
@Table(name = "LOG_HORARIO_VEHICULO")
public class LogHorarioVehiculo {

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

    @Column(name = "HORA_ACTUALIZACION", nullable = false)
    private LocalDateTime horaActualizacion;

    @Column(name = "HORA_ESTIMADA_PROXIMA_PARADA", nullable = false)
    private LocalDateTime horaEstimadaProximaParada;

    @Column(name = "TURNO", nullable = false, length = 10)
    private String turno;

    @Column(name = "CIUDAD_ORIGEN", nullable = false, length = 50)
    private String ciudadOrigen;

    @Column(name = "CIUDAD_DESTINO", nullable = false, length = 50)
    private String ciudadDestino;
}
