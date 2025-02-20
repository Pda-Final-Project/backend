package finpago.userservice;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", length = 64)
    private Long userId;

    @Column(name = "user_name", length = 20, nullable = false)
    private String userName;

    @Column(name = "user_phone", length = 20, nullable = false, unique = true)
    private String userPhone;

    @Column(name = "user_password", nullable = false)
    private String userPassword;

    @Column(name = "user_notification_switch")
    private Boolean userNotificationSwitch;
}
