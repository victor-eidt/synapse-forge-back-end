package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository repository;

    @Autowired
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;


    // =========================================================
    // CONVERSÃO DTO -> ENTITY
    // =========================================================

    public Pedido toEntity(PedidoRequestDTO dto, String usuarioId) {

        Pedido pedido = new Pedido();

        pedido.setUsuarioId(usuarioId);
        pedido.setCliente(dto.getCliente());
        pedido.setProjeto(dto.getProjeto());
        pedido.setDescricao(dto.getDescricao());
        pedido.setPrazo(dto.getPrazo());
        pedido.setStatus(dto.getStatus());

        return pedido;
    }


    // =========================================================
    // CONVERSÃO ENTITY -> DTO
    // =========================================================

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {

        String objeto3DFileId = pedido.getObjeto3DFileId();

        List<String> imagensBase64 = null;

        if (pedido.getImagensReferenciaFileIds() != null) {

            imagensBase64 = new ArrayList<>();

            for (String id : pedido.getImagensReferenciaFileIds()) {

                try {

                    ObjectId oid = new ObjectId(id);

                    com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                            gridFsTemplate.findOne(
                                    new Query(
                                            Criteria.where("_id").is(oid)
                                    )
                            );

                    if (gridFsFile == null) {
                        imagensBase64.add(null);
                        continue;
                    }

                    org.springframework.data.mongodb.gridfs.GridFsResource resource =
                            gridFsTemplate.getResource(gridFsFile);

                    java.io.InputStream is = resource.getInputStream();

                    byte[] bytes = is.readAllBytes();

                    String contentType = "application/octet-stream";

                    if (gridFsFile.getMetadata() != null) {

                        if (gridFsFile.getMetadata().getString("contentType") != null) {

                            contentType =
                                    gridFsFile.getMetadata().getString("contentType");

                        } else if (
                                gridFsFile.getMetadata().getString("_contentType") != null
                        ) {

                            contentType =
                                    gridFsFile.getMetadata().getString("_contentType");
                        }
                    }

                    String b64 =
                            java.util.Base64
                                    .getEncoder()
                                    .encodeToString(bytes);

                    imagensBase64.add(
                            "data:" + contentType + ";base64," + b64
                    );

                } catch (Exception e) {

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
                imagensBase64,
                pedido.getImagensReferenciaFileIds()
        );
    }


    // =========================================================
    // CRIAR
    // =========================================================

    public Pedido criar(Pedido pedido) {

        pedido.setStatus(StatusPedido.MODELAGEM);
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setAtualizadoEm(LocalDateTime.now());

        return repository.save(pedido);
    }


    // =========================================================
    // LISTAR
    // =========================================================
    //
    // CLIENTE -> somente seus próprios pedidos
    // TECNICO -> todos
    // GERENTE -> todos
    // ADMIN -> todos
    //

    public List<Pedido> listar(String usuarioId, Role role) {

        if (role == Role.CLIENTE) {
            return repository.findByUsuarioId(usuarioId);
        }

        return repository.findAll();
    }


    // =========================================================
    // LISTAR POR STATUS
    // =========================================================

    public List<Pedido> listarPorStatus(
            String usuarioId,
            Role role,
            StatusPedido status
    ) {

        if (role == Role.CLIENTE) {

            return repository.findByUsuarioIdAndStatus(
                    usuarioId,
                    status
            );
        }

        return repository.findAll()
                .stream()
                .filter(p -> p.getStatus() == status)
                .toList();
    }


    // =========================================================
    // BUSCAR POR ID
    // =========================================================
    //
    // CLIENTE -> somente se for dono
    // FUNCIONÁRIO / GERENTE / ADMIN -> qualquer pedido
    //

    public Optional<Pedido> buscarPorId(
            String id,
            String usuarioId,
            Role role
    ) {

        if (role == Role.CLIENTE) {

            return repository.findById(id)
                    .filter(p ->
                            usuarioId.equals(p.getUsuarioId())
                    );
        }

        return repository.findById(id);
    }


    // =========================================================
    // AVANÇAR STATUS
    // =========================================================

    public Pedido avancarStatus(
            String id,
            String usuarioId,
            Role role
    ) {

        Pedido pedido = buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        StatusPedido statusAtual = pedido.getStatus();

        if (statusAtual == null) {
            throw new RuntimeException("Status do pedido inválido");
        }

        StatusPedido[] valores = StatusPedido.values();

        int indiceAtual = statusAtual.ordinal();

        if (indiceAtual >= valores.length - 1) {
            throw new RuntimeException("Pedido já está finalizado");
        }

        pedido.setStatus(
                valores[indiceAtual + 1]
        );

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(pedido);
    }


    // =========================================================
    // REGREDIR STATUS
    // =========================================================

    public Pedido regredirStatus(
            String id,
            String usuarioId,
            Role role
    ) {

        Pedido pedido = buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        StatusPedido statusAtual = pedido.getStatus();

        if (statusAtual == null) {
            throw new RuntimeException("Status do pedido inválido");
        }

        int indiceAtual = statusAtual.ordinal();

        if (indiceAtual <= 0) {
            throw new RuntimeException(
                    "Pedido já está na primeira etapa"
            );
        }

        pedido.setStatus(
                StatusPedido.values()[indiceAtual - 1]
        );

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(pedido);
    }


    // =========================================================
    // ATUALIZAR
    // =========================================================

    public Pedido atualizar(
            String id,
            String usuarioId,
            Role role,
            Pedido dados
    ) {

        Pedido pedido = buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        pedido.setCliente(dados.getCliente());
        pedido.setProjeto(dados.getProjeto());
        pedido.setDescricao(dados.getDescricao());
        pedido.setPrazo(dados.getPrazo());

        if (dados.getStatus() != null) {
            pedido.setStatus(dados.getStatus());
        }

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(pedido);
    }


    // =========================================================
    // ATUALIZAR COM ARQUIVOS
    // =========================================================

    public Pedido atualizarComArquivos(
            String id,
            String usuarioId,
            Role role,
            Pedido dados,
            String novoObjeto3DFileId,
            boolean removerObjeto3D,
            List<String> novasImagensIds,
            List<String> imagensRemover
    ) {

        Pedido pedido = buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        String objetoAnterior =
                pedido.getObjeto3DFileId();

        List<String> imagensAtuais =
                new ArrayList<>(
                        pedido.getImagensReferenciaFileIds() == null
                                ? List.of()
                                : pedido.getImagensReferenciaFileIds()
                );

        Set<String> idsSolicitados =
                new HashSet<>(
                        imagensRemover == null
                                ? List.of()
                                : imagensRemover
                );

        idsSolicitados.retainAll(
                new HashSet<>(imagensAtuais)
        );


        pedido.setCliente(
                dados.getCliente()
        );

        pedido.setProjeto(
                dados.getProjeto()
        );

        pedido.setDescricao(
                dados.getDescricao()
        );

        pedido.setPrazo(
                dados.getPrazo()
        );

        if (dados.getStatus() != null) {
            pedido.setStatus(
                    dados.getStatus()
            );
        }


        // OBJETO 3D

        if (novoObjeto3DFileId != null) {

            pedido.setObjeto3DFileId(
                    novoObjeto3DFileId
            );

        } else if (removerObjeto3D) {

            pedido.setObjeto3DFileId(null);
        }


        // IMAGENS

        imagensAtuais.removeAll(
                idsSolicitados
        );

        if (novasImagensIds != null) {

            imagensAtuais.addAll(
                    novasImagensIds
            );
        }

        pedido.setImagensReferenciaFileIds(
                imagensAtuais
        );

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );


        Pedido salvo =
                repository.save(pedido);


        // Remove arquivos antigos

        if (
                (novoObjeto3DFileId != null || removerObjeto3D)
                        && objetoAnterior != null
        ) {

            deletarArquivoGridFs(
                    objetoAnterior
            );
        }

        idsSolicitados.forEach(
                this::deletarArquivoGridFs
        );

        return salvo;
    }


    // =========================================================
    // DELETAR
    // =========================================================

    public void deletar(
            String id,
            String usuarioId,
            Role role
    ) {

        Pedido pedido = buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        repository.deleteById(id);

        deletarArquivoGridFs(
                pedido.getObjeto3DFileId()
        );

        if (
                pedido.getImagensReferenciaFileIds()
                        != null
        ) {

            pedido.getImagensReferenciaFileIds()
                    .forEach(
                            this::deletarArquivoGridFs
                    );
        }
    }


    // =========================================================
    // VERIFICAÇÃO DE PERMISSÃO PARA ALTERAÇÃO
    // =========================================================
    //
    // CLIENTE -> não pode alterar
    // TECNICO -> pode
    // GERENTE -> pode
    // ADMIN -> pode
    //

    private Pedido buscarPedidoParaEdicao(
            String id,
            String usuarioId,
            Role role
    ) {

        if (role == Role.CLIENTE) {

            throw new RuntimeException(
                    "Cliente não possui permissão para alterar pedidos"
            );
        }

        return repository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Pedido não encontrado"
                        )
                );
    }


    // =========================================================
    // DELETAR ARQUIVO DO GRIDFS
    // =========================================================

    private void deletarArquivoGridFs(String id) {

        if (
                id == null
                        || !ObjectId.isValid(id)
        ) {
            return;
        }

        gridFsTemplate.delete(
                new Query(
                        Criteria.where("_id")
                                .is(new ObjectId(id))
                )
        );
    }
}