
package finpago.userservice.user.repository;

import finpago.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserPhone(String userPhone);

    Optional<User> findByUserId(Long userId);
}
