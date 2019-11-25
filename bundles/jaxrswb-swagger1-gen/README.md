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

import de.maggu2810.osgi.jaxrswb.swagger1.gen.JaxRsWhiteboardSwaggerGenerator;

/**
 * An endpoint to generate and provide an Swagger 1 description.
 *
 * @author Markus Rathgeb
 */
@Component(service = { App.class }, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@JSONRequired
@Path("/swagger")
public class App {

    private final Logger logger = LoggerFactory.getLogger(App.class);
    private final JaxRsWhiteboardSwaggerGenerator generator;

    /**
     * Creates a new instance.
     *
     * @param generator the generator
     */
    @Activate
    public App(final @Reference JaxRsWhiteboardSwaggerGenerator generator) {
        this.generator = generator;
    }

    /**
     * Gets the current JAX-RS Whiteboard provided endpoint information by Swagger 1.
     *
     * @return an Swagger 1 description of the endpoints
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getSwagger() {
        final Map<String, Object> map;
        try {
            map = generator.toMap(generator.generate());
        } catch (final IOException ex) {
            logger.warn("Error on Swagger DTO generation.", ex);
            return Response.serverError().build();
        }
        return Response.status(Response.Status.OK).entity(map).build();

    }

}
```