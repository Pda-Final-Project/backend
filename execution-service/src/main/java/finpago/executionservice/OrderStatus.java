package finpago.executionservice;

public enum OrderStatus {
    CREATED,    //생성됨
    PENDING,    // 미체결
    FINISHED,   // 체결 완료
    FAILED    // 취소됨
}
