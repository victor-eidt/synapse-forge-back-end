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

    public Pedido toEntity(PedidoRequestDTO dto) {
        Pedido pedido = new Pedido();
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

    public List<Pedido> listar() {
        return repository.findAll();
    }

    public List<Pedido> listarPorStatus(StatusPedido status) {
        return repository.findByStatus(status);
    }

    public Optional<Pedido> buscarPorId(String id) {
        return repository.findById(id);
    }

    public Pedido avancarStatus(String id) {
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        StatusPedido[] etapas = StatusPedido.values();
        int atual = pedido.getStatus().ordinal();

        if (atual == etapas.length - 1) {
            throw new RuntimeException("Pedido já está finalizado");
        }

        pedido.setStatus(etapas[atual + 1]);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public Pedido atualizar(String id, Pedido pedidoAtualizado) {
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setCliente(pedidoAtualizado.getCliente());
        pedido.setProjeto(pedidoAtualizado.getProjeto());
        pedido.setDescricao(pedidoAtualizado.getDescricao());
        pedido.setPrazo(pedidoAtualizado.getPrazo());
        pedido.setAtualizadoEm(LocalDateTime.now());

        return repository.save(pedido);
    }

    public void deletar(String id) {
        repository.deleteById(id);
    }
}
