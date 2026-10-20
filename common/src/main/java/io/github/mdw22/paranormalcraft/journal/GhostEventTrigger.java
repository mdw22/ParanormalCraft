package io.github.mdw22.paranormalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * {@code paranormalcraft:ghost_event}: one criterion trigger for every Field Journal and Paranormal Craft advancement.
 * Fired with an {@link JournalEvent} and, where it applies, the ghost's entity type id; an advancement criterion
 * matches an event and optionally a ghost type.
 */
public class GhostEventTrigger extends SimpleCriterionTrigger<GhostEventTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {

        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, JournalEvent event, Optional<String> ghostType) {

        this.trigger(player, instance -> instance.matches(event, ghostType));
    }

    /**
     * @param event     the event's serialised name
     * @param ghostType when present, only that ghost type's events count
     */
    public record TriggerInstance(Optional<ContextAwarePredicate> player, String event, Optional<String> ghostType)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event),
                Codec.STRING.optionalFieldOf("ghost_type").forGetter(TriggerInstance::ghostType)
        ).apply(instance, TriggerInstance::new));

        boolean matches(JournalEvent fired, Optional<String> firedType) {

            return this.event.equals(fired.serialName()) && (this.ghostType.isEmpty() || this.ghostType.equals(firedType));
        }

        public static Criterion<TriggerInstance> of(GhostEventTrigger trigger, JournalEvent event, Optional<String> ghostType) {

            return trigger.createCriterion(new TriggerInstance(Optional.empty(), event.serialName(), ghostType));
        }
    }
}
