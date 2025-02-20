package finpago.userservice.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReqDto {
    private String userPhone;
    private String userPassword;
}
