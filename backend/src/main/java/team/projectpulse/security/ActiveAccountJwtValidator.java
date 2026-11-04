package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * FR-SEC-active-account: refuses the token of an account that has been deactivated, or no longer exists, on its next
 * request, rather than letting it work until it expires.
 *
 * <p>Runs in the {@code JwtDecoder} after the default signature and timestamp checks, for every bearer-authenticated
 * request and every user, student or instructor. A refusal is the standard {@code invalid_token} error, which the
 * resource server turns into an {@code InvalidBearerTokenException} and {@code ExceptionHandlerAdvice} into a
 * {@code 401}; the SPA's {@code 401} handler then logs the user out.
 *
 * <p>The account is read live rather than from a claim, so deactivation takes effect without touching
 * {@code JwtProvider}. The cost is one primary-key read per authenticated request, negligible at course scale.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "The account this token was issued to is deactivated or no longer exists.", null);

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt.getClaim("userId"));
        if (userId == null || !this.userRepository.existsByIdAndEnabledTrue(userId)) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        }
        return OAuth2TokenValidatorResult.success();
    }

    /**
     * The {@code userId} claim as {@code UserUtils.getUserId} reads it: an Integer, a Long, or a numeric String. Anything
     * else, or nothing, is {@code null}, which the caller treats as a refusal.
     */
    private static Integer readUserId(Object claim) {
        if (claim instanceof Integer i) {
            return i;
        }
        if (claim instanceof Long l) {
            return l.intValue();
        }
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
