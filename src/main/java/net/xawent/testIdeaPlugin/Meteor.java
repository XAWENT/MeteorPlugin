package net.xawent.testIdeaPlugin;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

public class Meteor {
    private final Location target;
    private final World world;
    protected BlockDisplay meteorEntity;
    private Vector direction;
    private final Random random = new Random();

    public Meteor(Location target) {
        this.target = target;
        this.world = target.getWorld();
        spawn();
        animate();
    }

    private void spawn() {
        if (world == null) return;

        double distance = 100 + random.nextInt(30);
        double angle = random.nextDouble() * Math.PI * 2;
        double startX = target.getX() + Math.cos(angle) * distance;
        double startZ = target.getZ() + Math.sin(angle) * distance;
        double startY = world.getMaxHeight() + 50;
        Location startLoc = new Location(world, startX, startY, startZ);
        direction = target.toVector().subtract(startLoc.toVector()).normalize();
        meteorEntity = world.spawn(startLoc, BlockDisplay.class);
        meteorEntity.setBlock(Bukkit.createBlockData(Material.MAGMA_BLOCK));
        meteorEntity.setViewRange(120f);
        updateTransformation();
    }

    private void updateTransformation() {
        if (direction == null) return;

        Vector dir = direction.clone();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(yaw));

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(1.5f, 1.5f, 1.5f),
                new Quaternionf()
        );
        meteorEntity.setTransformation(transformation);
    }

    private void animate() {
        new BukkitRunnable() {
            final double speed = 1.8;
            int ticks = 0;

            @Override
            public void run() {
                if (meteorEntity == null || meteorEntity.isDead() || world == null) {
                    cancel();
                    return;
                }
                Location currentLoc = meteorEntity.getLocation();
                Location newLoc = currentLoc.add(direction.clone().multiply(speed));
                meteorEntity.teleport(newLoc);
                updateTransformation();
                world.spawnParticle(
                        Particle.LAVA,
                        newLoc,
                        10,
                        0.8, 0.8, 0.8,
                        0.05,
                        null,
                        true
                );
                world.spawnParticle(
                        Particle.CAMPFIRE_COSY_SMOKE,
                        newLoc,
                        15,
                        1.0, 1.0, 1.0,
                        0.04,
                        null,
                        true
                );
                if (ticks % 15 == 0) {
                    world.playSound(newLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.2f);
                    int flyRadius = 30;
                    for (Player player : world.getPlayers()) {
                        if (player.getLocation().distanceSquared(newLoc) < flyRadius * flyRadius) {
                            applyShake(player, 5, 0.1f);
                        }
                    }
                }
                if (isOnGround(newLoc)) {
                    explode();
                    createCore(newLoc);
                    createAftermathSmoke(newLoc);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
    }

    private boolean isOnGround(Location location) {
        return location.clone().subtract(0, 0.2, 0).getBlock().getType().isSolid();
    }

    protected void explode() {
        if (meteorEntity == null || world == null) return;

        Location loc = meteorEntity.getLocation();
        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0, 0, 0, 0.5, null, true);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.6f);
        world.createExplosion(loc, 8.0f, true, true);
        meteorEntity.remove();
        int explosionRadius = 25;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < explosionRadius * explosionRadius) {
                applyShake(player, 20, 0.5f);
            }
        }
    }

    private void createCore(Location center) {
        Location ground = center.clone();
        while (ground.getY() > world.getMinHeight() && ground.getBlock().getType().isAir()) {
            ground.add(0, -1, 0);
        }
        ground.add(0, 1, 0);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location blockLoc = ground.clone().add(x, y, z);
                    if (x == 0 && y == 1 && z == 0) {
                        blockLoc.getBlock().setType(Material.OBSIDIAN);
                    } else {
                        blockLoc.getBlock().setType(Material.MAGMA_BLOCK);
                    }
                }
            }
        }
        int coreRadius = 15;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(ground) < coreRadius * coreRadius) {
                applyShake(player, 15, 0.3f);
            }
        }
    }

    private void applyShake(Player player, int durationTicks, float intensity) {
        if (!player.isOnline()) return;
        Location originalLoc = player.getLocation();
        new BukkitRunnable() {
            int ticksPassed = 0;
            public void run() {
                if (ticksPassed++ >= durationTicks || !player.isOnline()) {
                    player.teleport(originalLoc);
                    cancel();
                    return;
                }


                double dx = (Math.random() - 0.5) * intensity;
                double dy = (Math.random() - 0.5) * intensity;
                double dz = (Math.random() - 0.5) * intensity;

                Location newLoc = originalLoc.clone().add(dx, dy, dz);
                player.teleport(newLoc);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
    }

    private void createAftermathSmoke(Location center) {
        new BukkitRunnable() {
            int duration = 180;

            @Override
            public void run() {
                if (duration <= 0 || world == null) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 20; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 10;
                    double offsetY = random.nextDouble() * 6;
                    double offsetZ = (random.nextDouble() - 0.5) * 10;

                    Location smokeLoc = center.clone().add(offsetX, offsetY, offsetZ);
                    world.spawnParticle(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            smokeLoc,
                            3,
                            0.4, 0.4, 0.4,
                            0.025,
                            null,
                            true
                    );

                    if (duration % 25 == 0) {
                        world.spawnParticle(
                                Particle.LAVA,
                                smokeLoc,
                                2,
                                0, 0, 0,
                                0,
                                null,
                                true
                        );
                    }
                }
                duration--;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
    }
}