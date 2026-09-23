package synapseforge.crud.controller;

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
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.ConviteEquipeService;
import synapseforge.crud.service.EquipeService;
import synapseforge.crud.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/equipes")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService service;
    private final UserService userService;
    private final ConviteEquipeService conviteEquipeService;

    @Autowired
    private GridFsTemplate gridFsTemplate;


    // =========================================================
    // CRIAR EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
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
    @PostMapping("/{equipeId}/convites/{usuarioId}")
    public ConviteEquipe criarConvite(
            @PathVariable String equipeId,
            @PathVariable String usuarioId,
            Authentication auth
    ) {

        String gerenteId = (String) auth.getPrincipal();

        return conviteEquipeService.criarConvite(
                equipeId,
                gerenteId,
                usuarioId
        );
    }


    // =========================================================
    // LISTAR CONVITES PENDENTES DA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @GetMapping("/{equipeId}/convites")
    public List<ConviteEquipeResponseDTO> listarConvites(
            @PathVariable String equipeId,
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

    @GetMapping("/convites/{token}")
    public ConviteEquipeResponseDTO buscarConvite(
            @PathVariable String token
    ) {

        ConviteEquipe convite =
                conviteEquipeService.buscarPorToken(token);

        return conviteEquipeService.toResponseDTO(convite);
    }


    // =========================================================
    // ACEITAR CONVITE
    // =========================================================

    @PostMapping("/convites/{token}/aceitar")
    public UserResponseDTO aceitarConvite(
            @PathVariable String token
    ) {

        User usuario =
                conviteEquipeService.aceitarConvite(token);

        return userService.toResponseDTO(usuario);
    }


    // =========================================================
    // RECUSAR CONVITE
    // =========================================================

    @PostMapping("/convites/{token}/recusar")
    public UserResponseDTO recusarConvite(
            @PathVariable String token
    ) {

        User usuario =
                conviteEquipeService.recusarConvite(token);

        return userService.toResponseDTO(usuario);
    }


    // =========================================================
    // REMOVER INTEGRANTE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @DeleteMapping("/{equipeId}/integrantes/{usuarioId}")
    public UserResponseDTO removerIntegrante(
            @PathVariable String equipeId,
            @PathVariable String usuarioId,
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
    @DeleteMapping("/minha/integrantes")
    public UserResponseDTO sairDaEquipe(
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

        return userService.toResponseDTO(atualizado);
    }


    // =========================================================
    // ATUALIZAR NOME DA EQUIPE
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @PutMapping("/{id}")
    public EquipeResponseDTO atualizar(
            @PathVariable String id,
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
    @PostMapping(
            value = "/{id}/foto",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO uploadFoto(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
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
    @DeleteMapping("/{id}/foto")
    public EquipeResponseDTO removerFoto(
            @PathVariable String id,
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
    @PostMapping(
            value = "/{id}/banner",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO uploadBanner(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
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
    @DeleteMapping("/{id}/banner")
    public EquipeResponseDTO removerBanner(
            @PathVariable String id,
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable String id,
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
    @PutMapping("/minha/integrantes/{usuarioId}/funcao-visual")
    public UserResponseDTO atualizarFuncaoVisual(
            @PathVariable String usuarioId,
            @RequestBody String funcaoVisual,
            Authentication auth
    ) {

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
}