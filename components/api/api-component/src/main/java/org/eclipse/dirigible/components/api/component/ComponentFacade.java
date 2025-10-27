package org.eclipse.dirigible.components.api.component;

import org.eclipse.dirigible.components.engine.javascript.parser.ComponentContext;
import org.eclipse.dirigible.components.engine.javascript.parser.ComponentContextRegistry;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComponentFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ComponentFacade.class.getCanonicalName());

    /** The bpm facade. */
    private static ComponentFacade INSTANCE;


    @Autowired
    private ComponentFacade() {

    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the component facade
     */
    public static ComponentFacade get() {
        return INSTANCE;
    }

    public static void injectDependencies(Value instance) {
        Value constructor = instance.getMember("constructor");
        String componentName = null;
        Value injectionsMap = null;
        String name = constructor.getMember("name")
                                 .asString();

        if (constructor.hasMember("__component_name")) {
            componentName = constructor.getMember("__component_name")
                                       .asString();
        }

        if (constructor.hasMember("__injections_map")) {
            injectionsMap = constructor.getMember("__injections_map");
        }

        if (!constructor.hasMember("__injections_map")) {
            // Fallback: retrieve from context
            String contextId = ComponentContextHolder.get();
            ComponentContext context = ComponentContextRegistry.getContext(contextId);
            if (constructor.hasMember("__component_name")) {
                String cname = constructor.getMember("__component_name")
                                          .asString();
                injectionsMap = context.getMetadata(cname);
            }
        }

        if (injectionsMap == null || injectionsMap.isNull()) {
            logger.error("Metadata is null for: {}", name);



            return;
        }

        Value entriesIterator = injectionsMap.getMember("entries")
                                             .execute();

        while (true) {
            Value next = entriesIterator.getMember("next")
                                        .execute();
            if (next.getMember("done")
                    .asBoolean()) {
                logger.error("Metadata is empty for: {}", name);



                break;
            }

            Value entry = next.getMember("value");
            String propertyKey = entry.getArrayElement(0)
                                      .asString();
            Value injectionNameValue = entry.getArrayElement(1);
            String lookupName = injectionNameValue.isNull() ? propertyKey : injectionNameValue.asString();

            String contextId = "default"; // or dynamic
            ComponentContext context = ComponentContextRegistry.getContext(contextId);
            Value dependency = context.getComponent(lookupName);

            if (dependency != null && !dependency.isNull()) {
                instance.putMember(propertyKey, dependency);
            } else {
                logger.warn("Dependency not found for property [{}] in context [{}]", lookupName, contextId);
            }
        }
    }

}
