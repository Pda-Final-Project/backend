package finpago.userservice.account.repository;

import finpago.userservice.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.accountWithholding = :newBalance WHERE a.userId = :userId")
    void updateAccountWithholding(Long userId, Long newBalance);

    @Query("SELECT a.accountWithholding FROM Account a WHERE a.userId = :userId")
    Long getBalanceByUserId(Long userId);
}
