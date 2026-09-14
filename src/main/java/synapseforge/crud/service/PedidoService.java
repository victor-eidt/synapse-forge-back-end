package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

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
    private EstoqueService estoqueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;


    // =========================================================
    // CONVERSÃO DTO -> ENTITY
    // =========================================================

    public Pedido toEntity(PedidoRequestDTO dto, String usuarioId) {

        Pedido pedido = new Pedido();

        pedido.setUsuarioId(usuarioId);
        pedido.setClienteId(dto.getClienteId());
        pedido.setCliente(dto.getCliente());
        pedido.setProjeto(dto.getProjeto());
        pedido.setDescricao(dto.getDescricao());
        pedido.setMaterialId(dto.getMaterialId());
        pedido.setVolumeCm3(dto.getVolumeCm3());
        pedido.setTempoImpressaoHoras(dto.getTempoImpressaoHoras());
        pedido.setTempoMaoDeObraHoras(dto.getTempoMaoDeObraHoras());
        pedido.setCustoMaquinaHora(dto.getCustoMaquinaHora());
        pedido.setCustoMaoDeObraHora(dto.getCustoMaoDeObraHora());
        pedido.setMargemLucro(dto.getMargemLucro());
        pedido.setCustoMaterial(dto.getCustoMaterial());
        pedido.setCustoMaquina(dto.getCustoMaquina());
        pedido.setCustoMaoDeObra(dto.getCustoMaoDeObra());
        pedido.setCustoTotal(dto.getCustoTotal());
        pedido.setPrecoFinal(dto.getPrecoFinal());
        pedido.setPrazo(dto.getPrazo());
        pedido.setStatus(dto.getStatus());

        return pedido;
    }


    // =========================================================
    // VALIDAR CLIENTE
    // =========================================================

    private void validarCliente(Pedido pedido) {

        if (pedido.getClienteId() == null ||
                pedido.getClienteId().isBlank()) {

            // Pedido sem cliente vinculado é permitido.
            return;
        }

        User cliente = userRepository.findById(
                pedido.getClienteId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Cliente não encontrado"
                )
        );

        if (cliente.getRole() != Role.CLIENTE) {
            throw new RuntimeException(
                    "O usuário selecionado não possui a role CLIENTE"
            );
        }

        // Garante que o nome salvo no pedido corresponde ao usuário.
        pedido.setCliente(cliente.getNome());
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {

        String objeto3DFileId =
                pedido.getObjeto3DFileId();

        List<String> imagensBase64 = null;

        if (pedido.getImagensReferenciaFileIds() != null) {

            imagensBase64 = new ArrayList<>();

            for (String id :
                    pedido.getImagensReferenciaFileIds()) {

                try {

                    ObjectId oid =
                            new ObjectId(id);

                    com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                            gridFsTemplate.findOne(
                                    new Query(
                                            Criteria.where("_id")
                                                    .is(oid)
                                    )
                            );

                    if (gridFsFile == null) {
                        imagensBase64.add(null);
                        continue;
                    }

                    org.springframework.data.mongodb.gridfs.GridFsResource resource =
                            gridFsTemplate.getResource(
                                    gridFsFile
                            );

                    java.io.InputStream is =
                            resource.getInputStream();

                    byte[] bytes =
                            is.readAllBytes();

                    String contentType =
                            "application/octet-stream";

                    if (gridFsFile.getMetadata() != null) {

                        if (gridFsFile.getMetadata()
                                .getString("contentType") != null) {

                            contentType =
                                    gridFsFile.getMetadata()
                                            .getString("contentType");

                        } else if (
                                gridFsFile.getMetadata()
                                        .getString("_contentType") != null
                        ) {

                            contentType =
                                    gridFsFile.getMetadata()
                                            .getString("_contentType");
                        }
                    }

                    String b64 =
                            java.util.Base64
                                    .getEncoder()
                                    .encodeToString(bytes);

                    imagensBase64.add(
                            "data:"
                                    + contentType
                                    + ";base64,"
                                    + b64
                    );

                } catch (Exception e) {

                    imagensBase64.add(null);
                }
            }
        }

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getCliente(),
                pedido.getProjeto(),
                pedido.getDescricao(),
                pedido.getOrcamentoId(),
                pedido.getMaterialId(),
                pedido.getVolumeCm3(),
                pedido.getTempoImpressaoHoras(),
                pedido.getTempoMaoDeObraHoras(),
                pedido.getCustoMaquinaHora(),
                pedido.getCustoMaoDeObraHora(),
                pedido.getMargemLucro(),
                pedido.getCustoMaterial(),
                pedido.getCustoMaquina(),
                pedido.getCustoMaoDeObra(),
                pedido.getCustoTotal(),
                pedido.getPrecoFinal(),
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

        validarCliente(pedido);

        pedido.setStatus(
                StatusPedido.MODELAGEM
        );

        pedido.setCriadoEm(
                LocalDateTime.now()
        );

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(pedido);
    }


    // =========================================================
    // LISTAR
    // =========================================================
    //
    // CLIENTE -> somente pedidos vinculados ao próprio ID
    // TECNICO -> todos
    // GERENTE -> todos
    // ADMIN -> todos
    //

    public List<Pedido> listar(
            String usuarioId,
            Role role
    ) {

        if (role == Role.CLIENTE) {

            return repository.findByClienteId(
                    usuarioId
            );
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

            return repository.findByClienteIdAndStatus(
                    usuarioId,
                    status
            );
        }

        return repository.findAll()
                .stream()
                .filter(p ->
                        p.getStatus() == status
                )
                .toList();
    }


    // =========================================================
    // BUSCAR POR ID
    // =========================================================
    //
    // CLIENTE -> somente se o pedido estiver vinculado a ele
    // TECNICO / GERENTE / ADMIN -> qualquer pedido
    //

    public Optional<Pedido> buscarPorId(
            String id,
            String usuarioId,
            Role role
    ) {

        if (role == Role.CLIENTE) {

            return repository.findById(id)
                    .filter(p ->
                            usuarioId.equals(
                                    p.getClienteId()
                            )
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

        Pedido pedido =
                buscarPedidoParaEdicao(
                        id,
                        usuarioId,
                        role
                );

        StatusPedido statusAtual =
                pedido.getStatus();

        if (statusAtual == null) {

            throw new RuntimeException(
                    "Status do pedido inválido"
            );
        }
        if (statusAtual == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido cancelado não pode mudar de etapa");
        }

        int indiceAtual = StatusPedido.indiceEtapaProducao(statusAtual);

        if (indiceAtual >= StatusPedido.ETAPAS_PRODUCAO.size() - 1) {
            throw new RuntimeException("Pedido já está finalizado");
        }

        StatusPedido novoStatus = StatusPedido.ETAPAS_PRODUCAO.get(indiceAtual + 1);
        pedido.setStatus(novoStatus);
        // se o estoque for insuficiente a exceção sobe e o pedido não é salvo: a etapa não muda
        estoqueService.baixarPorEtapa(id, novoStatus, usuarioId);
        pedido.setAtualizadoEm(LocalDateTime.now());
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

        Pedido pedido =
                buscarPedidoParaEdicao(
                        id,
                        usuarioId,
                        role
                );

        StatusPedido statusAtual =
                pedido.getStatus();

        if (statusAtual == null) {

            throw new RuntimeException(
                    "Status do pedido inválido"
            );
        }
        if (statusAtual == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido cancelado não pode mudar de etapa");
        }

        int indiceAtual = StatusPedido.indiceEtapaProducao(statusAtual);

        if (indiceAtual <= 0) {

            throw new RuntimeException(
                    "Pedido já está na primeira etapa"
            );
        }

        pedido.setStatus(StatusPedido.ETAPAS_PRODUCAO.get(indiceAtual - 1));
        // o estorno é da etapa abandonada (status atual), não da etapa de destino
        estoqueService.estornarPorEtapa(id, statusAtual, usuarioId);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }


    // =========================================================
    // CANCELAR
    // =========================================================

    public Pedido cancelar(
            String id,
            String usuarioId,
            Role role
    ) {

        Pedido pedido =
                buscarPedidoParaEdicao(
                        id,
                        usuarioId,
                        role
                );

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual == null) {
            throw new RuntimeException("Status do pedido inválido");
        }
        if (statusAtual == StatusPedido.CANCELADO) {
            throw new RuntimeException("Pedido já está cancelado");
        }
        if (statusAtual == StatusPedido.FINALIZADO) {
            throw new RuntimeException("Pedido finalizado não pode ser cancelado");
        }

        // Cancelar NÃO estorna: material já consumido virou peça e não volta à prateleira,
        // e o custo do pedido permanece registrado. Diferente de regredir, que estorna por
        // ser correção de fluxo (a etapa não aconteceu de fato). Etapas nunca alcançadas
        // nunca foram debitadas, então seguem no estoque sem qualquer ação aqui.
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return repository.save(pedido);
    }


    // =========================================================
    // ATUALIZAR
    // =========================================================

    // LIMITAÇÃO CONHECIDA: este método aceita trocar o status diretamente, sem passar pelo
    // gatilho de baixa/estorno de estoque de avancarStatus/regredirStatus — é uma porta
    // lateral que ignora o estoque. Decisão pendente de alinhamento com o grupo.
    public Pedido atualizar(
            String id,
            String usuarioId,
            Role role,
            Pedido dados
    ) {

        Pedido pedido =
                buscarPedidoParaEdicao(
                        id,
                        usuarioId,
                        role
                );

        pedido.setClienteId(
                dados.getClienteId()
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

        copiarDadosOrcamento(pedido, dados);

        if (dados.getStatus() != null) {

            pedido.setStatus(
                    dados.getStatus()
            );
        }

        validarCliente(pedido);

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

        Pedido pedido =
                buscarPedidoParaEdicao(
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
                new HashSet<>(
                        imagensAtuais
                )
        );


        pedido.setClienteId(
                dados.getClienteId()
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

        copiarDadosOrcamento(pedido, dados);

        if (dados.getStatus() != null) {

            pedido.setStatus(
                    dados.getStatus()
            );
        }

        validarCliente(pedido);


        // =====================================================
        // OBJETO 3D
        // =====================================================

        if (
                novoObjeto3DFileId != null
        ) {

            pedido.setObjeto3DFileId(
                    novoObjeto3DFileId
            );

        } else if (
                removerObjeto3D
        ) {

            pedido.setObjeto3DFileId(
                    null
            );
        }


        // =====================================================
        // IMAGENS
        // =====================================================

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


        // =====================================================
        // REMOVE ARQUIVOS ANTIGOS
        // =====================================================

        if (
                (
                        novoObjeto3DFileId != null
                                || removerObjeto3D
                )
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

        Pedido pedido =
                buscarPedidoParaEdicao(
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
                        () ->
                                new RuntimeException(
                                        "Pedido não encontrado"
                                )
                );
    }


    // =========================================================
    // COPIAR DADOS DE ORÇAMENTO
    // =========================================================

    private void copiarDadosOrcamento(Pedido destino, Pedido origem) {
        destino.setMaterialId(origem.getMaterialId());
        destino.setVolumeCm3(origem.getVolumeCm3());
        destino.setTempoImpressaoHoras(origem.getTempoImpressaoHoras());
        destino.setTempoMaoDeObraHoras(origem.getTempoMaoDeObraHoras());
        destino.setCustoMaquinaHora(origem.getCustoMaquinaHora());
        destino.setCustoMaoDeObraHora(origem.getCustoMaoDeObraHora());
        destino.setMargemLucro(origem.getMargemLucro());
        destino.setCustoMaterial(origem.getCustoMaterial());
        destino.setCustoMaquina(origem.getCustoMaquina());
        destino.setCustoMaoDeObra(origem.getCustoMaoDeObra());
        destino.setCustoTotal(origem.getCustoTotal());
        destino.setPrecoFinal(origem.getPrecoFinal());
    }


    // =========================================================
    // DELETAR ARQUIVO DO GRIDFS
    // =========================================================

    private void deletarArquivoGridFs(
            String id
    ) {

        if (
                id == null
                        || !ObjectId.isValid(id)
        ) {

            return;
        }

        gridFsTemplate.delete(
                new Query(
                        Criteria.where("_id")
                                .is(
                                        new ObjectId(id)
                                )
                )
        );
    }
}
