package com.kraken.api.query.graphicsobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import net.runelite.api.GraphicsObject;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A fluent query over the graphics objects currently playing in the scene.
 *
 * <p>Finished effects are excluded at the source, so everything matched is live. Usage:</p>
 *
 * <pre>{@code
 * ctx.graphicsObjects().withId(AOE_TELEGRAPH).at(myTile).isPresent();  // standing in a telegraph?
 * ctx.graphicsObjects().withId(AOE_TELEGRAPH).within(2).list();
 * }</pre>
 */
public class GraphicsObjectQuery extends AbstractSpatialQuery<GraphicsObjectEntity, GraphicsObjectQuery, GraphicsObject> {

    public GraphicsObjectQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<GraphicsObjectEntity>> source() {
        return () -> StreamSupport.stream(ctx.getClient().getGraphicsObjects().spliterator(), false)
                .filter(Objects::nonNull)
                .filter(g -> !g.finished())
                .map(g -> new GraphicsObjectEntity(ctx, g));
    }
}
