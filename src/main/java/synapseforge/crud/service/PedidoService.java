package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository repository;

    public Pedido toEntity(PedidoRequestDTO dto, String usuarioId) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId);
        pedido.setCliente(dto.getCliente());
        pedido.setProjeto(dto.getProjeto());
        pedido.setDescricao(dto.getDescricao());
        pedido.setPrazo(dto.getPrazo());
        return pedido;
    }

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProjeto(),
                pedido.getDescricao(),
                pedido.getStatus(),
                pedido.getPrazo(),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm()
        );
    }

    public Pedido criar(Pedido pedido) {
        pedido.setStatus(StatusPedido.MODELAGEM);
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public List<Pedido> listar(String usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    public List<Pedido> listarPorStatus(String usuarioId, StatusPedido status) {
        return repository.findByUsuarioIdAndStatus(usuarioId, status);
    }

    public Optional<Pedido> buscarPorId(String id, String usuarioId) {
        return repository.findById(id)
                .filter(p -> usuarioId.equals(p.getUsuarioId()));
    }

    public Pedido avancarStatus(String id, String usuarioId) {
        Pedido pedido = repository.findById(id)
                .filter(p -> usuarioId.equals(p.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        StatusPedido[] valores = StatusPedido.values();
        int indiceAtual = pedido.getStatus().ordinal();

        if (indiceAtual >= valores.length - 1) {
            throw new RuntimeException("Pedido já está finalizado");
        }

        pedido.setStatus(valores[indiceAtual + 1]);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public Pedido atualizar(String id, String usuarioId, Pedido dados) {
        Pedido pedido = repository.findById(id)
                .filter(p -> usuarioId.equals(p.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        pedido.setCliente(dados.getCliente());
        pedido.setProjeto(dados.getProjeto());
        pedido.setDescricao(dados.getDescricao());
        pedido.setPrazo(dados.getPrazo());
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public void deletar(String id, String usuarioId) {
        repository.findById(id)
                .filter(p -> usuarioId.equals(p.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        repository.deleteById(id);
    }
}
