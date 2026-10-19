package io.github.mdw22.paranormalcraft.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mdw22.paranormalcraft.Balance;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The Tether Wand's heat and wear, as plain logic so it can be unit tested. Firing adds 1 heat a tick; 8 s of
 * continuous fire ({@link #MAX_HEAT}) overheats it: a 4 s lockout while it cools. Cooling takes 2 heat a tick, so an
 * overheated wand is cold again just as the lockout ends. Every {@link #TICKS_PER_DURABILITY} ticks of beam cost one
 * point of durability (300 durability = 300 s of beam).
 *
 * @param heat      0 to {@link #MAX_HEAT}
 * @param lockout   ticks left before it can fire again after overheating
 * @param beamTicks beam ticks since durability was last charged
 */
public record WandHeat(int heat, int lockout, int beamTicks) {

    public static final Codec<WandHeat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("heat").forGetter(WandHeat::heat),
            Codec.INT.fieldOf("lockout").forGetter(WandHeat::lockout),
            Codec.INT.fieldOf("beam_ticks").forGetter(WandHeat::beamTicks)
    ).apply(instance, WandHeat::new));
    public static final StreamCodec<ByteBuf, WandHeat> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WandHeat::heat,
            ByteBufCodecs.VAR_INT, WandHeat::lockout,
            ByteBufCodecs.VAR_INT, WandHeat::beamTicks,
            WandHeat::new);

    public static final int MAX_HEAT = Balance.Tether.MAX_HEAT;
    public static final int COOL_PER_TICK = Balance.Tether.COOL_PER_TICK;
    public static final int LOCKOUT_TICKS = Balance.Tether.LOCKOUT_TICKS;
    public static final int TICKS_PER_DURABILITY = Balance.Tether.TICKS_PER_DURABILITY;
    public static final WandHeat COLD = new WandHeat(0, 0, 0);

    public boolean canFire() {

        return this.lockout == 0;
    }

    public boolean overheated() {

        return this.lockout > 0;
    }

    /**
     * One tick of firing. If this tick reaches {@link #MAX_HEAT}, the wand overheats.
     */
    public WandHeat fire() {

        if (!canFire()) {
            return this;
        }
        int newHeat = Math.min(MAX_HEAT, this.heat + 1);
        int newLockout = newHeat >= MAX_HEAT ? LOCKOUT_TICKS : 0;
        return new WandHeat(newHeat, newLockout, this.beamTicks + 1);
    }

    /**
     * One tick of not firing.
     */
    public WandHeat cool() {

        return new WandHeat(Math.max(0, this.heat - COOL_PER_TICK), Math.max(0, this.lockout - 1), this.beamTicks);
    }

    /**
     * Forces an overheat (crossed beams).
     */
    public WandHeat overheat() {

        return new WandHeat(MAX_HEAT, LOCKOUT_TICKS, this.beamTicks);
    }

    /**
     * Whether a point of durability is due; {@link #charged()} resets the count.
     */
    public boolean durabilityDue() {

        return this.beamTicks >= TICKS_PER_DURABILITY;
    }

    public WandHeat charged() {

        return new WandHeat(this.heat, this.lockout, this.beamTicks - TICKS_PER_DURABILITY);
    }

    public float fraction() {

        return (float) this.heat / MAX_HEAT;
    }
}
