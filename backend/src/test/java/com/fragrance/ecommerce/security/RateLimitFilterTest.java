package com.fragrance.ecommerce.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    @Test
    void blocksRequestsAfterConfiguredLimit() throws Exception {
        var filter = new RateLimitFilter(2, 60);
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.2");

        filter.doFilter(request, new MockHttpServletResponse(), chain);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        var blocked = new MockHttpServletResponse();
        filter.doFilter(request, blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        verify(chain, org.mockito.Mockito.times(2)).doFilter(same(request), any());
    }
}
