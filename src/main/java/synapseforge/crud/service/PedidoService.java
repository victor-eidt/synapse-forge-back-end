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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    @Autowired
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {
        // keep objeto3DFileId as the file id (frontend will call the protected endpoint to download)
        String objeto3DFileId = pedido.getObjeto3DFileId();

        List<String> imagensBase64 = null;
        if (pedido.getImagensReferenciaFileIds() != null) {
            imagensBase64 = new java.util.ArrayList<>();
            for (String id : pedido.getImagensReferenciaFileIds()) {
                try {
                    org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
                    com.mongodb.client.gridfs.model.GridFSFile gridFsFile = gridFsTemplate.findOne(new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(oid)));
                    if (gridFsFile == null) {
                        imagensBase64.add(null);
                        continue;
                    }
                    org.springframework.data.mongodb.gridfs.GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
                    java.io.InputStream is = resource.getInputStream();
                    byte[] bytes = is.readAllBytes();

                    String contentType = "application/octet-stream";
                    if (gridFsFile.getMetadata() != null) {
                        if (gridFsFile.getMetadata().getString("contentType") != null) {
                            contentType = gridFsFile.getMetadata().getString("contentType");
                        } else if (gridFsFile.getMetadata().getString("_contentType") != null) {
                            contentType = gridFsFile.getMetadata().getString("_contentType");
                        }
                    }

                    String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    imagensBase64.add("data:" + contentType + ";base64," + b64);
                } catch (Exception e) {
                    // on error, add null to keep positions
                    imagensBase64.add(null);
                }
            }
        }

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getProjeto(),
                pedido.getDescricao(),
                pedido.getStatus(),
                pedido.getPrazo(),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm(),
                objeto3DFileId,
                imagensBase64
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

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual == null) {
            throw new RuntimeException("Status do pedido inválido");
        }

        StatusPedido[] valores = StatusPedido.values();
        int indiceAtual = statusAtual.ordinal();

        if (indiceAtual >= valores.length - 1) {
            throw new RuntimeException("Pedido já está finalizado");
        }

        pedido.setStatus(valores[indiceAtual + 1]);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }

    public Pedido regredirStatus(String id, String usuarioId) {
        Pedido pedido = repository.findById(id)
                .filter(p -> usuarioId.equals(p.getUsuarioId()))
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual == null) {
            throw new RuntimeException("Status do pedido inválido");
        }

        int indiceAtual = statusAtual.ordinal();

        if (indiceAtual <= 0) {
            throw new RuntimeException("Pedido já está na primeira etapa");
        }

        pedido.setStatus(StatusPedido.values()[indiceAtual - 1]);
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
