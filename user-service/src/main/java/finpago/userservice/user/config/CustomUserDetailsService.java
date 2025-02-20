
package finpago.userservice.user.config;

import finpago.userservice.user.entity.User;
import finpago.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userPhone) throws UsernameNotFoundException {

        User user = userRepository.findByUserPhone(userPhone)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다. 전화번호 = " + userPhone));

        return new CustomUserDetails(user);
    }
}
