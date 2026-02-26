package com.musabeli.consumidor_monitoreo.services;

import com.musabeli.consumidor_monitoreo.dtos.UbicacionVehiculoDto;
import com.musabeli.consumidor_monitoreo.entities.LogUbicacionVehiculo;
import com.musabeli.consumidor_monitoreo.repositories.LogUbicacionVehiculoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class MonitoreoUbicacionService {

    @Autowired
    private LogUbicacionVehiculoRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "ubicaciones_vehiculos", groupId = "${spring.kafka.consumer.group-id}")
    public void registrarUbicacion(String mensaje) {
        log.info(">>> [MONITOREO] Mensaje recibido desde ubicaciones_vehiculos: {}", mensaje);
        try {
            UbicacionVehiculoDto dto = objectMapper.readValue(mensaje, UbicacionVehiculoDto.class);

            LogUbicacionVehiculo log_entity = LogUbicacionVehiculo.builder()
                    .patente(dto.getPatente())
                    .latitud(dto.getLatitud())
                    .longitud(dto.getLongitud())
                    .fechaActualizacion(dto.getFechaActualizacion())
                    .build();

            repository.save(log_entity);
            log.info(">>> [MONITOREO] Ubicacion guardada en BD para patente: {}", dto.getPatente());

        } catch (Exception e) {
            log.error(">>> [MONITOREO] ERROR al registrar ubicacion: {}", e.getMessage(), e);
        }
    }
}
