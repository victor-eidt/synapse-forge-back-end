package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import synapseforge.crud.DTO.Equipe.ConviteEquipeResponseDTO;
import synapseforge.crud.DTO.Equipe.EquipeRequestDTO;
import synapseforge.crud.DTO.Equipe.EquipeResponseDTO;
import synapseforge.crud.DTO.Equipe.MeuConviteResponseDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.ConviteEquipeService;
import synapseforge.crud.service.EquipeService;
import synapseforge.crud.service.UserService;
import synapseforge.crud.infrastructure.security.JwtService;

import java.util.List;
import java.util.Map;

@Tag(name = "Equipes", description = "Equipe (oficina): criação, integrantes, convites, foto e banner.")
@RestController
@RequestMapping("/equipes")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService service;
    private final UserService userService;
    private final ConviteEquipeService conviteEquipeService;
    private final JwtService jwtService;

    @Autowired
    private GridFsTemplate gridFsTemplate;


    // =========================================================
    // CRIAR EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Criar equipe", description = "Cria a equipe do gerente logado.")
    @PostMapping
    public ResponseEntity<EquipeResponseDTO> criar(
            @Valid @RequestBody EquipeRequestDTO dto,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        Equipe equipe = service.criar(
                gerenteId,
                dto.getNome(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.toResponseDTO(equipe));
    }


    // =========================================================
    // MINHA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN', 'TECNICO')")
    @Operation(summary = "Minha equipe", description = "Retorna a equipe do usuário logado (gerente ou técnico).")
    @GetMapping("/minha")
    public ResponseEntity<EquipeResponseDTO> minhaEquipe(
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        User usuario = userService.buscarPorId(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        // GERENTE/ADMIN procuram pela equipe que administram
        if (usuario.getRole().name().equals("GERENTE")
                || usuario.getRole().name().equals("ADMIN")) {

            return service.buscarPorGerenteId(usuarioId)
                    .map(equipe ->
                            ResponseEntity.ok(
                                    service.toResponseDTO(equipe)
                            )
                    )
                    .orElseGet(() ->
                            ResponseEntity.notFound().build()
                    );
        }

        // TÉCNICO procura pela equipe vinculada ao seu equipeId
        if (usuario.getEquipeId() == null) {
            return ResponseEntity.notFound().build();
        }

        return service.buscarPorId(usuario.getEquipeId())
                .map(equipe ->
                        ResponseEntity.ok(
                                service.toResponseDTO(equipe)
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }


    // =========================================================
    // INTEGRANTES DA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN', 'TECNICO')")
    @Operation(summary = "Integrantes da minha equipe", description = "Lista os usuários da equipe do usuário logado.")
    @GetMapping("/minha/integrantes")
    public List<UserResponseDTO> listarIntegrantes(
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        User usuario = userService.buscarPorId(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        String equipeId = usuario.getEquipeId();

        // GERENTE/ADMIN não possuem equipeId atualmente.
        if (equipeId == null &&
                (usuario.getRole().name().equals("GERENTE")
                        || usuario.getRole().name().equals("ADMIN"))) {

            equipeId = service.buscarPorGerenteId(usuarioId)
                    .map(Equipe::getId)
                    .orElseThrow(() ->
                            new RuntimeException("Você não possui uma equipe")
                    );
        }

        if (equipeId == null) {
            throw new RuntimeException(
                    "Você não pertence a nenhuma equipe"
            );
        }

        return userService.listarPorEquipeId(equipeId)
                .stream()
                .map(userService::toResponseDTO)
                .toList();
    }


    // =========================================================
    // CRIAR CONVITE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Convidar usuário", description = "Envia um convite por e-mail para o usuário (cliente) entrar na equipe como técnico. O convite expira em 7 dias.")
    @PostMapping("/{equipeId}/convites/{usuarioId}")
    public ConviteEquipeResponseDTO criarConvite(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String equipeId,
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String usuarioId,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        // DTO, não a entidade: a entidade carrega o token do convite, e com ele o
        // gerente poderia aceitar no lugar do convidado (a rota por token é pública).
        return conviteEquipeService.toResponseDTO(
                conviteEquipeService.criarConvite(
                        equipeId,
                        gerenteId,
                        usuarioId
                )
        );
    }


    // =========================================================
    // LISTAR CONVITES PENDENTES DA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Convites pendentes da equipe", description = "Lista os convites ainda não respondidos.")
    @GetMapping("/{equipeId}/convites")
    public List<ConviteEquipeResponseDTO> listarConvites(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String equipeId,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        // Garante que o gerente realmente é dono da equipe
        service.buscarPorId(equipeId)
                .filter(equipe ->
                        gerenteId.equals(equipe.getGerenteId())
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Você não possui permissão para acessar esta equipe"
                        )
                );

        return conviteEquipeService
                .listarConvitesDaEquipe(equipeId)
                .stream()
                .map(conviteEquipeService::toResponseDTO)
                .toList();
    }


    // =========================================================
    // CONSULTAR CONVITE PELO TOKEN
    // =========================================================

    @Operation(summary = "Ver convite pelo token", description = "Rota pública usada pelo link do e-mail.")
    @GetMapping("/convites/{token}")
    public ConviteEquipeResponseDTO buscarConvite(
            @Parameter(description = "Token recebido por e-mail", example = "3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c") @PathVariable String token
    ) {

        ConviteEquipe convite =
                conviteEquipeService.buscarPorToken(token);

        return conviteEquipeService.toResponseDTO(convite);
    }


    // =========================================================
    // ACEITAR CONVITE
    // =========================================================

    @Operation(summary = "Aceitar convite pelo token", description = "Rota pública do link do e-mail: o usuário entra na equipe e passa a ser TECNICO.")
    @PostMapping("/convites/{token}/aceitar")
    public UserResponseDTO aceitarConvite(
            @Parameter(description = "Token recebido por e-mail", example = "3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c") @PathVariable String token
    ) {

        User usuario =
                conviteEquipeService.aceitarConvite(token);

        return userService.toResponseDTO(usuario);
    }


    // =========================================================
    // MEU CONVITE (SYN-101): o convidado responde dentro do app
    // =========================================================
    // Fora de /equipes/convites/** de propósito: aquelas rotas são
    // públicas (link do e-mail); estas exigem login.

    @Operation(summary = "Meu convite pendente", description = "Convite pendente do usuário logado; 204 quando não há.")
    @GetMapping("/meu-convite")
    public ResponseEntity<MeuConviteResponseDTO> meuConvite(
            Authentication auth
    ) {

        return conviteEquipeService
                .buscarConvitePendenteDoUsuario(
                        (String) auth.getPrincipal()
                )
                .map(convite -> new MeuConviteResponseDTO(
                        conviteEquipeService.toResponseDTO(convite),
                        service.buscarPorId(convite.getEquipeId())
                                .map(service::toResponseDTO)
                                .orElse(null)
                ))
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.noContent().build()
                );
    }

    @Operation(summary = "Aceitar meu convite", description = "Aceita o convite pendente; devolve um token novo, já com o papel TECNICO.")
    @ApiResponse(responseCode = "200", description = "Convite aceito: novo token com o papel TECNICO", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"access_token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NzA0YTFjMmU0YjBmODFhMmMzZDRlMDEiLCJyb2xlIjoiR0VSRU5URSJ9.Xq3r8Kp2mZ7vT1nH5yL0wQe4sD9fJ6bA2cG8uR1tY0o\", \"user_id\": \"6704a1c2e4b0f81a2c3d4e02\"}")))
    @PostMapping("/meu-convite/aceitar")
    public Map<String, String> aceitarMeuConvite(
            Authentication auth
    ) {

        User usuario =
                conviteEquipeService.aceitarConviteDoUsuario(
                        (String) auth.getPrincipal()
                );

        // O papel mudou (CLIENTE -> TECNICO): devolve um token com o papel atual.
        return Map.of(
                "access_token",
                jwtService.generateToken(
                        usuario.getId(),
                        usuario.getRole()
                ),
                "user_id",
                usuario.getId()
        );
    }

    @Operation(summary = "Recusar meu convite", description = "Recusa o convite pendente; o gerente é avisado por e-mail.")
    @PostMapping("/meu-convite/recusar")
    public ResponseEntity<Void> recusarMeuConvite(
            Authentication auth
    ) {

        conviteEquipeService.recusarConviteDoUsuario(
                (String) auth.getPrincipal()
        );

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // RECUSAR CONVITE
    // =========================================================

    @Operation(summary = "Recusar convite pelo token", description = "Rota pública do link do e-mail.")
    @PostMapping("/convites/{token}/recusar")
    public UserResponseDTO recusarConvite(
            @Parameter(description = "Token recebido por e-mail", example = "3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c") @PathVariable String token
    ) {

        User usuario =
                conviteEquipeService.recusarConvite(token);

        return userService.toResponseDTO(usuario);
    }


    // =========================================================
    // REMOVER INTEGRANTE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Remover integrante", description = "Tira o usuário da equipe; ele volta a ser CLIENTE.")
    @DeleteMapping("/{equipeId}/integrantes/{usuarioId}")
    public UserResponseDTO removerIntegrante(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String equipeId,
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String usuarioId,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        service.buscarPorId(equipeId)
                .filter(equipe ->
                        gerenteId.equals(equipe.getGerenteId())
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Você não possui permissão para acessar esta equipe"
                        )
                );

        User usuario =
                userService.sairDaEquipe(
                        usuarioId,
                        equipeId
                );

        return userService.toResponseDTO(usuario);
    }


    // =========================================================
    // TÉCNICO SAI DA EQUIPE
    // =========================================================

    @PreAuthorize("hasRole('TECNICO')")
    @Operation(summary = "Sair da equipe", description = "O técnico logado sai da equipe e volta a ser CLIENTE.")
    @ApiResponse(responseCode = "200", description = "Saiu da equipe: novo token com o papel CLIENTE", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"access_token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI2NzA0YTFjMmU0YjBmODFhMmMzZDRlMDEiLCJyb2xlIjoiR0VSRU5URSJ9.Xq3r8Kp2mZ7vT1nH5yL0wQe4sD9fJ6bA2cG8uR1tY0o\", \"user_id\": \"6704a1c2e4b0f81a2c3d4e02\"}")))
    @DeleteMapping("/minha/integrantes")
    public Map<String, String> sairDaEquipe(
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        User usuario = userService.buscarPorId(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        if (usuario.getEquipeId() == null) {
            throw new RuntimeException(
                    "Você não pertence a nenhuma equipe"
            );
        }

        User atualizado =
                userService.sairDaEquipe(
                        usuarioId,
                        usuario.getEquipeId()
                );

        // O papel mudou (TECNICO -> CLIENTE): devolve um token com o papel atual,
        // como o aceitar do convite faz no sentido contrário.
        return Map.of(
                "access_token",
                jwtService.generateToken(
                        atualizado.getId(),
                        atualizado.getRole()
                ),
                "user_id",
                atualizado.getId()
        );
    }


    // =========================================================
    // ATUALIZAR NOME DA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Renomear equipe", description = "Altera o nome da equipe.")
    @PutMapping("/{id}")
    public EquipeResponseDTO atualizar(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            @Valid @RequestBody EquipeRequestDTO dto,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        Equipe equipe =
                service.atualizar(
                        id,
                        gerenteId,
                        dto.getNome()
                );

        return service.toResponseDTO(equipe);
    }


    // =========================================================
    // UPLOAD DA FOTO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Enviar foto da equipe", description = "Upload multipart da foto (campo file).")
    @PostMapping(
            value = "/{id}/foto",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO uploadFoto(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            @Parameter(description = "Arquivo de imagem (PNG/JPG)") @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws Exception {

        String gerenteId = (String) auth.getPrincipal();


        org.bson.Document meta =
                new org.bson.Document();

        meta.put("contentType", file.getContentType());

        Object fileId =
                gridFsTemplate.store(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        meta
                );

        Equipe equipe =
                service.salvarFoto(
                        id,
                        gerenteId,
                        fileId.toString()
                );

        return service.toResponseDTO(equipe);
    }


    // =========================================================
    // REMOVER FOTO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Remover foto da equipe", description = "Apaga a foto atual.")
    @DeleteMapping("/{id}/foto")
    public EquipeResponseDTO removerFoto(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        Equipe equipe =
                service.removerFoto(
                        id,
                        gerenteId
                );

        return service.toResponseDTO(equipe);
    }


    // =========================================================
    // UPLOAD DO BANNER
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Enviar banner da equipe", description = "Upload multipart do banner (campo file).")
    @PostMapping(
            value = "/{id}/banner",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO uploadBanner(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            @Parameter(description = "Arquivo de imagem (PNG/JPG)") @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws Exception {

        String gerenteId = (String) auth.getPrincipal();

        org.bson.Document meta =
                new org.bson.Document();

        meta.put("contentType", file.getContentType());

        Object fileId =
                gridFsTemplate.store(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        meta
                );

        Equipe equipe =
                service.salvarBanner(
                        id,
                        gerenteId,
                        fileId.toString()
                );

        return service.toResponseDTO(equipe);
    }


    // =========================================================
    // REMOVER BANNER
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Remover banner da equipe", description = "Apaga o banner atual.")
    @DeleteMapping("/{id}/banner")
    public EquipeResponseDTO removerBanner(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        Equipe equipe =
                service.removerBanner(
                        id,
                        gerenteId
                );

        return service.toResponseDTO(equipe);
    }


    // =========================================================
    // DELETAR EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Excluir equipe", description = "Remove a equipe; os técnicos voltam a ser CLIENTE e os convites pendentes são cancelados.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @Parameter(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06") @PathVariable String id,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        service.deletar(id, gerenteId);

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // ATUALIZAR FUNÇÃO VISUAL DO INTEGRANTE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Definir função do integrante", description = "Texto exibido para o integrante na página da equipe (ex.: Pintora). Corpo vazio limpa a função.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Função do integrante (texto JSON)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "\"Pintora\"")))
    @PutMapping("/minha/integrantes/{usuarioId}/funcao-visual")
    public UserResponseDTO atualizarFuncaoVisual(
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String usuarioId,
            @RequestBody(required = false) String corpo,
            Authentication auth
    ) {

        // O front manda JSON.stringify(valor): sem isso o banco guardava "Pintor" com aspas.
        String funcaoVisual = textoDoCorpo(corpo);

        String gerenteId =
                (String) auth.getPrincipal();

        User gerente =
                userService.buscarPorId(gerenteId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuário não encontrado"
                                )
                        );

        String equipeId =
                gerente.getEquipeId();

        /*
         * GERENTE/ADMIN normalmente não possuem equipeId.
         * Então buscamos a equipe que administram.
         */
        if (equipeId == null) {

            equipeId =
                    service.buscarPorGerenteId(gerenteId)
                            .map(Equipe::getId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Você não possui uma equipe"
                                    )
                            );
        }

        User integrante =
                userService.buscarPorId(usuarioId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuário não encontrado"
                                )
                        );

        /*
         * Garante que o usuário pertence
         * à equipe que está sendo administrada.
         */
        if (
                integrante.getEquipeId() == null
                        || !integrante.getEquipeId()
                        .equals(equipeId)
        ) {

            throw new RuntimeException(
                    "O usuário não pertence à sua equipe"
            );
        }

        User atualizado =
                userService.atualizarFuncaoVisual(
                        usuarioId,
                        funcaoVisual
                );

        return userService.toResponseDTO(
                atualizado
        );
    }


    /** Aceita o corpo como texto puro ou como string JSON ("Pintor"); vazio vira null. */
    static String textoDoCorpo(String corpo) {
        if (corpo == null) {
            return null;
        }
        String texto = corpo.trim();
        if (texto.length() >= 2 && texto.startsWith("\"") && texto.endsWith("\"")) {
            try {
                texto = new com.fasterxml.jackson.databind.ObjectMapper().readValue(texto, String.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                texto = texto.substring(1, texto.length() - 1);
            }
        }
        return texto.isBlank() ? null : texto.trim();
    }
}
