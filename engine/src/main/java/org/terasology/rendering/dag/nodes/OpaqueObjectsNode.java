/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.terasology.assets.ResourceUrn;
import org.terasology.config.Config;
import org.terasology.config.RenderingDebugConfig;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.dag.AbstractNode;
import org.terasology.rendering.dag.WireframeCapable;
import org.terasology.rendering.dag.WireframeTrigger;
import org.terasology.rendering.dag.stateChanges.LookThrough;
import org.terasology.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.rendering.dag.stateChanges.SetWireframe;
import org.terasology.rendering.opengl.FBO;
import org.terasology.rendering.opengl.fbms.DisplayResolutionDependentFBOs;
import org.terasology.rendering.world.WorldRenderer;

import static org.terasology.rendering.opengl.fbms.DisplayResolutionDependentFBOs.SCENE_OPAQUE;

/**
 * This node renders the opaque (as opposed to semi-transparent)
 * objects present in the world. This node -does not- render the landscape.
 *
 * Objects to be rendered must be registered as implementing the interface RenderSystem and
 * take advantage of the RenderSystem.renderOpaque() method, which is called in process().
 */
public class OpaqueObjectsNode extends AbstractNode implements WireframeCapable {
    @In
    private ComponentSystemManager componentSystemManager;

    @In
    private Config config;

    @In
    private WorldRenderer worldRenderer;

    @In
    private DisplayResolutionDependentFBOs displayResolutionDependentFBOs;

    private Camera playerCamera;
    private SetWireframe wireframeStateChange;
    private RenderingDebugConfig renderingDebugConfig;
    private FBO sceneOpaqueFbo;

    /**
     * Initialises this node. -Must- be called once after instantiation.
     */
    @Override
    public void initialise() {
        sceneOpaqueFbo = displayResolutionDependentFBOs.get(SCENE_OPAQUE);

        playerCamera = worldRenderer.getActiveCamera();

        wireframeStateChange = new SetWireframe(true);
        renderingDebugConfig = config.getRendering().getDebug();
        new WireframeTrigger(renderingDebugConfig, this);

        addDesiredStateChange(new LookThrough(playerCamera));
        addDesiredStateChange(new SetViewportToSizeOf(SCENE_OPAQUE, displayResolutionDependentFBOs));
    }

    public void enableWireframe() {
        if (!getDesiredStateChanges().contains(wireframeStateChange)) {
            addDesiredStateChange(wireframeStateChange);
            worldRenderer.requestTaskListRefresh();
        }
    }

    public void disableWireframe() {
        if (getDesiredStateChanges().contains(wireframeStateChange)) {
            removeDesiredStateChange(wireframeStateChange);
            worldRenderer.requestTaskListRefresh();
        }
    }

    /**
     * Iterates over any registered RenderSystem instance and calls its renderOpaque() method.
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/opaqueObjects");

        sceneOpaqueFbo.bind(); // TODO: remove when we can bind this via a StateChange

        for (RenderSystem renderer : componentSystemManager.iterateRenderSubscribers()) {
            renderer.renderOpaque();
        }

        PerformanceMonitor.endActivity();
    }
}
