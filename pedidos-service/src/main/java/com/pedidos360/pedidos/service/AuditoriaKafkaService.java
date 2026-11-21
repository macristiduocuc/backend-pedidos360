package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.AuditoriaKafkaResponse;
import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.kafka.consumer.EventoInvalidoException;
import com.pedidos360.pedidos.model.AuditoriaKafka;
import com.pedidos360.pedidos.repository.AuditoriaKafkaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Historial de auditoría alimentado desde Kafka.
 *
 * Kafka puede entregar un mismo mensaje más de una vez (por ejemplo, tras un reinicio). Por eso cada
 * evento se guarda con su partición y offset, y si ese par ya existe se ignora: el historial no se duplica.
 */
@Service
public class AuditoriaKafkaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaKafkaService.class);

    private final AuditoriaKafkaRepository repositorio;

    public AuditoriaKafkaService(AuditoriaKafkaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void registrar(ConsumerRecord<String, PedidoEvento> registro) {
        PedidoEvento evento = registro.value();
        if (evento == null || evento.pedidoId() == null || evento.tipo() == null) {
            throw new EventoInvalidoException("El evento no trae el id del pedido o su tipo");
        }
        if (repositorio.existsByTopicoAndParticionAndOffsetRegistro(
                registro.topic(), registro.partition(), registro.offset())) {
            log.info("El evento (partición {}, offset {}) ya estaba registrado; se ignora", registro.partition(), registro.offset());
            return;
        }

        repositorio.save(new AuditoriaKafka(
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                evento.tipo().name(), evento.pedidoId(), evento.localId(), evento.estado(), evento.total(),
                evento.ocurridoEn() == null ? null : OffsetDateTime.ofInstant(evento.ocurridoEn(), ZoneOffset.UTC)));
        log.info("Auditoría: evento {} del pedido {} registrado (partición {}, offset {})",
                evento.tipo(), evento.pedidoId(), registro.partition(), registro.offset());
    }

    @Transactional(readOnly = true)
    public List<AuditoriaKafkaResponse> listar(Long pedidoId) {
        List<AuditoriaKafka> registros = pedidoId != null
                ? repositorio.findByPedidoIdOrderByParticionAscOffsetRegistroAsc(pedidoId)
                : repositorio.findTop100ByOrderByIdDesc();
        return registros.stream().map(this::aRespuesta).toList();
    }

    private AuditoriaKafkaResponse aRespuesta(AuditoriaKafka a) {
        return new AuditoriaKafkaResponse(a.getId(), a.getTopico(), a.getParticion(), a.getOffsetRegistro(),
                a.getClave(), a.getTipo(), a.getPedidoId(), a.getLocalId(), a.getEstado(), a.getTotal(),
                a.getOcurridoEn(), a.getRecibidoEn());
    }
}