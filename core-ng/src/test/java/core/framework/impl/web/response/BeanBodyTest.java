package core.framework.impl.web.response;

import core.framework.impl.web.bean.BeanClassNameValidator;
import core.framework.impl.web.bean.BeanMappers;
import core.framework.impl.web.bean.ResponseBeanMapper;
import core.framework.impl.web.bean.TestBean;
import core.framework.internal.validate.ValidationException;
import io.undertow.io.Sender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author neo
 */
class BeanBodyTest {
    @Test
    void send() {
        var sender = mock(Sender.class);
        var responseBeanMapper = new ResponseBeanMapper(new BeanMappers());
        responseBeanMapper.register(TestBean.class, new BeanClassNameValidator());
        var context = new ResponseHandlerContext(responseBeanMapper, null);
        var body = new BeanBody(new TestBean());
        assertThatThrownBy(() -> body.send(sender, context))
                .isInstanceOf(ValidationException.class);
    }
}
