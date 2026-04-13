package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private static final List<StatusPedido> SEQUENCIA = List.of(
            StatusPedido.MODELAGEM,
            StatusPedido.IMPRESSAO,
            StatusPedido.PINTURA,
            StatusPedido.ACABAMENTO,
            StatusPedido.FINALIZADO
    );

    @Autowired
    private PedidoRepository repository;

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

    public Pedido buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + id));
    }

    public Pedido avancarStatus(String id) {
        Pedido pedido = buscarPorId(id);

        int indiceAtual = SEQUENCIA.indexOf(pedido.getStatus());
        if (indiceAtual == SEQUENCIA.size() - 1) {
            throw new RuntimeException("Pedido já está finalizado.");
        }

        pedido.setStatus(SEQUENCIA.get(indiceAtual + 1));
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public Pedido atualizar(String id, Pedido dados) {
        Pedido pedido = buscarPorId(id);
        pedido.setCliente(dados.getCliente());
        pedido.setProjeto(dados.getProjeto());
        pedido.setDescricao(dados.getDescricao());
        pedido.setPrazo(dados.getPrazo());
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public void deletar(String id) {
        buscarPorId(id); // garante que existe
        repository.deleteById(id);
    }
}
