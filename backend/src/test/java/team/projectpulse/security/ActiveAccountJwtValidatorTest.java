package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.user.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FR-SEC-active-account: the token of a deactivated or deleted account is refused.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    ActiveAccountJwtValidator validator;

    @Test
    void testEnabledUserIsValid() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(true);

        assertThat(this.validator.validate(jwtWithUserId(4)).hasErrors()).isFalse();
    }

    @Test
    void testUserIdClaimAsLongOrStringIsRead() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(true);

        assertThat(this.validator.validate(jwtWithUserId(4L)).hasErrors()).isFalse();
        assertThat(this.validator.validate(jwtWithUserId("4")).hasErrors()).isFalse();
    }

    @Test
    void testDisabledOrMissingUserIsInvalidToken() {
        // existsByIdAndEnabledTrue is false both for a deactivated account and for one that no longer exists
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(false);

        assertInvalidToken(this.validator.validate(jwtWithUserId(4)));
    }

    @Test
    void testNoOrMalformedUserIdClaimIsInvalidTokenWithoutARead() {
        Jwt noClaim = Jwt.withTokenValue("token").header("alg", "RS256").subject("j.smith@abc.edu")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        assertInvalidToken(this.validator.validate(noClaim));
        assertInvalidToken(this.validator.validate(jwtWithUserId("not-a-number")));
        verify(this.userRepository, never()).existsByIdAndEnabledTrue(anyInt());
    }

    private static void assertInvalidToken(OAuth2TokenValidatorResult result) {
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).allSatisfy(error -> assertThat(error.getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_TOKEN));
    }

    private static Jwt jwtWithUserId(Object userId) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("j.smith@abc.edu").claim("userId", userId)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

}
