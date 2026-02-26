package com.musabeli.consumidor_monitoreo.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UbicacionVehiculoDto {
    private String patente;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private LocalDateTime fechaActualizacion;
}
