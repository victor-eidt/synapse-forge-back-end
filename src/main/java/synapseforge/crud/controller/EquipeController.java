package synapseforge.crud.controller;

import jakarta.validation.constraints.NotBlank;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import synapseforge.crud.DTO.Equipe.EquipeResponseDTO;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.service.EquipeService;

import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.IOException;

@RestController
@RequestMapping("/equipes")
public class EquipeController {

    @Autowired
    private EquipeService service;

    @Autowired
    private GridFsTemplate gridFsTemplate;


    // =========================================================
    // CRIAR EQUIPE
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO criar(
            @RequestParam("nome")
            @NotBlank
            String nome,

            @RequestParam(value = "foto", required = false)
            MultipartFile foto,

            @RequestParam(value = "banner", required = false)
            MultipartFile banner,

            Authentication auth
    ) throws IOException {

        String usuarioId =
                (String) auth.getPrincipal();


        // -----------------------------------------------------
        // Verifica se o gerente já possui equipe
        // -----------------------------------------------------

        if (
                service.buscarPorGerenteId(usuarioId)
                        .isPresent()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O gerente já possui uma equipe"
            );
        }


        // -----------------------------------------------------
        // Salva foto no GridFS
        // -----------------------------------------------------

        String fotoFileId = null;

        if (
                foto != null
                        && !foto.isEmpty()
        ) {

            ObjectId id =
                    serviceGridFsStore(
                            foto
                    );

            fotoFileId =
                    id.toHexString();
        }


        // -----------------------------------------------------
        // Salva banner no GridFS
        // -----------------------------------------------------

        String bannerFileId = null;

        if (
                banner != null
                        && !banner.isEmpty()
        ) {

            ObjectId id =
                    serviceGridFsStore(
                            banner
                    );

            bannerFileId =
                    id.toHexString();
        }


        // -----------------------------------------------------
        // Cria equipe
        // -----------------------------------------------------

        Equipe equipe;

        try {

            equipe =
                    service.criar(
                            usuarioId,
                            nome,
                            fotoFileId,
                            bannerFileId
                    );

        } catch (RuntimeException e) {

            // Se algo der errado depois de salvar os arquivos,
            // evita deixar arquivos órfãos no GridFS.

            if (fotoFileId != null) {
                deletarArquivoGridFs(fotoFileId);
            }

            if (bannerFileId != null) {
                deletarArquivoGridFs(bannerFileId);
            }

            throw e;
        }


        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // BUSCAR MINHA EQUIPE
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @GetMapping("/minha")
    public EquipeResponseDTO minhaEquipe(
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Equipe equipe =
                service.buscarPorGerenteId(
                                usuarioId
                        )
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Equipe não encontrada"
                                        )
                        );

        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // BUSCAR EQUIPE POR ID
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')"
    )
    @GetMapping("/{id}")
    public EquipeResponseDTO buscar(
            @PathVariable String id
    ) {

        Equipe equipe =
                service.buscarPorId(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Equipe não encontrada"
                                        )
                        );

        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // ATUALIZAR NOME
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @PutMapping("/{id}")
    public EquipeResponseDTO atualizar(
            @PathVariable String id,

            @RequestBody
            @jakarta.validation.Valid
            synapseforge.crud.DTO.Equipe.EquipeRequestDTO dto,

            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Equipe equipe =
                service.atualizar(
                        id,
                        usuarioId,
                        dto.getNome()
                );

        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // ATUALIZAR FOTO
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @PutMapping(
            value = "/{id}/foto",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO atualizarFoto(
            @PathVariable String id,

            @RequestParam("foto")
            MultipartFile foto,

            Authentication auth
    ) throws IOException {

        if (
                foto == null
                        || foto.isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nenhuma foto foi enviada"
            );
        }

        String usuarioId =
                (String) auth.getPrincipal();


        // Primeiro valida se o gerente pode alterar
        // essa equipe.
        Equipe existente =
                service.buscarPorId(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Equipe não encontrada"
                                        )
                        );

        if (!usuarioId.equals(
                existente.getGerenteId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não possui permissão para alterar esta equipe"
            );
        }


        ObjectId fileId =
                serviceGridFsStore(
                        foto
                );

        try {

            Equipe equipe =
                    service.salvarFoto(
                            id,
                            usuarioId,
                            fileId.toHexString()
                    );

            return service.toResponseDTO(
                    equipe
            );

        } catch (RuntimeException e) {

            deletarArquivoGridFs(
                    fileId.toHexString()
            );

            throw e;
        }
    }


    // =========================================================
    // ATUALIZAR BANNER
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @PutMapping(
            value = "/{id}/banner",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public EquipeResponseDTO atualizarBanner(
            @PathVariable String id,

            @RequestParam("banner")
            MultipartFile banner,

            Authentication auth
    ) throws IOException {

        if (
                banner == null
                        || banner.isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nenhum banner foi enviado"
            );
        }

        String usuarioId =
                (String) auth.getPrincipal();


        // Primeiro valida se o gerente pode alterar
        // essa equipe.
        Equipe existente =
                service.buscarPorId(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Equipe não encontrada"
                                        )
                        );

        if (!usuarioId.equals(
                existente.getGerenteId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não possui permissão para alterar esta equipe"
            );
        }


        ObjectId fileId =
                serviceGridFsStore(
                        banner
                );

        try {

            Equipe equipe =
                    service.salvarBanner(
                            id,
                            usuarioId,
                            fileId.toHexString()
                    );

            return service.toResponseDTO(
                    equipe
            );

        } catch (RuntimeException e) {

            deletarArquivoGridFs(
                    fileId.toHexString()
            );

            throw e;
        }
    }


    // =========================================================
    // REMOVER FOTO
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @DeleteMapping("/{id}/foto")
    public EquipeResponseDTO removerFoto(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Equipe equipe =
                service.removerFoto(
                        id,
                        usuarioId
                );

        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // REMOVER BANNER
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @DeleteMapping("/{id}/banner")
    public EquipeResponseDTO removerBanner(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        Equipe equipe =
                service.removerBanner(
                        id,
                        usuarioId
                );

        return service.toResponseDTO(
                equipe
        );
    }


    // =========================================================
    // DELETAR EQUIPE
    // =========================================================

    @PreAuthorize(
            "hasAnyRole('GERENTE', 'ADMIN')"
    )
    @DeleteMapping("/{id}")
    public void deletar(
            @PathVariable String id,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        service.deletar(
                id,
                usuarioId
        );
    }


    // =========================================================
    // GRIDFS STORE
    // =========================================================

    private ObjectId serviceGridFsStore(
            MultipartFile arquivo
    ) throws IOException {

        return gridFsTemplate.store(
                arquivo.getInputStream(),
                arquivo.getOriginalFilename(),
                arquivo.getContentType()
        );
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
                new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria
                                .where("_id")
                                .is(
                                        new ObjectId(id)
                                )
                )
        );
    }
}