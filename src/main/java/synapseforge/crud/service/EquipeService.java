package synapseforge.crud.service;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import synapseforge.crud.DTO.Equipe.EquipeResponseDTO;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.repository.EquipeRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class EquipeService {

    @Autowired
    private EquipeRepository repository;

    @Autowired
    private GridFsTemplate gridFsTemplate;


    // =========================================================
    // BUSCAR EQUIPE DO GERENTE
    // =========================================================

    public Optional<Equipe> buscarPorGerenteId(String gerenteId) {

        return repository.findByGerenteId(gerenteId);
    }


    // =========================================================
    // BUSCAR POR ID
    // =========================================================

    public Optional<Equipe> buscarPorId(String id) {

        return repository.findById(id);
    }


    // =========================================================
    // CRIAR EQUIPE
    // =========================================================

    public Equipe criar(
            String gerenteId,
            String nome,
            String fotoFileId,
            String bannerFileId
    ) {

        if (repository.findByGerenteId(gerenteId).isPresent()) {

            throw new RuntimeException(
                    "O gerente já possui uma equipe"
            );
        }

        Equipe equipe = new Equipe();

        equipe.setNome(nome);
        equipe.setGerenteId(gerenteId);

        equipe.setFotoFileId(
                fotoFileId
        );

        equipe.setBannerFileId(
                bannerFileId
        );

        LocalDateTime agora =
                LocalDateTime.now();

        equipe.setCriadoEm(agora);
        equipe.setAtualizadoEm(agora);

        return repository.save(equipe);
    }


    // =========================================================
    // ATUALIZAR EQUIPE
    // =========================================================

    public Equipe atualizar(
            String id,
            String gerenteId,
            String nome
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        equipe.setNome(nome);

        equipe.setAtualizadoEm(
                LocalDateTime.now()
        );

        return repository.save(equipe);
    }


    // =========================================================
    // SALVAR FOTO
    // =========================================================

    public Equipe salvarFoto(
            String id,
            String gerenteId,
            String fileId
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        String fotoAnterior =
                equipe.getFotoFileId();

        equipe.setFotoFileId(fileId);

        equipe.setAtualizadoEm(
                LocalDateTime.now()
        );

        Equipe salva =
                repository.save(equipe);

        if (fotoAnterior != null) {

            deletarArquivoGridFs(
                    fotoAnterior
            );
        }

        return salva;
    }


    // =========================================================
    // SALVAR BANNER
    // =========================================================

    public Equipe salvarBanner(
            String id,
            String gerenteId,
            String fileId
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        String bannerAnterior =
                equipe.getBannerFileId();

        equipe.setBannerFileId(fileId);

        equipe.setAtualizadoEm(
                LocalDateTime.now()
        );

        Equipe salva =
                repository.save(equipe);

        if (bannerAnterior != null) {

            deletarArquivoGridFs(
                    bannerAnterior
            );
        }

        return salva;
    }


    // =========================================================
    // REMOVER FOTO
    // =========================================================

    public Equipe removerFoto(
            String id,
            String gerenteId
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        String fotoAnterior =
                equipe.getFotoFileId();

        equipe.setFotoFileId(null);

        equipe.setAtualizadoEm(
                LocalDateTime.now()
        );

        Equipe salva =
                repository.save(equipe);

        if (fotoAnterior != null) {

            deletarArquivoGridFs(
                    fotoAnterior
            );
        }

        return salva;
    }


    // =========================================================
    // REMOVER BANNER
    // =========================================================

    public Equipe removerBanner(
            String id,
            String gerenteId
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        String bannerAnterior =
                equipe.getBannerFileId();

        equipe.setBannerFileId(null);

        equipe.setAtualizadoEm(
                LocalDateTime.now()
        );

        Equipe salva =
                repository.save(equipe);

        if (bannerAnterior != null) {

            deletarArquivoGridFs(
                    bannerAnterior
            );
        }

        return salva;
    }


    // =========================================================
    // DELETAR EQUIPE
    // =========================================================

    public void deletar(
            String id,
            String gerenteId
    ) {

        Equipe equipe =
                buscarEquipeDoGerente(
                        id,
                        gerenteId
                );

        repository.deleteById(id);

        deletarArquivoGridFs(
                equipe.getFotoFileId()
        );

        deletarArquivoGridFs(
                equipe.getBannerFileId()
        );
    }


    // =========================================================
    // BUSCAR EQUIPE E VALIDAR GERENTE
    // =========================================================

    private Equipe buscarEquipeDoGerente(
            String id,
            String gerenteId
    ) {

        Equipe equipe =
                repository.findById(id)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Equipe não encontrada"
                                        )
                        );

        if (!gerenteId.equals(
                equipe.getGerenteId()
        )) {

            throw new RuntimeException(
                    "Você não possui permissão para acessar esta equipe"
            );
        }

        return equipe;
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    public EquipeResponseDTO toResponseDTO(
            Equipe equipe
    ) {

        String fotoBase64 =
                converterArquivoParaBase64(
                        equipe.getFotoFileId()
                );

        String bannerBase64 =
                converterArquivoParaBase64(
                        equipe.getBannerFileId()
                );

        return new EquipeResponseDTO(
                equipe.getId(),
                equipe.getNome(),
                equipe.getGerenteId(),
                fotoBase64,
                bannerBase64,
                equipe.getCriadoEm(),
                equipe.getAtualizadoEm()
        );
    }


    // =========================================================
    // GRIDFS -> BASE64
    // =========================================================

    private String converterArquivoParaBase64(
            String fileId
    ) {

        if (
                fileId == null
                        || !ObjectId.isValid(fileId)
        ) {

            return null;
        }

        try {

            ObjectId objectId =
                    new ObjectId(fileId);

            com.mongodb.client.gridfs.model.GridFSFile gridFsFile =
                    gridFsTemplate.findOne(
                            new Query(
                                    Criteria.where("_id")
                                            .is(objectId)
                            )
                    );

            if (gridFsFile == null) {

                return null;
            }

            GridFsResource resource =
                    gridFsTemplate.getResource(
                            gridFsFile
                    );

            try (InputStream inputStream =
                         resource.getInputStream()) {

                byte[] bytes =
                        inputStream.readAllBytes();

                String contentType =
                        "application/octet-stream";

                if (
                        gridFsFile.getMetadata() != null
                ) {

                    if (
                            gridFsFile.getMetadata()
                                    .getString(
                                            "contentType"
                                    ) != null
                    ) {

                        contentType =
                                gridFsFile.getMetadata()
                                        .getString(
                                                "contentType"
                                        );

                    } else if (
                            gridFsFile.getMetadata()
                                    .getString(
                                            "_contentType"
                                    ) != null
                    ) {

                        contentType =
                                gridFsFile.getMetadata()
                                        .getString(
                                                "_contentType"
                                        );
                    }
                }

                String base64 =
                        Base64
                                .getEncoder()
                                .encodeToString(bytes);

                return "data:"
                        + contentType
                        + ";base64,"
                        + base64;
            }

        } catch (Exception e) {

            return null;
        }
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