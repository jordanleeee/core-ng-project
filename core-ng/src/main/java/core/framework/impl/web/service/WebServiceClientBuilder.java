package core.framework.impl.web.service;

import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.impl.asm.CodeBuilder;
import core.framework.impl.asm.DynamicInstanceBuilder;
import core.framework.impl.reflect.Methods;
import core.framework.impl.reflect.Params;
import core.framework.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import static core.framework.impl.asm.Literal.type;
import static core.framework.impl.asm.Literal.variable;

/**
 * @author neo
 */
public class WebServiceClientBuilder<T> {
    final DynamicInstanceBuilder<T> builder;
    private final Class<T> serviceInterface;
    private final WebServiceClient client;

    public WebServiceClientBuilder(Class<T> serviceInterface, WebServiceClient client) {
        this.serviceInterface = serviceInterface;
        this.client = client;
        builder = new DynamicInstanceBuilder<>(serviceInterface, serviceInterface.getCanonicalName() + "$Client");
    }

    public T build() {
        builder.addField("private final {} client;", type(WebServiceClient.class));
        builder.addField("private final {} logger = {}.getLogger({});", type(Logger.class), type(LoggerFactory.class), variable(WebServiceClient.class));
        builder.constructor(new Class<?>[]{WebServiceClient.class}, "this.client = $1;");

        Method[] methods = serviceInterface.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));    // to make generated code deterministic
        for (Method method : methods) {
            builder.addMethod(buildMethod(method));
        }

        return builder.build(client);
    }

    private String buildMethod(Method method) {
        CodeBuilder builder = new CodeBuilder();

        Type returnType = method.getGenericReturnType();
        Class<?> returnClass = method.getReturnType();

        Map<String, Integer> pathParamIndexes = Maps.newHashMap();
        Class<?> requestBeanClass = null;
        Integer requestBeanIndex = null;
        builder.append("public {} {}(", type(returnClass), method.getName());
        Annotation[][] annotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramClass = parameterTypes[i];
            if (i > 0) builder.append(", ");
            builder.append("{} param{}", type(paramClass), i);

            PathParam pathParam = Params.annotation(annotations[i], PathParam.class);
            if (pathParam != null) {
                pathParamIndexes.put(pathParam.value(), i);
            } else {
                requestBeanIndex = i;
                requestBeanClass = method.getParameterTypes()[i];
            }
        }
        builder.append(") {\n");

        builder.indent(1).append("logger.debug(\"call web service, method={}\");\n", Methods.path(method));

        builder.indent(1).append("java.lang.Class requestBeanClass = {};\n", requestBeanClass == null ? "null" : variable(requestBeanClass));
        builder.indent(1).append("Object requestBean = {};\n", requestBeanIndex == null ? "null" : "param" + requestBeanIndex);

        builder.indent(1).append("java.util.Map pathParams = new java.util.HashMap();\n");
        pathParamIndexes.forEach((name, index) ->
                builder.indent(1).append("pathParams.put({}, param{});\n", variable(name), index));

        String path = method.getDeclaredAnnotation(Path.class).value();
        builder.indent(1).append("String serviceURL = client.serviceURL({}, pathParams);\n", variable(path)); // to pass path as string literal, not escaping char, like \\ which is not allowed, refer to core.framework.impl.web.route.PathPatternValidator

        builder.indent(1);
        if (returnType != void.class) builder.append("return ({}) ", type(returnClass));
        builder.append("client.execute({}, serviceURL, requestBeanClass, requestBean, {});\n", variable(HTTPMethods.httpMethod(method)), variable(returnType));

        builder.append("}");
        return builder.build();
    }
}
