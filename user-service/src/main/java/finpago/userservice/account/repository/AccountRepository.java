package finpago.userservice.account.repository;

import finpago.userservice.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Account findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.accountWithholding = :newBalance WHERE a.userId = :userId")
    void updateAccountWithholding(Long userId, Long newBalance);
}
