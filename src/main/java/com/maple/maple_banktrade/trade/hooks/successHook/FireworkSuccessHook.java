package com.maple.maple_banktrade.trade.hooks.successHook;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class FireworkSuccessHook extends MachineTradeHooks.SuccessHook {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FireworkSuccessHook");
        t.setDaemon(true);
        return t;
    });

    @Persisted
    private int intensity;

    public FireworkSuccessHook(int intensity) {
        this.intensity = Math.clamp(intensity, 1, 3);
    }

    // ==============================================
    // 颜色工具：高饱和彩色为主，带完整 Alpha 通道
    // ==============================================
    private static int randomBrightColor() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int r = rand.nextInt(180, 256);
        int g = rand.nextInt(180, 256);
        int b = rand.nextInt(180, 256);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int randomHueColor(float hueMin, float hueMax) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        float hue = rand.nextFloat() * (hueMax - hueMin) + hueMin;
        float r, g, b;
        if (hue < 1.0f / 3) {
            r = 1.0f;
            g = hue * 3;
            b = 0;
        } else if (hue < 2.0f / 3) {
            r = 1 - (hue - 1.0f / 3) * 3;
            g = 1.0f;
            b = (hue - 1.0f / 3) * 3;
        } else {
            r = 0;
            g = 1 - (hue - 2.0f / 3) * 3;
            b = 1.0f;
        }
        float bright = 0.85f + rand.nextFloat() * 0.15f;
        int ir = (int) (r * bright * 255);
        int ig = (int) (g * bright * 255);
        int ib = (int) (b * bright * 255);
        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }

    /**
     * 生成高饱和彩色，避免灰白区间，作为爆炸主体色
     */
    private static int randomVividColor() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        float hue = rand.nextFloat();
        float saturation = 0.85f + rand.nextFloat() * 0.15f;
        float bright = 0.85f + rand.nextFloat() * 0.15f;

        int sector = (int) (hue * 6) % 6;
        float f = hue * 6 - sector;
        float p = bright * (1 - saturation);
        float q = bright * (1 - f * saturation);
        float t = bright * (1 - (1 - f) * saturation);

        float r, g, b;
        switch (sector) {
            case 0 -> {
                r = bright;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = bright;
                b = p;
            }
            case 2 -> {
                r = p;
                g = bright;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = bright;
            }
            case 4 -> {
                r = t;
                g = p;
                b = bright;
            }
            default -> {
                r = bright;
                g = p;
                b = q;
            }
        }

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }

    // ==============================================
    // 主逻辑：多发礼花升空 + 爆炸
    // ==============================================
    @Override
    public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                             MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {
        Vec3 originPos = context.getVec3();
        if (originPos == null) return;

        Level level = context.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        double baseY = originPos.y();
        int rocketCount = intensity;
        long launchInterval = 200L;

        for (int i = 0; i < rocketCount; i++) {
            long launchDelay = i * launchInterval;
            ThreadLocalRandom rand = ThreadLocalRandom.current();

            int flyTime = 600 + rand.nextInt(400);
            double flyHeight = 1.0 + rand.nextDouble(1.5);
            int mainColor = randomVividColor();
            int secondaryColor = randomVividColor();
            int trailColor = randomHueColor(0.05f, 0.15f);

            double offsetAngle = rand.nextDouble() * Math.PI * 2;
            double offsetDist = rand.nextDouble() * 0.8 * intensity;
            double rocketX = originPos.x() + Math.cos(offsetAngle) * offsetDist;
            double rocketZ = originPos.z() + Math.sin(offsetAngle) * offsetDist;
            Vec3 explodePos = new Vec3(rocketX, baseY + flyHeight, rocketZ);

            // 发射音效
            playSoundDelayed(serverLevel, new Vec3(rocketX, baseY, rocketZ),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    14 + rand.nextFloat(6), 1.1f + rand.nextFloat(0.3f), launchDelay);

            // 上升尾迹：加量加厚，每50ms一批
            int trailSteps = flyTime / 50;
            for (int step = 0; step < trailSteps; step++) {
                long stepDelay = launchDelay + step * 50L;
                double progress = (double) step / trailSteps;
                double currentY = baseY + flyHeight * progress;
                SCHEDULER.schedule(() -> serverLevel.getServer().execute(() -> {
                    DustParticleOptions dustTrail = new DustParticleOptions(trailColor, 0.8f);
                    serverLevel.sendParticles(dustTrail, rocketX, currentY, rocketZ,
                            6, 0.15, 0.15, 0.15, 0.08);
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, rocketX, currentY, rocketZ,
                            5, 0.2, 0.2, 0.2, 0.15);
                }), stepDelay, TimeUnit.MILLISECONDS);
            }

            long explodeDelay = launchDelay + flyTime;

            // 3波次爆炸叠加，大幅延长存在时间
            spawnExplosionWave(serverLevel, explodePos, mainColor, secondaryColor, intensity, explodeDelay, 0);
            spawnExplosionWave(serverLevel, explodePos, mainColor, secondaryColor, intensity, explodeDelay, 120);
            spawnExplosionWave(serverLevel, explodePos, mainColor, secondaryColor, intensity, explodeDelay, 240);

            // 爆炸音效
            SoundEvent blastSound = (i % 2 == 0) ?
                    SoundEvents.FIREWORK_ROCKET_LARGE_BLAST : SoundEvents.FIREWORK_ROCKET_BLAST;
            playSoundDelayed(serverLevel, explodePos, blastSound,
                    14 + rand.nextFloat(6), 0.9f + rand.nextFloat(0.5f), explodeDelay);

            // 余烬闪烁音效
            if (intensity >= 2) {
                playSoundDelayed(serverLevel, explodePos, SoundEvents.FIREWORK_ROCKET_TWINKLE,
                        12 + rand.nextFloat(5), 1.2f + rand.nextFloat(0.4f),
                        explodeDelay + 150L);
            }

            // 4批持续余烬粒子，延长飘落时间
            for (int w = 0; w < 4; w++) {
                long emberDelay = explodeDelay + 200 + w * 200L;
                SCHEDULER.schedule(() -> serverLevel.getServer().execute(() -> {
                    int emberCount = 15 + intensity * 10;
                    double blastSpeed = 0.25 + intensity * 0.08;
                    // 彩色火花余烬
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, explodePos.x(), explodePos.y(), explodePos.z(),
                            emberCount, 0, 0, 0, blastSpeed);
                    // 彩色发光点
                    serverLevel.sendParticles(ParticleTypes.GLOW, explodePos.x(), explodePos.y(), explodePos.z(),
                            emberCount / 2, 0, 0, 0, blastSpeed * 0.7);
                }), emberDelay, TimeUnit.MILLISECONDS);
            }
        }

        // 强度3专属：高空大礼花
        if (intensity >= 3) {
            spawnGrandFirework(serverLevel, originPos, baseY);
        }
    }

    // ==============================================
    // 单波次爆炸：彩色粉尘为绝对主体
    // ==============================================
    private void spawnExplosionWave(ServerLevel level, Vec3 center, int mainColor, int secondaryColor,
                                    int strength, long baseDelay, long waveOffset) {
        SCHEDULER.schedule(() -> level.getServer().execute(() -> {
            ThreadLocalRandom.current();
            int baseCount = 80 + strength * 40;
            double blastSpeed = 0.25 + strength * 0.08; // 降低初速度，延长停留时间

            // 1. 主爆炸球：高饱和彩色 Dust，数量最多
            DustParticleOptions mainDust = new DustParticleOptions(mainColor, 1.25f);
            level.sendParticles(mainDust, center.x(), center.y(), center.z(),
                    baseCount, 0, 0, 0, blastSpeed);

            // 2. 第二层彩色粉尘：异色叠加，提升色彩丰富度
            DustParticleOptions secondaryDust = new DustParticleOptions(secondaryColor, 1.1f);
            level.sendParticles(secondaryDust, center.x(), center.y(), center.z(),
                    baseCount / 2, 0, 0, 0, blastSpeed * 0.9);

            // 3. 亮色点缀：仅占约 8%，不使用纯白
            int brightColor = randomBrightColor();
            DustParticleOptions brightDust = new DustParticleOptions(brightColor, 1.5f);
            level.sendParticles(brightDust, center.x(), center.y(), center.z(),
                    Math.max(5, baseCount / 12), 0, 0, 0, blastSpeed * 1.1);

            // 4. 核心闪光仅保留极少量点睛
            if (waveOffset == 0) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x(), center.y(), center.z(),
                        1, 0.2, 0.2, 0.2, 0);
            }

            // 5. 延迟花瓣环：彩色环，第二波出现
            if (waveOffset == 120) {
                int ringCount = 14 + strength * 5;
                double ringRadius = 1.1 + strength * 0.35;
                int ringColor = randomVividColor();
                DustParticleOptions ringDust = new DustParticleOptions(ringColor, 0.9f);
                for (int i = 0; i < ringCount; i++) {
                    double angle = 2 * Math.PI * i / ringCount;
                    double dx = Math.cos(angle) * ringRadius;
                    double dz = Math.sin(angle) * ringRadius;
                    level.sendParticles(ringDust,
                            center.x() + dx, center.y(), center.z() + dz,
                            3, 0.1, 0.1, 0.1, 0.12);
                }
            }
        }), baseDelay + waveOffset, TimeUnit.MILLISECONDS);
    }

    // ==============================================
    // 高空大礼花：多层彩色 + 多波次叠加
    // ==============================================
    private void spawnGrandFirework(ServerLevel level, Vec3 origin, double baseY) {
        long grandDelay = 400L;
        int grandFlyTime = 1200;
        double grandHeight = 5.5;
        Vec3 explodePos = new Vec3(origin.x(), baseY + grandHeight, origin.z());
        ThreadLocalRandom.current();

        // 发射音效
        playSoundDelayed(level, origin, SoundEvents.FIREWORK_ROCKET_LAUNCH,
                18f, 1.0f, grandDelay);

        // 粗壮上升尾迹：加量加厚
        for (int step = 0; step < grandFlyTime / 50; step++) {
            long stepDelay = grandDelay + step * 50L;
            double progress = (double) step / ((double) grandFlyTime / 50);
            double currentY = baseY + grandHeight * progress;
            SCHEDULER.schedule(() -> level.getServer().execute(() -> {
                DustParticleOptions dustTrail = new DustParticleOptions(0xFFFFCC66, 1.0f);
                level.sendParticles(dustTrail, origin.x(), currentY, origin.z(),
                        8, 0.2, 0.2, 0.2, 0.1);
                level.sendParticles(ParticleTypes.FIREWORK, origin.x(), currentY, origin.z(),
                        6, 0.25, 0.25, 0.25, 0.2);
            }), stepDelay, TimeUnit.MILLISECONDS);
        }

        long explodeDelay = grandDelay + grandFlyTime;

        // 大礼花 3 波次爆炸叠加
        for (int wave = 0; wave < 3; wave++) {
            long waveDelay = explodeDelay + wave * 150L;
            SCHEDULER.schedule(() -> level.getServer().execute(() -> {
                // 三层彩色爆炸球，全高饱和彩色 Dust
                int[] layers = { 200, 140, 80 };
                double[] speeds = { 0.35, 0.28, 0.2 };
                int[] colors = {
                        randomVividColor(),
                        randomVividColor(),
                        randomVividColor()
                };
                for (int i = 0; i < layers.length; i++) {
                    DustParticleOptions dust = new DustParticleOptions(colors[i], 1.3f - i * 0.2f);
                    level.sendParticles(dust, explodePos.x(), explodePos.y(), explodePos.z(),
                            layers[i], 0, 0, 0, speeds[i]);
                }
            }), waveDelay, TimeUnit.MILLISECONDS);
        }

        // 花瓣环 + 放射射线：第二波出现
        SCHEDULER.schedule(() -> level.getServer().execute(() -> {
            // 彩色花瓣环
            int ringCount = 24;
            double ringRadius = 1.8;
            int ringColor = randomVividColor();
            DustParticleOptions ringDust = new DustParticleOptions(ringColor, 1.0f);
            for (int i = 0; i < ringCount; i++) {
                double angle = 2 * Math.PI * i / ringCount;
                double dx = Math.cos(angle) * ringRadius;
                double dz = Math.sin(angle) * ringRadius;
                level.sendParticles(ringDust,
                        explodePos.x() + dx, explodePos.y(), explodePos.z() + dz,
                        4, 0.15, 0.15, 0.15, 0.15);
            }

            // 彩色放射射线
            int rays = 10;
            double rayLength = 3.0;
            for (int k = 0; k < rays; k++) {
                double angle = 2 * Math.PI * k / rays;
                double dx = Math.cos(angle) * rayLength;
                double dz = Math.sin(angle) * rayLength;
                int rayColor = randomVividColor();
                DustParticleOptions rayDust = new DustParticleOptions(rayColor, 1.1f);
                for (int step = 0; step < 12; step++) {
                    double t = step / 11.0;
                    level.sendParticles(rayDust,
                            explodePos.x() + dx * t,
                            explodePos.y() + Math.sin(angle * 2) * 0.5 * t,
                            explodePos.z() + dz * t,
                            2, 0.05, 0.05, 0.05, 0.06);
                }
            }

            // 核心闪光仅少量
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, explodePos.x(), explodePos.y(), explodePos.z(),
                    2, 0.3, 0.3, 0.3, 0);
        }), explodeDelay + 150, TimeUnit.MILLISECONDS);

        // 大礼花持续余烬：5批，飘落更久
        for (int w = 0; w < 5; w++) {
            long emberDelay = explodeDelay + 300 + w * 250L;
            SCHEDULER.schedule(() -> level.getServer().execute(() -> {
                level.sendParticles(ParticleTypes.FIREWORK, explodePos.x(), explodePos.y(), explodePos.z(),
                        50, 0, 0, 0, 0.3);
                level.sendParticles(ParticleTypes.GLOW, explodePos.x(), explodePos.y(), explodePos.z(),
                        20, 0, 0, 0, 0.22);
            }), emberDelay, TimeUnit.MILLISECONDS);
        }

        // 爆炸音效
        playSoundDelayed(level, explodePos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                22f, 0.85f, explodeDelay);
        playSoundDelayed(level, explodePos, SoundEvents.FIREWORK_ROCKET_TWINKLE,
                18f, 1.1f, explodeDelay + 200L);
    }

    // ==============================================
    // 延迟播放音效工具
    // ==============================================
    private void playSoundDelayed(ServerLevel level, Vec3 pos, SoundEvent sound,
                                  float volume, float pitch, long delay) {
        SCHEDULER.schedule(() -> level.getServer().execute(() -> level.playSound(null, pos.x(), pos.y(), pos.z(), sound,
                SoundSource.MASTER, volume, pitch)), delay, TimeUnit.MILLISECONDS);
    }
}
