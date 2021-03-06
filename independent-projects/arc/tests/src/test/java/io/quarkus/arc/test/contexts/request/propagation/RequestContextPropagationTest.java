package io.quarkus.arc.test.contexts.request.propagation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ContextNotActiveException;
import org.junit.Rule;
import org.junit.Test;

public class RequestContextPropagationTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(SuperController.class, SuperButton.class);

    @Test
    public void testPropagation() {

        ArcContainer arc = Arc.container();
        ManagedContext requestContext = arc.requestContext();

        try {
            arc.instance(SuperController.class).get().getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate();
        assertFalse(SuperController.DESTROYED.get());
        SuperController controller1 = arc.instance(SuperController.class).get();
        SuperController controller2 = arc.instance(SuperController.class).get();
        String controller2Id = controller2.getId();
        assertEquals(controller1.getId(), controller2Id);
        assertNotNull(controller2.getButton());
        assertTrue(controller2.getButton() == controller1.getButton());

        // Store existing instances
        ContextState state = requestContext.getState();
        // Deactivate but don't destroy
        requestContext.deactivate();

        assertFalse(SuperController.DESTROYED.get());
        assertFalse(SuperButton.DESTROYED.get());

        try {
            // Proxy should not work
            controller1.getId();
            fail();
        } catch (ContextNotActiveException expected) {
        }

        requestContext.activate(state);
        assertEquals(arc.instance(SuperController.class).get().getId(), controller2Id);

        requestContext.terminate();
        assertTrue(SuperController.DESTROYED.get());
        assertTrue(SuperButton.DESTROYED.get());
    }

}
