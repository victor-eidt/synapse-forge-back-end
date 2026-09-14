package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoResponseDTO;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConsumoPedidoService {

    private final ConsumoPedidoRepository repository;

    public ConsumoPedidoResponseDTO salvar(ConsumoPedidoRequestDTO dto) {
        validarItensDuplicados(dto);

        ConsumoPedido consumo = repository.findByPedidoId(dto.getPedidoId())
                .orElseGet(() -> {
                    ConsumoPedido novo = new ConsumoPedido();
                    novo.setPedidoId(dto.getPedidoId());
                    novo.setCriadoEm(LocalDateTime.now());
                    return novo;
                });

        consumo.setItens(dto.getItens().stream().map(this::toItem).toList());
        consumo.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(consumo));
    }

    public ConsumoPedidoResponseDTO buscarPorPedido(String pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Ficha de consumo não encontrada para o pedido"));
    }

    // (tipoInsumo, insumoId, etapa) precisa ser único: é a chave de idempotência da baixa
    private void validarItensDuplicados(ConsumoPedidoRequestDTO dto) {
        Set<String> vistos = new HashSet<>();
        for (ItemConsumoRequestDTO item : dto.getItens()) {
            String chave = item.getTipoInsumo() + ":" + item.getInsumoId() + ":" + item.getEtapaConsumo();
            if (!vistos.add(chave)) {
                throw new IllegalArgumentException(
                        "Item duplicado na ficha para o mesmo insumo e etapa: " + chave);
            }
        }
    }

    private ItemConsumo toItem(ItemConsumoRequestDTO dto) {
        return new ItemConsumo(
                dto.getTipoInsumo(),
                dto.getInsumoId(),
                dto.getQuantidade(),
                dto.getUnidade(),
                dto.getEtapaConsumo()
        );
    }

    private ConsumoPedidoResponseDTO toResponseDTO(ConsumoPedido consumo) {
        return new ConsumoPedidoResponseDTO(
                consumo.getId(),
                consumo.getPedidoId(),
                consumo.getItens().stream()
                        .map(item -> new ItemConsumoResponseDTO(
                                item.getTipoInsumo(),
                                item.getInsumoId(),
                                item.getQuantidade(),
                                item.getUnidade(),
                                item.getEtapaConsumo()))
                        .toList(),
                consumo.getCriadoEm(),
                consumo.getAtualizadoEm()
        );
    }
}
