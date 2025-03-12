package finpago.userservice.pinnedStock.entity;

import finpago.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pinned_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pinnedStockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String stockTicker;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationTimestamp = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updateTimestamp = LocalDateTime.now();
}
