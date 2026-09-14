package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.TipoInsumo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimentoEstoqueRepository extends MongoRepository<MovimentoEstoque, String> {

    Optional<MovimentoEstoque> findByChaveIdempotencia(String chave);

    List<MovimentoEstoque> findByTipoInsumoAndInsumoIdOrderByCriadoEmDesc(TipoInsumo tipoInsumo, String insumoId);

    List<MovimentoEstoque> findByPedidoId(String pedidoId);

    List<MovimentoEstoque> findByCriadoEmBetween(LocalDateTime inicio, LocalDateTime fim);
}
