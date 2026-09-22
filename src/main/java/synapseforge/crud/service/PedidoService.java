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
import java.util.Objects;
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
    private EquipeContexto equipeContexto;

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
    //
    // O cliente vinculado precisa ser da mesma equipe do pedido;
    // cliente de outra equipe se comporta como inexistente.
    // exigirMesmaEquipe=false só na edição que mantém o cliente já
    // vinculado: vínculos anteriores ao isolamento por equipe continuam
    // editáveis sem precisar trocar o cliente.
    //

    private void validarCliente(Pedido pedido) {

        validarCliente(pedido, true);
    }

    private void validarCliente(
            Pedido pedido,
            boolean exigirMesmaEquipe
    ) {

        if (pedido.getClienteId() == null ||
                pedido.getClienteId().isBlank()) {

            // Pedido sem cliente vinculado é permitido.
            return;
        }

        User cliente = userRepository.findById(
                pedido.getClienteId()
        ).filter(u ->
                !exigirMesmaEquipe
                        || (pedido.getEquipeId() != null
                        && pedido.getEquipeId().equals(u.getEquipeId()))
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

                    String b64 =
                            java.util.Base64
                                    .getEncoder()
                                    .encodeToString(bytes);

                    imagensBase64.add(
                            "data:"
                                    + ArquivoUtils.contentType(gridFsFile)
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

    // A equipe sai de quem cria o pedido (pedido.usuarioId = usuário logado).
    public Pedido criar(Pedido pedido) {

        pedido.setEquipeId(
                equipeContexto.equipeObrigatoria(
                        pedido.getUsuarioId()
                )
        );

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
    // Sempre dentro da equipe do usuário (sem equipe -> lista vazia)
    // CLIENTE -> somente pedidos vinculados ao próprio ID
    // TECNICO -> todos da equipe
    // GERENTE -> todos da equipe
    // ADMIN -> todos da equipe
    //

    public List<Pedido> listar(
            String usuarioId,
            Role role
    ) {

        Optional<String> equipe =
                equipeContexto.equipeDe(usuarioId);

        if (equipe.isEmpty()) {
            return List.of();
        }

        if (role == Role.CLIENTE) {

            return repository.findByEquipeIdAndClienteId(
                    equipe.get(),
                    usuarioId
            );
        }

        return repository.findByEquipeId(
                equipe.get()
        );
    }


    // =========================================================
    // LISTAR POR STATUS
    // =========================================================

    public List<Pedido> listarPorStatus(
            String usuarioId,
            Role role,
            StatusPedido status
    ) {

        Optional<String> equipe =
                equipeContexto.equipeDe(usuarioId);

        if (equipe.isEmpty()) {
            return List.of();
        }

        if (role == Role.CLIENTE) {

            return repository.findByEquipeIdAndClienteIdAndStatus(
                    equipe.get(),
                    usuarioId,
                    status
            );
        }

        return repository.findByEquipeIdAndStatus(
                equipe.get(),
                status
        );
    }


    // =========================================================
    // BUSCAR POR ID
    // =========================================================
    //
    // Pedido de outra equipe (ou usuário sem equipe) -> vazio
    // CLIENTE -> somente se o pedido estiver vinculado a ele
    // TECNICO / GERENTE / ADMIN -> qualquer pedido da equipe
    //

    public Optional<Pedido> buscarPorId(
            String id,
            String usuarioId,
            Role role
    ) {

        Optional<Pedido> pedido =
                equipeContexto.equipeDe(usuarioId)
                        .flatMap(equipeId ->
                                repository.findByIdAndEquipeId(
                                        id,
                                        equipeId
                                )
                        );

        if (role == Role.CLIENTE) {

            return pedido
                    .filter(p ->
                            usuarioId.equals(
                                    p.getClienteId()
                            )
                    );
        }

        return pedido;
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

        boolean clienteAlterado =
                !Objects.equals(
                        pedido.getClienteId(),
                        dados.getClienteId()
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

        validarCliente(pedido, clienteAlterado);

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

        boolean clienteAlterado =
                !Objects.equals(
                        pedido.getClienteId(),
                        dados.getClienteId()
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

        validarCliente(pedido, clienteAlterado);


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
    //
    // Perfil primeiro (CLIENTE nunca altera), depois a equipe:
    // sem equipe -> 403 SEM_EQUIPE; pedido de outra equipe -> não encontrado.
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

        String equipeId =
                equipeContexto.equipeObrigatoria(
                        usuarioId
                );

        return repository.findByIdAndEquipeId(id, equipeId)
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
