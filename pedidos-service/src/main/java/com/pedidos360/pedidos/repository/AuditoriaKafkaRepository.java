package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.AuditoriaKafka;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaKafkaRepository extends JpaRepository<AuditoriaKafka, Long> {

    boolean existsByTopicoAndParticionAndOffsetRegistro(String topico, int particion, long offsetRegistro);

    List<AuditoriaKafka> findByPedidoIdOrderByParticionAscOffsetRegistroAsc(Long pedidoId);

    List<AuditoriaKafka> findTop100ByOrderByIdDesc();
}