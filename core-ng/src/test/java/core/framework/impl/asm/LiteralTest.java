package core.framework.impl.asm;

import core.framework.http.HTTPMethod;
import core.framework.util.Types;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author neo
 */
public class LiteralTest {
    @Test
    public void enumVariable() {
        assertEquals("core.framework.http.HTTPMethod.POST", Literal.variable(HTTPMethod.POST));
    }

    @Test
    public void typeVariable() {
        assertEquals("java.lang.String.class", Literal.variable(String.class));

        assertEquals("core.framework.util.Types.list(java.lang.String.class)", Literal.variable(Types.list(String.class)));

        assertEquals("core.framework.util.Types.optional(java.lang.String.class)", Literal.variable(Types.optional(String.class)));
    }
}
