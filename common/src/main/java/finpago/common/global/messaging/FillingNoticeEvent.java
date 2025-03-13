package finpago.common.global.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FillingNoticeEvent {
    private String ticker;
    private String filling_type;
}


