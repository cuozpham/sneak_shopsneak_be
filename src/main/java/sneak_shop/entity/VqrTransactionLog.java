package sneak_shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vqr_transaction_logs")
public class VqrTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String content;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_account", length = 255)
    private String bankAccount;

    @Column(name = "transaction_date", length = 255)
    private String transactionDate;

    @Lob
    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
