package com.lorenzosanz.uploadservice;
import com.lorenzosanz.uploadservice.support.PostgresTestContainerConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class UploadServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
