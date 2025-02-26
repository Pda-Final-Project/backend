
package finpago.userservice.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinReqDto {
    private String userName;
    private String userPhone;
    private String userPassword;
}
