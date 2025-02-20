package finpago.userservice;


import finpago.common.global.exception.error.DuplicateUserPhoneException;
import finpago.userservice.dto.JoinReqDto;
import finpago.userservice.dto.LoginReqDto;
import finpago.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public void join(JoinReqDto joinReqDto) {

        if (userRepository.findByUserPhone(joinReqDto.getUserPhone()).isPresent()) {
            throw new DuplicateUserPhoneException("이미 등록된 전화번호입니다.");
        }
        String encodedPassword = passwordEncoder.encode(joinReqDto.getUserPassword());

        User user = User.builder()
                .userPhone(joinReqDto.getUserPhone())
                .userName(joinReqDto.getUserName())
                .userPassword(encodedPassword)
                .userNotificationSwitch(true)
                .build();

        userRepository.save(user);
    }

    public String login(LoginReqDto loginReqDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginReqDto.getUserPhone(), loginReqDto.getUserPassword())
        );

        User user = userRepository.findByUserPhone(loginReqDto.getUserPhone())
                .orElseThrow(() -> new UsernameNotFoundException("등록되지 않은 전화번호입니다."));

        return jwtUtil.generateToken(user.getUserPhone());
    }
}