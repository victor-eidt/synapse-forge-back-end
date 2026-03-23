package com.synapseforge.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private String id;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
}
