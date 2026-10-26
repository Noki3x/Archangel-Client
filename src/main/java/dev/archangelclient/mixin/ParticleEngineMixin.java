package dev.archangelclient.mixin;

import dev.archangelclient.ArchangelClient;
import dev.archangelclient.performance.FpsBoostModule;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FPS Boost's particle limit: new particles are dropped once the cap is reached. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow @Final private Map<ParticleRenderType, ParticleGroup<?>> particles;
    @Shadow @Final private Queue<Particle> particlesToAdd;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void archangel$particleCap(Particle particle, CallbackInfo ci) {
        int cap = ArchangelClient.modules().get(FpsBoostModule.class).particleCap();
        if (cap < 0) return;
        int alive = particlesToAdd.size();
        for (ParticleGroup<?> group : particles.values()) alive += group.size();
        if (alive >= cap) ci.cancel();
    }
}
