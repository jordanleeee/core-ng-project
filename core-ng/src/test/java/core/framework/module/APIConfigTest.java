package core.framework.module;

import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.impl.module.ModuleContext;
import core.framework.impl.reflect.Classes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class APIConfigTest {
    private APIConfig config;
    private ModuleContext context;

    @BeforeAll
    void createAPIConfig() {
        config = new APIConfig();
        context = new ModuleContext();
        config.initialize(context, null);
    }

    @Test
    void service() {
        config.service(TestWebService.class, new TestWebServiceImpl());

        assertThat(config.serviceInterfaces).containsEntry(Classes.className(TestWebService.class), TestWebService.class);
    }

    @Test
    void client() {
        config.httpClient().timeout(Duration.ofSeconds(5));
        config.client(TestWebService.class, "http://localhost");

        TestWebService client = context.beanFactory.bean(TestWebService.class, null);
        assertThat(client).isNotNull();
    }

    public interface TestWebService {
        @PUT
        @Path("/test/:id")
        void put(@PathParam("id") Integer id);
    }

    public static class TestWebServiceImpl implements TestWebService {
        @Override
        public void put(Integer id) {
        }
    }
}
