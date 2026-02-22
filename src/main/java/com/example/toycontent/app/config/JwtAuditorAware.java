package com.example.toycontent.app.config;

import com.example.toycontent.app.auth.token.JwtParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static com.example.toycontent.app.common.constants.GlobalConstants.AUTHORIZATION_HEADER;

@Component
@RequiredArgsConstructor
public class JwtAuditorAware implements AuditorAware<Long> {

    private final JwtParser jwtParser;

    @Override
    public Optional<Long> getCurrentAuditor() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }

            HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
            String token = request.getHeader(AUTHORIZATION_HEADER);

            if (token == null || !token.startsWith("Bearer ")) {
                return Optional.empty();
            }

            token = token.substring(7);
            return Optional.of(jwtParser.getUserId(token));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
