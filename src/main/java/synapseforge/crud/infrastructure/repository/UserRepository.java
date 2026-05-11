package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByEmailConfirmToken(String emailConfirmToken);

    Optional<User> findByEmailMudancaToken(String emailMudancaToken);

    java.util.List<User> findByNomeIgnoreCaseContaining(String nome);

    java.util.List<User> findByEmailConfirmadoFalseAndEmailConfirmTokenIsNull();

}