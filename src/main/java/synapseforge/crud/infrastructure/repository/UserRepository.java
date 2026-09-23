package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {


    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByEmailConfirmToken(String emailConfirmToken);

    Optional<User> findByEmailMudancaToken(String emailMudancaToken);

    List<User> findByNomeIgnoreCaseContaining(String nome);

    List<User> findByEmailConfirmadoFalseAndEmailConfirmTokenIsNull();

    List<User> findByEquipeId(String equipeId);

    List<User> findByEquipeIdAndNomeIgnoreCaseContaining(String equipeId, String nome);

    // Recebe o padrão já ancorado e escapado (^\Q...\E$): comparação exata sem diferenciar caixa
    @Query("{ 'email': { $regex: ?0, $options: 'i' } }")
    List<User> findByEmailPadraoIgnorandoCaixa(String padraoEmail);


}
