# JAX-RS Whiteboard OpenAPI Generator

## Examples

### JAX-RS Whiteboard resource

```java
import java.io.IOException;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JSONRequired;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.maggu2810.jaxrswb.oapi.gen.JaxRsWhiteboardOpenAPIGenerator;
import io.swagger.v3.oas.annotations.Hidden;

/**
 * An endpoint to generate and provide an OpenAPI description.
 *
 * @author Markus Rathgeb
 */
@Component(service = { App.class }, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@JSONRequired
@Path("/oapi")
public class App {

    private final Logger logger = LoggerFactory.getLogger(App.class);
    private final JaxRsWhiteboardOpenAPIGenerator generator;

    /**
     * Creates a new instance.
     *
     * @param generator the generator
     */
    @Activate
    public App(final @Reference JaxRsWhiteboardOpenAPIGenerator generator) {
        this.generator = generator;
    }

    /**
     * Gets the current JAX-RS Whiteboard provided endpoint information by OpenAPI.
     *
     * @return an OpenAPI description of the endpoints
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Hidden
    public Response getOpenApi() {
        final Map<String, Object> map;
        try {
            map = generator.generateMap();
        } catch (final IOException ex) {
            logger.warn("Error on OpenAPI DTO generation.", ex);
            return Response.serverError().build();
        }
        return Response.status(Response.Status.OK).entity(map).build();
    }

}
```