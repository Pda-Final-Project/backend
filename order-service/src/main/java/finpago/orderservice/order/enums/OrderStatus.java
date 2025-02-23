package finpago.orderservice.order.enums;

public enum OrderStatus {
    SENT,
    PENDING,    // 미체결
    FINISHED,   // 체결 완료
    FAILED    // 취소됨
}
