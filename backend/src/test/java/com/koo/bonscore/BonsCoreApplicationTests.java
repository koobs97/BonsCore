package com.koo.bonscore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Redis, Oracle 등 실제 인프라가 필요한 통합 테스트 - 로컬 환경에서만 실행")
class BonsCoreApplicationTests {

    @Test
    void contextLoads() {
    }

}
