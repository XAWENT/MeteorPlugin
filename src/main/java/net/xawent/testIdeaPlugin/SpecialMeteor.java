package net.xawent.testIdeaPlugin;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

public class SpecialMeteor {
    private static class MeteorCore {
        private BlockDisplay coreBlock;
        private final Location startLocation;
        private Location currentLocation;
        private int ticks = 0;
        private final Random random = new Random();
        private final List<RiftBranch> riftBranches = new ArrayList<>();
        private final Set<Entity> teleportingEntities = new HashSet<>();
        private boolean reachedTop = false;
        private final World world;

        public MeteorCore(Location loc) {
            this.startLocation = loc.clone();
            this.currentLocation = loc.clone();
            this.world = loc.getWorld();
            if (world == null) return;
            createPhysicalCore();
            createCommandBlock();
            world.playSound(startLocation, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.8f);

            startAnimation();
        }

        private void createCommandBlock() {
            coreBlock = world.spawn(startLocation.clone().add(0, 0.5, 0), BlockDisplay.class);
            coreBlock.setBlock(Bukkit.createBlockData(Material.COMMAND_BLOCK));
            coreBlock.setViewRange(200f);
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new Quaternionf()
            );
            coreBlock.setTransformation(transformation);
        }

        private void createPhysicalCore() {
            startLocation.getBlock().setType(Material.OBSIDIAN);
            int obsidianCount = 1 + random.nextInt(2);
            for (int i = 0; i < obsidianCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 0.5 + random.nextDouble() * 1.5;
                Location obsidianLoc = startLocation.clone().add(
                        Math.cos(angle) * dist,
                        0,
                        Math.sin(angle) * dist
                );
                obsidianLoc.getBlock().setType(Material.OBSIDIAN);
            }
            int magmaCount = 3 + random.nextInt(3);
            for (int i = 0; i < magmaCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 1.5 + random.nextDouble() * 2.5;
                Location magmaLoc = startLocation.clone().add(
                        Math.cos(angle) * dist,
                        random.nextDouble() - 0.5,
                        Math.sin(angle) * dist
                );
                magmaLoc.getBlock().setType(Material.MAGMA_BLOCK);
            }
        }

        private void startAnimation() {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ticks++ > 600) {
                        createFinalExplosion();
                        cancel();
                        return;
                    }


                    if (!reachedTop) {
                        double targetY = startLocation.getY() + 7;
                        if (currentLocation.getY() < targetY) {
                            currentLocation.add(0, 0.15, 0);
                            if (coreBlock != null) {
                                coreBlock.teleport(currentLocation.clone().add(0, 0.5, 0));
                            }
                        } else {
                            reachedTop = true;
                        }
                    }


                    if (coreBlock != null) {
                        coreBlock.setRotation(ticks * 6f, 0);
                    }
                    if (ticks % 20 == 0 && reachedTop) {
                        createRiftBranch();
                    }

                    if (reachedTop && ticks % 5 == 0) {
                        createPulse();
                    }
                    updateRifts();
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
        }

        private void createPulse() {
            world.playSound(currentLocation, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.7f);

            int pulseRadius = 1 + (ticks / 5) % 15;
            double density = 0.7;
            int points = (int) (4 * Math.PI * pulseRadius * pulseRadius * density);
            for (int i = 0; i < points; i++) {
                double theta = Math.acos(2 * random.nextDouble() - 1);
                double phi = random.nextDouble() * Math.PI * 2;

                double x = pulseRadius * Math.sin(theta) * Math.cos(phi);
                double y = pulseRadius * Math.cos(theta);
                double z = pulseRadius * Math.sin(theta) * Math.sin(phi);

                Location pulseLoc = currentLocation.clone().add(x, y + 0.5, z);
                attractToPulse(pulseLoc, pulseRadius);
                world.spawnParticle(Particle.DUST, pulseLoc, 1,
                        new Particle.DustOptions(
                                Color.fromRGB(180, 70, 220),
                                0.8f
                        )
                );
                world.spawnParticle(Particle.DUST, pulseLoc, 1,
                        new Particle.DustOptions(
                                Color.fromRGB(100, 200, 255),
                                0.5f
                        )
                );
            }
        }
        private void attractToPulse(Location pulseLoc, int radius) {
            for (Entity entity : world.getNearbyEntities(pulseLoc, radius, radius, radius)) {
                if (entity instanceof Player) {
                    Vector direction = currentLocation.toVector().subtract(entity.getLocation().toVector());
                    if (direction.lengthSquared() > 0) {
                        Vector velocity = direction.normalize().multiply(0.000005);
                        entity.setVelocity(entity.getVelocity().add(velocity));
                    }
                }
            }
        }

        private void createRiftBranch() {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 9 + random.nextDouble() * 16;
            double x = currentLocation.getX() + Math.cos(angle) * distance;
            double z = currentLocation.getZ() + Math.sin(angle) * distance;
            double y = currentLocation.getY() + (1 + random.nextDouble() * 2);
            double groundY = world.getHighestBlockYAt((int) x, (int) z) + 1;
            if (y < groundY) {
                y = groundY + random.nextDouble() * 2;
            }

            Location riftStart = new Location(world, x, y, z);
            RiftBranch branch = new RiftBranch(riftStart, random, world, 2);
            riftBranches.add(branch);
            world.playSound(riftStart, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.7f);
        }

        private void updateRifts() {
            Iterator<RiftBranch> iterator = riftBranches.iterator();
            while (iterator.hasNext()) {
                RiftBranch branch = iterator.next();
                if (!branch.update()) {
                    iterator.remove();
                } else {
                    branch.render(world);
                    processBranchAttraction(branch);
                }
            }
        }

        private void processBranchAttraction(RiftBranch branch) {
            for (Location point : branch.getPoints()) {
                for (Entity entity : world.getNearbyEntities(point, 7, 7, 7)) {
                    if (entity instanceof LivingEntity && !teleportingEntities.contains(entity)) {
                        Vector direction = point.toVector().subtract(entity.getLocation().toVector());
                        if (direction.lengthSquared() > 0) {
                            Vector velocity = direction.normalize().multiply(0.15);
                            entity.setVelocity(velocity);
                        }
                        if (entity.getLocation().distanceSquared(point) < 2.45) {
                            teleportEntity(entity, point);
                            teleportingEntities.add(entity);
                        }
                    }
                }
            }
        }

        private void teleportEntity(Entity entity, Location riftPoint) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 10 + random.nextDouble() * 40;
            double x = currentLocation.getX() + Math.cos(angle) * radius;
            double z = currentLocation.getZ() + Math.sin(angle) * radius;
            double groundY = world.getHighestBlockYAt((int) x, (int) z);
            double y = groundY + 40 + random.nextInt(20);

            Location destination = new Location(world, x, y, z);
            new RiftNetwork(destination, world, 4);
            world.playSound(riftPoint, Sound.ENTITY_SHULKER_TELEPORT, 1.8f, 0.9f);
            new BukkitRunnable() {
                @Override
                public void run() {
                    entity.teleport(destination);
                    entity.setVelocity(new Vector(0, -0.5, 0));
                    world.playSound(destination, Sound.ENTITY_SHULKER_TELEPORT, 1.5f, 0.8f);
                    teleportingEntities.remove(entity);
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 10);
        }

        private void createFinalExplosion() {
            world.spawnParticle(Particle.FLASH, currentLocation, 60, 2, 2, 2, 0.2);
            world.playSound(currentLocation, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.7f);
            int radius = 15;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(currentLocation) < radius * radius) {
                    applyShake(player, 20, 0.4f); // Длительная тряска
                }
            }
            if (coreBlock != null) {
                coreBlock.remove();
            }
        }

    }
    private static class RiftNetwork {
        private final Location location;
        private final World world;
        private final Random random = new Random();
        private int ticks = 0;
        private final int duration;

        public RiftNetwork(Location loc, World world, int seconds) {
            this.location = loc;
            this.world = world;
            this.duration = seconds * 40;
            startAnimation();
        }

        private void startAnimation() {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ticks++ > duration) {
                        cancel();
                        return;
                    }
                    createNetwork();
                    if (ticks % 20 == 0) {
                        world.playSound(location, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.2f);
                    }
                }
            }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
        }

        private void createNetwork() {
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 6 + random.nextDouble() * 8;
                Location end = location.clone().add(
                        Math.cos(angle) * dist,
                        random.nextDouble() * 3,
                        Math.sin(angle) * dist
                );
                drawLine(location, end, 0.5);
            }
            for (int i = 0; i < 5; i++) {
                Location start = location.clone().add(
                        random.nextGaussian() * 3,
                        random.nextGaussian() * 2,
                        random.nextGaussian() * 3
                );

                Location end = location.clone().add(
                        random.nextGaussian() * 12,
                        random.nextGaussian() * 6,
                        random.nextGaussian() * 12
                );

                drawLine(start, end, 0.6);
            }
        }
        private void drawLine(Location start, Location end, double step) {
            Vector direction = end.toVector().subtract(start.toVector());
            double length = direction.length();
            direction.normalize();

            for (double d = 0; d < length; d += step) {
                Location point = start.clone().add(direction.clone().multiply(d));
                spawnRiftParticle(point);
            }
        }

        private void spawnRiftParticle(Location location) {
            world.spawnParticle(Particle.DUST, location, 1,
                    new Particle.DustOptions(
                            Color.fromRGB(
                                    100 + random.nextInt(155),
                                    50 + random.nextInt(200),
                                    150 + random.nextInt(105)
                            ),
                            1.5f
                    )
            );
        }
    }
    private static class RiftBranch {
        private final List<Location> points = new ArrayList<>();
        private final Random random;
        private int age = 0;
        private final int maxAge = 180;
        private final World world;
        private final int maxLevel;

        public RiftBranch(Location start, Random random, World world, int maxLevel) {
            this.random = random;
            this.world = world;
            this.maxLevel = maxLevel;
            generateBranch(start, 0);
        }

        private void generateBranch(Location start, int level) {
            points.add(start.clone());
            int segments = 5 + random.nextInt(3);
            Location current = start.clone();
            Vector direction = new Vector(0, -0.3, 0);

            for (int i = 0; i < segments; i++) {
                Vector deviation = new Vector(
                        (random.nextDouble() - 0.5) * 0.3,
                        (random.nextDouble() - 0.5) * 0.1,
                        (random.nextDouble() - 0.5) * 0.3
                );

                direction.add(deviation).normalize().multiply(1.0 + random.nextDouble() * 1.2);

                Location newPoint = current.clone().add(direction);
                double groundY = world.getHighestBlockYAt(newPoint) + 1;
                if (newPoint.getY() < groundY) {
                    newPoint.setY(groundY + random.nextDouble());
                }

                points.add(newPoint.clone());
                current = newPoint;

                // Меньше ветвлений
                if (level < maxLevel && random.nextDouble() < 0.4) {
                    createSubBranch(current.clone(), direction, level + 1);
                }
            }
        }

        private void createSubBranch(Location start, Vector mainDirection, int level) {
            int subSegments = 3 + random.nextInt(2);
            Location current = start.clone();

            Vector direction = mainDirection.clone().rotateAroundY(
                    (random.nextDouble() - 0.5) * Math.PI * 0.5
            ).normalize();
            direction.multiply(0.5 + random.nextDouble() * 0.7);

            for (int i = 0; i < subSegments; i++) {
                Location newPoint = current.clone().add(direction);

                double groundY = world.getHighestBlockYAt(newPoint) + 1;
                if (newPoint.getY() < groundY) {
                    newPoint.setY(groundY + random.nextDouble());
                }

                points.add(newPoint.clone());
                current = newPoint;
                direction.rotateAroundY(random.nextDouble() * 0.3 - 0.15);
            }
        }

        public void render(World world) {
            for (int i = 1; i < points.size(); i++) {
                drawLine(points.get(i-1), points.get(i), world);
            }
        }

        private void drawLine(Location start, Location end, World world) {
            Vector direction = end.toVector().subtract(start.toVector());
            double length = direction.length();
            direction.normalize();

            for (double d = 0; d < length; d += 0.4) {
                Location point = start.clone().add(direction.clone().multiply(d));
                spawnRiftParticle(point, world);
            }
        }

        private void spawnRiftParticle(Location location, World world) {
            world.spawnParticle(Particle.DUST, location, 1,
                    new Particle.DustOptions(
                            Color.fromRGB(
                                    120 + random.nextInt(135),
                                    60 + random.nextInt(90),
                                    160 + random.nextInt(95)
                            ),
                            1.0f
                    )
            );
        }

        public boolean update() {
            return age++ < maxAge;
        }

        public List<Location> getPoints() {
            return points;
        }
    }
    private final Location target;
    private final World world;
    protected BlockDisplay meteorEntity;
    private Vector direction;
    private final Random random = new Random();

    public SpecialMeteor(Location target) {
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

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(3.5f, 3.5f, 3.5f),
                new Quaternionf()
        );
        meteorEntity.setTransformation(transformation);
    }

    private void animate() {
        new BukkitRunnable() {
            final double speed = 2.0;
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

                
                updateRotation();

                world.spawnParticle(
                        Particle.LAVA,
                        newLoc,
                        20,
                        2.0, 2.0, 2.0,
                        0.12,
                        null,
                        true
                );

                // Густой дым
                world.spawnParticle(
                        Particle.CAMPFIRE_COSY_SMOKE,
                        newLoc,
                        25,
                        2.2, 2.2, 2.2,
                        0.12,
                        null,
                        true
                );

                if (ticks % 5 == 0) {
                    world.playSound(newLoc, Sound.ENTITY_GHAST_SHOOT, 2.0f, 0.4f);
                }

                if (ticks % 15 == 0) {
                    world.playSound(newLoc, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.3f);
                }

                if (isOnGround(newLoc)) {
                    explode();
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("TestIdeaPlugin"), 0, 1);
    }

    private void updateRotation() {
        if (direction == null) return;

        Vector dir = direction.clone();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(yaw));

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                new Vector3f(3.5f, 3.5f, 3.5f),
                new Quaternionf()
        );
        meteorEntity.setTransformation(transformation);
    }

    private boolean isOnGround(Location location) {
        return location.clone().subtract(0, 0.5, 0).getBlock().getType().isSolid();
    }

    private void explode() {
        if (meteorEntity == null || world == null) return;

        Location loc = meteorEntity.getLocation();


        world.createExplosion(loc, 10.0f, true, true);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.5f);


        world.spawnParticle(Particle.EXPLOSION, loc, 5, 2.0, 2.0, 2.0, 0.5);
        world.spawnParticle(Particle.FLASH, loc, 30, 0, 0, 0, 0.5);

        meteorEntity.remove();

        int radius = 20;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < radius * radius) {
                applyShake(player, 15, 0.5f);
            }
        }


        new MeteorCore(loc.clone());
    }

    private static void applyShake(Player player, int durationTicks, float intensity) {
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
}