package com.musabeli.consumidor_monitoreo.services;

import com.musabeli.consumidor_monitoreo.dtos.HorarioVehiculoDto;
import com.musabeli.consumidor_monitoreo.entities.LogHorarioVehiculo;
import com.musabeli.consumidor_monitoreo.repositories.LogHorarioVehiculoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class MonitoreoHorarioService {

    @Autowired
    private LogHorarioVehiculoRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "horarios", groupId = "${spring.kafka.consumer.group-id}")
    public void registrarHorario(String mensaje) {
        log.info(">>> [MONITOREO] Mensaje recibido desde horarios: {}", mensaje);
        try {
            HorarioVehiculoDto dto = objectMapper.readValue(mensaje, HorarioVehiculoDto.class);

            LogHorarioVehiculo log_entity = LogHorarioVehiculo.builder()
                    .patente(dto.getPatente())
                    .latitud(dto.getLatitud())
                    .longitud(dto.getLongitud())
                    .horaActualizacion(dto.getHoraActualizacion())
                    .horaEstimadaProximaParada(dto.getHoraEstimadaProximaParada())
                    .turno(dto.getTurno())
                    .ciudadOrigen(dto.getCiudadOrigen())
                    .ciudadDestino(dto.getCiudadDestino())
                    .build();

            repository.save(log_entity);
            log.info(">>> [MONITOREO] Horario guardado en BD para patente: {} | {} -> {}",
                    dto.getPatente(), dto.getCiudadOrigen(), dto.getCiudadDestino());

        } catch (Exception e) {
            log.error(">>> [MONITOREO] ERROR al registrar horario: {}", e.getMessage(), e);
        }
    }
}
