package fr.crarax.aptreasure.tasks;

import fr.crarax.aptreasure.APTreasure;
import fr.crarax.aptreasure.managers.TreasureManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Repeating task that handles treasure item animation: random movement,
 * particle effects, sound effects, and flee logic.
 * Runs on the main thread every N ticks (configurable).
 */
public final class TreasureAnimationTask extends BukkitRunnable {

    private static final Sound[] AMBIENT_SOUNDS = {
            Sound.ENTITY_VILLAGER_AMBIENT,
            Sound.ENTITY_CHICKEN_AMBIENT,
            Sound.ENTITY_ITEM_PICKUP,
            Sound.ENTITY_ENDERMAN_TELEPORT
    };

    private final APTreasure plugin;
    private final TreasureManager manager;

    /** Counter to throttle sound playback (~every 2 seconds). */
    private int tickCounter;

    /**
     * Creates a new TreasureAnimationTask.
     *
     * @param plugin the owning plugin
     */
    public TreasureAnimationTask(APTreasure plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTreasureManager();
        this.tickCounter = 0;
    }

    @Override
    public void run() {
        tickCounter++;

        int tickInterval = plugin.getConfig().getInt("tick-interval", 5);
        int durationTicks = plugin.getConfig().getInt("duration", 15) * 20;
        double fleeRadius = plugin.getConfig().getDouble("flee-radius", 5.0);
        boolean soundsEnabled = plugin.getConfig().getBoolean("sounds.enabled", true);
        float soundVolume = (float) plugin.getConfig().getDouble("sounds.volume", 0.8);
        float soundPitch = (float) plugin.getConfig().getDouble("sounds.pitch", 1.0);
        boolean particlesEnabled = plugin.getConfig().getBoolean("particles.enabled", true);
        int particleCount = plugin.getConfig().getInt("particles.count", 5);

        long currentTick = plugin.getServer().getCurrentTick();
        // Sound every ~2 seconds: 2s / (tickInterval * 50ms) = 40 / tickInterval ticks
        int soundInterval = Math.max(1, 40 / tickInterval);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Collect expired/invalid entries to remove after iteration
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Item> entry : manager.getActiveTreasures().entrySet()) {
            UUID entityId = entry.getKey();
            Item item = entry.getValue();

            // Remove dead or invalid items
            if (item == null || item.isDead() || !item.isValid()) {
                toRemove.add(entityId);
                continue;
            }

            // Check lifetime expiration
            long spawnTick = manager.getSpawnTick(entityId);
            if (spawnTick >= 0 && (currentTick - spawnTick) > durationTicks) {
                toRemove.add(entityId);
                continue;
            }

            Location itemLoc = item.getLocation();

            // 1. Random movement
            double vx = random.nextDouble(-0.15, 0.15);
            double vy = random.nextDouble(0.1, 0.25);
            double vz = random.nextDouble(-0.15, 0.15);
            Vector velocity = new Vector(vx, vy, vz);

            // 2. Flee logic: check for nearby players
            double fleeRadiusSq = fleeRadius * fleeRadius;
            Player nearest = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (Entity entity : item.getNearbyEntities(fleeRadius, fleeRadius, fleeRadius)) {
                if (entity instanceof Player player) {
                    double distSq = player.getLocation().distanceSquared(itemLoc);
                    if (distSq < fleeRadiusSq && distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = player;
                    }
                }
            }

            if (nearest != null) {
                // Calculate direction away from nearest player and boost
                Vector fleeDirection = itemLoc.toVector()
                        .subtract(nearest.getLocation().toVector())
                        .normalize()
                        .multiply(0.3);
                velocity.add(fleeDirection);
                velocity.multiply(1.5);
            }

            item.setVelocity(velocity);

            // 3. Sound effects (throttled)
            if (soundsEnabled && tickCounter % soundInterval == 0) {
                Sound sound = AMBIENT_SOUNDS[random.nextInt(AMBIENT_SOUNDS.length)];
                float pitch = (sound == Sound.ENTITY_ITEM_PICKUP) ? 0.5f : soundPitch;
                item.getWorld().playSound(itemLoc, sound, soundVolume, pitch);
            }

            // 4. Particles
            if (particlesEnabled) {
                Particle particle = random.nextBoolean() ? Particle.ENCHANT : Particle.SMOKE;
                item.getWorld().spawnParticle(particle, itemLoc.add(0, 0.3, 0),
                        particleCount, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // Clean up expired/invalid treasures
        for (UUID id : toRemove) {
            manager.unregisterTreasure(id);
        }
    }
}
