package finpago.userservice.account.entity;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Id
    @Column(name = "user_id", length = 64, nullable = false)
    private Long userId;

    @Column(name = "account_name", length = 20)
    private String accountName;

    @Column(name = "account_number", length = 64, unique = true)
    private String accountNumber;

    //간편비밀번호
    @Column(name = "account_password", length = 64)
    private String accountPassword;

    //예수금
    @Column(name = "account_withholding")
    private Long accountWithholding;

}
