package io.github.mdw22.paranormalcraft.client.tether;

import io.github.mdw22.paranormalcraft.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * The Tether Wand's hum, looping at the firing player for as long as their beam is active.
 */
public class TetherHumSound extends AbstractTickableSoundInstance {

    private final Player owner;

    public TetherHumSound(Player owner) {

        super(ModSounds.TETHER_HUM.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.owner = owner;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6F;
        this.x = owner.getX();
        this.y = owner.getEyeY();
        this.z = owner.getZ();
    }

    @Override
    public void tick() {

        if (this.owner.isRemoved()) {
            stop();
            return;
        }
        this.x = this.owner.getX();
        this.y = this.owner.getEyeY();
        this.z = this.owner.getZ();
    }

    public void finish() {

        stop();
    }
}
