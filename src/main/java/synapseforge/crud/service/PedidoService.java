package synapseforge.crud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Pedido.PedidoEnvioRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.FormaEnvio;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusEnvio;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDate;
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
    private EquipeContexto equipeContexto;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;

    @Autowired
    private FormaEnvioService formaEnvioService;


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

        // envio: opcional no cadastro; a forma é validada em criar()/atualizar()
        pedido.setFormaEnvioId(limparTexto(dto.getFormaEnvioId()));
        pedido.setCodigoRastreio(limparTexto(dto.getCodigoRastreio()));
        pedido.setDataPostagem(dto.getDataPostagem());
        pedido.setPrevisaoEntrega(dto.getPrevisaoEntrega());

        return pedido;
    }


    // =========================================================
    // VALIDAR CLIENTE
    // =========================================================
    //
    // Cliente não pertence a equipe: basta ser um usuário CLIENTE.
    // O vínculo do cliente com a oficina é o próprio pedido.
    //

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

        // Forma de envio resolvida na leitura: pedido antigo (sem envio) ou forma
        // apagada depois não derrubam a resposta; nome e link ficam nulos.
        Optional<FormaEnvio> formaEnvio =
                formaEnvioService.buscarPorId(pedido.getFormaEnvioId());

        String formaEnvioNome =
                formaEnvio.map(FormaEnvio::getNome).orElse(null);

        String linkRastreio =
                formaEnvio
                        .map(f -> f.montarLinkRastreio(pedido.getCodigoRastreio()))
                        .orElse(null);

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
                pedido.getImagensReferenciaFileIds(),
                pedido.getFormaEnvioId(),
                formaEnvioNome,
                pedido.getCodigoRastreio(),
                pedido.getDataPostagem(),
                pedido.getPrevisaoEntrega(),
                pedido.getStatusEnvio() == null
                        ? StatusEnvio.PREPARANDO
                        : pedido.getStatusEnvio(),
                linkRastreio
        );
    }


    // =========================================================
    // CRIAR
    // =========================================================

    // A equipe sai de quem cria o pedido (pedido.usuarioId = usuário logado).
    public Pedido criar(Pedido pedido) {

        prepararParaCriacao(pedido);

        pedido.setStatus(
                StatusPedido.MODELAGEM
        );

        // envio informado já no cadastro: forma precisa existir para a equipe
        if (pedido.getFormaEnvioId() != null) {
            formaEnvioService.buscarDisponivel(
                    pedido.getFormaEnvioId(),
                    pedido.getEquipeId()
            );
        }
        pedido.setStatusEnvio(
                statusEnvioPara(pedido.getCodigoRastreio(), null, null)
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
    // VALIDAR ANTES DE GRAVAR ARQUIVOS
    // =========================================================
    //
    // Resolve a equipe e valida o cliente. Chamado pelo controller
    // multipart ANTES de gravar no GridFS: requisição recusada não
    // deixa arquivo órfão. criar() repete a validação (não confia
    // em equipeId já preenchido).
    //

    public void prepararParaCriacao(Pedido pedido) {

        pedido.setEquipeId(
                equipeContexto.equipeObrigatoria(
                        pedido.getUsuarioId()
                )
        );

        validarCliente(pedido);
    }

    public void validarAtualizacao(
            String id,
            String usuarioId,
            Role role,
            Pedido dados
    ) {

        buscarPedidoParaEdicao(
                id,
                usuarioId,
                role
        );

        Pedido clienteInformado = new Pedido();
        clienteInformado.setClienteId(dados.getClienteId());
        validarCliente(clienteInformado);
    }


    // =========================================================
    // LISTAR
    // =========================================================
    //
    // CLIENTE -> pedidos vinculados ao próprio ID, de qualquer equipe
    //            (cliente não pertence a equipe; nunca SEM_EQUIPE)
    // TECNICO / GERENTE / ADMIN -> todos da própria equipe
    //                              (sem equipe -> lista vazia)
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

        Optional<String> equipe =
                equipeContexto.equipeDe(usuarioId);

        if (equipe.isEmpty()) {
            return List.of();
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

        if (role == Role.CLIENTE) {

            return repository.findByClienteIdAndStatus(
                    usuarioId,
                    status
            );
        }

        Optional<String> equipe =
                equipeContexto.equipeDe(usuarioId);

        if (equipe.isEmpty()) {
            return List.of();
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
    // CLIENTE -> somente se o pedido estiver vinculado a ele (qualquer equipe)
    // TECNICO / GERENTE / ADMIN -> qualquer pedido da própria equipe
    // Fora disso (outra equipe, sem equipe) -> vazio
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

        return equipeContexto.equipeDe(usuarioId)
                .flatMap(equipeId ->
                        repository.findByIdAndEquipeId(
                                id,
                                equipeId
                        )
                );
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
        Pedido salvo = repository.save(pedido);

        // Só depois de salvo: se a etapa não mudou, o cliente não é avisado.
        // Vale para todas as etapas, inclusive FINALIZADO.
        notificacaoService.notificarEtapaAlterada(salvo);

        return salvo;
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
        Pedido salvo = repository.save(pedido);

        // voltar etapa também é mudança: o cliente vê onde o pedido está agora
        notificacaoService.notificarEtapaAlterada(salvo);

        return salvo;
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

    // A etapa (status) NÃO muda por aqui: só por avancarStatus/regredirStatus/cancelar, que
    // disparam a baixa e o estorno de estoque. Um status no corpo do PUT é ignorado.
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

        copiarDadosEnvio(pedido, dados);

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

        copiarDadosEnvio(pedido, dados);

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
    // REGISTRAR ENVIO
    // =========================================================
    //
    // Substitui o bloco de envio inteiro pelo que veio no PATCH: forma, código,
    // data de postagem e previsão. Com código informado o envio passa a POSTADO
    // (salvo status explícito no corpo); sem código volta a PREPARANDO.
    //

    public Pedido registrarEnvio(
            String id,
            String usuarioId,
            Role role,
            PedidoEnvioRequestDTO dto
    ) {

        Pedido pedido =
                buscarPedidoParaEdicao(
                        id,
                        usuarioId,
                        role
                );

        String formaEnvioId =
                limparTexto(dto.getFormaEnvioId());

        if (formaEnvioId != null) {
            // forma inexistente, inativa ou de outra equipe -> 404
            formaEnvioService.buscarDisponivel(
                    formaEnvioId,
                    pedido.getEquipeId()
            );
        }

        String codigoRastreio =
                limparTexto(dto.getCodigoRastreio());

        pedido.setFormaEnvioId(formaEnvioId);
        pedido.setCodigoRastreio(codigoRastreio);
        pedido.setPrevisaoEntrega(dto.getPrevisaoEntrega());

        // código sem data de postagem: a postagem é hoje
        pedido.setDataPostagem(
                dto.getDataPostagem() == null && codigoRastreio != null
                        ? LocalDate.now()
                        : dto.getDataPostagem()
        );

        pedido.setStatusEnvio(
                statusEnvioPara(
                        codigoRastreio,
                        dto.getStatusEnvio(),
                        pedido.getStatusEnvio()
                )
        );

        pedido.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(pedido);
    }


    // Status do envio resultante: o explícito vence; senão, código informado leva a
    // POSTADO quando o envio ainda estava em preparação (não regride EM_TRANSITO ou
    // ENTREGUE); sem código, PREPARANDO.
    private static StatusEnvio statusEnvioPara(
            String codigoRastreio,
            StatusEnvio explicito,
            StatusEnvio atual
    ) {

        if (explicito != null) {
            return explicito;
        }

        if (codigoRastreio == null) {
            return StatusEnvio.PREPARANDO;
        }

        if (atual == null || atual == StatusEnvio.PREPARANDO) {
            return StatusEnvio.POSTADO;
        }

        return atual;
    }


    // =========================================================
    // COPIAR DADOS DE ENVIO (PUT)
    // =========================================================
    //
    // No PUT só o que veio preenchido sobrescreve: a edição comum do pedido (que
    // não mostra o envio) não pode apagar forma e código já registrados. O
    // formulário multipart nem envia estes campos, então nada muda por ali.
    //

    private void copiarDadosEnvio(Pedido destino, Pedido origem) {

        boolean mudou = false;

        if (origem.getFormaEnvioId() != null) {
            formaEnvioService.buscarDisponivel(
                    origem.getFormaEnvioId(),
                    destino.getEquipeId()
            );
            destino.setFormaEnvioId(origem.getFormaEnvioId());
            mudou = true;
        }

        if (origem.getCodigoRastreio() != null) {
            destino.setCodigoRastreio(origem.getCodigoRastreio());
            mudou = true;
        }

        if (origem.getDataPostagem() != null) {
            destino.setDataPostagem(origem.getDataPostagem());
        }

        if (origem.getPrevisaoEntrega() != null) {
            destino.setPrevisaoEntrega(origem.getPrevisaoEntrega());
        }

        if (mudou || destino.getStatusEnvio() == null) {
            destino.setStatusEnvio(
                    statusEnvioPara(
                            destino.getCodigoRastreio(),
                            null,
                            destino.getStatusEnvio()
                    )
            );
        }
    }


    private static String limparTexto(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
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
