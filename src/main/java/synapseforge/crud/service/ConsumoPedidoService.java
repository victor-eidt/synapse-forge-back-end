package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoResponseDTO;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.repository.ConsumoPedidoRepository;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// A ficha de consumo é da equipe do pedido; pedido e insumos precisam ser da equipe do usuário.
@Service
@RequiredArgsConstructor
public class ConsumoPedidoService {

    private final ConsumoPedidoRepository repository;
    private final PedidoRepository pedidoRepository;
    private final MaterialRepository materialRepository;
    private final CorRepository corRepository;
    private final EquipeContexto equipeContexto;

    public ConsumoPedidoResponseDTO salvar(ConsumoPedidoRequestDTO dto, String usuarioId) {
        validarItensDuplicados(dto);

        String equipeId = equipeContexto.equipeObrigatoria(usuarioId);
        pedidoRepository.findByIdAndEquipeId(dto.getPedidoId(), equipeId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        validarInsumosDaEquipe(dto, equipeId);

        ConsumoPedido consumo = repository.findByPedidoIdAndEquipeId(dto.getPedidoId(), equipeId)
                .orElseGet(() -> {
                    ConsumoPedido novo = new ConsumoPedido();
                    novo.setEquipeId(equipeId);
                    novo.setPedidoId(dto.getPedidoId());
                    novo.setCriadoEm(LocalDateTime.now());
                    return novo;
                });

        consumo.setItens(dto.getItens().stream().map(this::toItem).toList());
        consumo.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(consumo));
    }

    public ConsumoPedidoResponseDTO buscarPorPedido(String pedidoId, String usuarioId) {
        return equipeContexto.equipeDe(usuarioId)
                .flatMap(equipeId -> repository.findByPedidoIdAndEquipeId(pedidoId, equipeId))
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

    // insumo de outra equipe se comporta como inexistente
    private void validarInsumosDaEquipe(ConsumoPedidoRequestDTO dto, String equipeId) {
        for (ItemConsumoRequestDTO item : dto.getItens()) {
            boolean existe = item.getTipoInsumo() == TipoInsumo.MATERIAL
                    ? materialRepository.findByIdAndEquipeId(item.getInsumoId(), equipeId).isPresent()
                    : corRepository.findByIdAndEquipeId(item.getInsumoId(), equipeId).isPresent();
            if (!existe) {
                throw new RuntimeException(item.getTipoInsumo() == TipoInsumo.MATERIAL
                        ? "Material não encontrado" : "Cor não encontrada");
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
