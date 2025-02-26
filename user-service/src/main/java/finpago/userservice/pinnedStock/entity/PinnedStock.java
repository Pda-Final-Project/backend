package finpago.userservice.pinnedStock.entity;

import finpago.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pinnedStockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String stockTicker;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creationTimestamp = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updateTimestamp = LocalDateTime.now();
}
