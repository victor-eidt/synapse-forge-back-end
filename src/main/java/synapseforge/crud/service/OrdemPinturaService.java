package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.OrdemPinturaRepository;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdemPinturaService {

    private final OrdemPinturaRepository repository;
    private final PedidoRepository pedidoRepository;
    private final CorRepository corRepository;
    private final PedidoService pedidoService;

    public List<OrdemPinturaResponseDTO> listar(String usuarioId) {
        return repository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public OrdemPinturaResponseDTO criar(OrdemPinturaRequestDTO dto, String usuarioId) {
        validarRelacionamentos(dto, usuarioId);

        OrdemPintura ordem = new OrdemPintura();
        ordem.setUsuarioId(usuarioId);
        ordem.setPedidoId(dto.getPedidoId());
        ordem.setCorId(dto.getCorId());
        ordem.setTecnico(dto.getTecnico().trim());
        ordem.setPrioridade(dto.getPrioridade());
        ordem.setPrazo(dto.getPrazo());
        ordem.setEtapa(EtapaOrdemPintura.AGUARDANDO);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(ordem));
    }

    public OrdemPinturaResponseDTO atualizarEtapa(
            String id,
            EtapaOrdemPintura etapa,
            String usuarioId
    ) {
        OrdemPintura ordem = buscarDoUsuario(id, usuarioId);
        ordem.setEtapa(etapa);
        ordem.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(ordem));
    }

    public OrdemPinturaResponseDTO atualizar(
            String id,
            OrdemPinturaRequestDTO dto,
            String usuarioId
    ) {
        validarRelacionamentos(dto, usuarioId);
        OrdemPintura ordem = buscarDoUsuario(id, usuarioId);
        ordem.setPedidoId(dto.getPedidoId());
        ordem.setCorId(dto.getCorId());
        ordem.setTecnico(dto.getTecnico().trim());
        ordem.setPrioridade(dto.getPrioridade());
        ordem.setPrazo(dto.getPrazo());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return toResponseDTO(repository.save(ordem));
    }

    public void deletar(String id, String usuarioId) {
        OrdemPintura ordem = buscarDoUsuario(id, usuarioId);
        repository.delete(ordem);
    }

    private OrdemPintura buscarDoUsuario(String id, String usuarioId) {
        return repository.findById(id)
                .filter(ordem -> usuarioId.equals(ordem.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Ordem de pintura nao encontrada"));
    }

    private void validarRelacionamentos(OrdemPinturaRequestDTO dto, String usuarioId) {
        pedidoRepository.findById(dto.getPedidoId())
                .filter(pedido -> usuarioId.equals(pedido.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Pedido nao encontrado"));

        corRepository.findById(dto.getCorId())
                .filter(cor -> usuarioId.equals(cor.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Cor nao encontrada"));

    }

    private OrdemPinturaResponseDTO toResponseDTO(OrdemPintura ordem) {
        Pedido pedido = pedidoRepository.findById(ordem.getPedidoId()).orElse(null);
        Cor cor = corRepository.findById(ordem.getCorId()).orElse(null);

        List<String> referencias = pedido == null
                ? List.of()
                : pedidoService.toResponseDTO(pedido).getImagensReferenciaFileIds();

        return new OrdemPinturaResponseDTO(
                ordem.getId(),
                ordem.getPedidoId(),
                pedido == null ? "Pedido removido" : pedido.getProjeto(),
                pedido == null ? "" : pedido.getCliente(),
                ordem.getCorId(),
                cor == null ? "Cor removida" : cor.getNome(),
                cor == null ? "#D9D9D9" : cor.getHex(),
                cor == null || cor.getAcabamento() == null ? null : cor.getAcabamento().name(),
                ordem.getTecnico(),
                ordem.getPrioridade(),
                ordem.getPrazo(),
                ordem.getEtapa(),
                referencias == null ? List.of() : referencias,
                ordem.getCriadoEm(),
                ordem.getAtualizadoEm()
        );
    }
}
