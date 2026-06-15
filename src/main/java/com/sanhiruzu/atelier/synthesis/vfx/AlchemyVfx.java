package com.sanhiruzu.atelier.synthesis.vfx;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisExecutionResult;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.world.ItemSourceSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class AlchemyVfx {
    private static final AlchemyVfxStyle READY_SOLVENT = new AlchemyVfxStyle(0.24F, 0.95F, 0.48F, 0.9F, ParticleTypes.HAPPY_VILLAGER);
    private static final AlchemyVfxStyle EXTRACTING = new AlchemyVfxStyle(1.0F, 0.55F, 0.12F, 1.05F, ParticleTypes.BUBBLE);
    private static final AlchemyVfxStyle REJECT = new AlchemyVfxStyle(0.95F, 0.12F, 0.08F, 1.0F, ParticleTypes.SMOKE);

    private AlchemyVfx() {
    }

    public static void extractionPrimed(ServerLevel level, BlockPos pos) {
        dustBurst(level, pos, READY_SOLVENT, 18, 0.95D, 0.28D, 0.03D, 0.28D, 0.02D);
        particleBurst(level, pos, ParticleTypes.HAPPY_VILLAGER, 6, 1.02D, 0.25D, 0.12D, 0.25D, 0.02D);
    }

    public static void extractionStarted(ServerLevel level, BlockPos pos, ItemSourceSnapshot source) {
        AlchemyVfxStyle sourceStyle = styleForSource(source.itemId());
        particleBurst(level, pos, ParticleTypes.BUBBLE, 10, 0.9D, 0.25D, 0.1D, 0.25D, 0.03D);
        particleBurst(level, pos, ParticleTypes.SPLASH, 6, 0.95D, 0.22D, 0.04D, 0.22D, 0.02D);
        dustBurst(level, pos, sourceStyle, 10, 0.98D, 0.22D, 0.03D, 0.22D, 0.035D);
        dustBurst(level, pos, EXTRACTING, 8, 0.94D, 0.28D, 0.03D, 0.28D, 0.03D);
    }

    public static void ingredientDissolving(ServerLevel level, BlockPos pos, ItemSourceSnapshot source, double progress) {
        AlchemyVfxStyle sourceStyle = styleForSource(source.itemId());
        int count = progress > 0.72D ? 8 : 3;
        double radius = Math.max(0.08D, 0.28D * (1.0D - progress));
        dustBurst(level, pos, sourceStyle, count, 1.04D - progress * 0.23D, radius, 0.04D, radius, 0.035D);
        if (progress > 0.78D) {
            particleBurst(level, pos, ParticleTypes.SMOKE, 2, 0.9D, 0.16D, 0.05D, 0.16D, 0.015D);
        }
    }

    public static void ingredientDissolved(ServerLevel level, BlockPos pos, ItemSourceSnapshot source) {
        AlchemyVfxStyle sourceStyle = styleForSource(source.itemId());
        particleBurst(level, pos, ParticleTypes.SPLASH, 10, 0.88D, 0.22D, 0.08D, 0.22D, 0.04D);
        dustBurst(level, pos, sourceStyle, 20, 0.95D, 0.34D, 0.12D, 0.34D, 0.08D);
        particleBurst(level, pos, sourceStyle.accent(), 6, 0.98D, 0.28D, 0.12D, 0.28D, 0.06D);
    }

    public static void extractionTick(ServerLevel level, BlockPos pos, ItemSourceSnapshot source, double progress, double volatility) {
        AlchemyVfxStyle sourceStyle = styleForSource(source.itemId());
        int heatCount = progress > 0.75D ? 5 : 2;
        double pulse = progress > 0.8D ? 0.08D : 0.025D;
        dustBurst(level, pos, EXTRACTING, 5 + (int) Math.round(volatility * 5.0D), 0.96D, 0.25D, 0.03D, 0.25D, 0.02D + pulse);
        dustBurst(level, pos, sourceStyle, Math.max(2, heatCount), 1.03D, 0.22D, 0.08D, 0.22D, 0.035D + pulse);
        particleBurst(level, pos, ParticleTypes.BUBBLE, 4 + (int) Math.round(progress * 5.0D), 0.9D, 0.25D, 0.08D, 0.25D, 0.02D);
        emitHeatShimmer(level, pos, 2 + (int) Math.round(progress * 3.0D));
        emitGlowPulse(level, pos, sourceStyle, progress);
        if (volatility > 0.25D || progress > 0.65D) {
            emitMist(level, pos, sourceStyle, volatility, progress);
        }
        if (volatility > 0.36D) {
            emitInstability(level, pos, sourceStyle, volatility, progress);
        }
    }

    public static void extractionCompleted(ServerLevel level, BlockPos pos, List<ReagentStack> reagents) {
        AlchemyVfxStyle resultStyle = styleForReagents(reagents);
        particleBurst(level, pos, ParticleTypes.ENCHANT, 18, 1.05D, 0.35D, 0.2D, 0.35D, 0.08D);
        dustBurst(level, pos, resultStyle, 20, 1.02D, 0.32D, 0.12D, 0.32D, 0.06D);
        dustBurst(level, pos, READY_SOLVENT, 12, 0.95D, 0.28D, 0.03D, 0.28D, 0.02D);
    }

    public static void reagentPopped(ServerLevel level, BlockPos pos, ReagentStack reagent) {
        dustBurst(level, pos, styleForReagentId(reagent.reagentId()), 8, 1.22D, 0.12D, 0.05D, 0.12D, 0.04D);
    }

    public static void gatheringCollected(ServerLevel level, Vec3 center, ReagentStack reagent) {
        AlchemyVfxStyle style = styleForReagentId(reagent.reagentId());
        particleBurst(level, center.add(0.0D, 0.92D, 0.0D), ParticleTypes.GLOW, 4, 0.16D, 0.1D, 0.16D, 0.02D);
        dustBurst(level, center.add(0.0D, 0.78D, 0.0D), style, 7, 0.2D, 0.1D, 0.2D, 0.025D);
        particleBurst(level, center.add(0.0D, 0.08D, 0.0D), ParticleTypes.POOF, 2, 0.1D, 0.03D, 0.1D, 0.005D);
    }

    public static void extractionRejected(ServerLevel level, BlockPos pos, int particleCount) {
        dustBurst(level, pos, REJECT, particleCount, 1.0D, 0.28D, 0.08D, 0.28D, 0.04D);
        particleBurst(level, pos, ParticleTypes.SMOKE, Math.max(3, particleCount / 2), 1.0D, 0.22D, 0.08D, 0.22D, 0.03D);
    }

    public static void synthesisCompleted(ServerLevel level, BlockPos pos, SynthesisProfile profile, SynthesisExecutionResult result) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.82D, 0.0D);
        AlchemyVfxStyle categoryStyle = styleForCategory(profile.category());
        AlchemyVfxStyle reagentStyle = styleForReagents(result.consumedReagents());
        int richness = result.result().successful() ? 24 : 10;

        particleBurst(level, center, categoryStyle.accent(), Math.max(6, richness / 3), 0.35D, 0.22D, 0.35D, 0.05D);
        dustBurst(level, center, categoryStyle, richness, 0.36D, 0.18D, 0.36D, 0.05D);
        dustBurst(level, center, reagentStyle, Math.max(8, richness / 2), 0.28D, 0.12D, 0.28D, 0.035D);
        if (!result.result().successful()) {
            particleBurst(level, center.add(0.0D, 0.06D, 0.0D), ParticleTypes.SMOKE, 30, 0.5D, 0.18D, 0.5D, 0.035D);
            particleBurst(level, center.add(0.0D, -0.08D, 0.0D), ParticleTypes.CLOUD, 18, 0.62D, 0.12D, 0.62D, 0.012D);
            dustBurst(level, center.add(0.0D, 0.03D, 0.0D), REJECT, 26, 0.46D, 0.2D, 0.46D, 0.055D);
        }
        emitSynthesisAfterglow(level, center, categoryStyle, result.result().successful());
    }

    private static AlchemyVfxStyle styleForReagents(Collection<ReagentStack> reagents) {
        return reagents.stream()
                .map(ReagentStack::reagentId)
                .findFirst()
                .map(AlchemyVfx::styleForReagentId)
                .orElse(READY_SOLVENT);
    }

    private static AlchemyVfxStyle styleForSource(String sourceId) {
        String key = key(sourceId);
        if (key.contains("redstone")) {
            return new AlchemyVfxStyle(0.95F, 0.12F, 0.16F, 1.0F, ParticleTypes.ELECTRIC_SPARK);
        }
        if (key.contains("glowstone")) {
            return new AlchemyVfxStyle(1.0F, 0.82F, 0.22F, 1.05F, ParticleTypes.ENCHANT);
        }
        if (key.contains("slime")) {
            return new AlchemyVfxStyle(0.45F, 1.0F, 0.24F, 1.0F, ParticleTypes.ITEM_SLIME);
        }
        if (key.contains("amethyst")) {
            return new AlchemyVfxStyle(0.72F, 0.48F, 1.0F, 1.0F, ParticleTypes.ENCHANT);
        }
        if (key.contains("honey") || key.contains("sugar")) {
            return new AlchemyVfxStyle(1.0F, 0.66F, 0.18F, 0.95F, ParticleTypes.HAPPY_VILLAGER);
        }
        if (key.contains("copper")) {
            return new AlchemyVfxStyle(0.88F, 0.46F, 0.25F, 1.0F, ParticleTypes.ELECTRIC_SPARK);
        }
        if (key.contains("rotten")) {
            return new AlchemyVfxStyle(0.42F, 0.62F, 0.2F, 0.95F, ParticleTypes.SPORE_BLOSSOM_AIR);
        }
        if (key.contains("kelp") || key.contains("bone")) {
            return new AlchemyVfxStyle(0.42F, 0.9F, 0.42F, 0.95F, ParticleTypes.HAPPY_VILLAGER);
        }
        return READY_SOLVENT;
    }

    private static AlchemyVfxStyle styleForReagentId(String reagentId) {
        String key = key(reagentId);
        if (key.contains("spark") || key.contains("fire")) {
            return new AlchemyVfxStyle(1.0F, 0.48F, 0.12F, 1.05F, ParticleTypes.FLAME);
        }
        if (key.contains("conductive") || key.contains("charged")) {
            return new AlchemyVfxStyle(0.95F, 0.15F, 0.2F, 1.0F, ParticleTypes.ELECTRIC_SPARK);
        }
        if (key.contains("binding") || key.contains("preserving") || key.contains("sticky")) {
            return new AlchemyVfxStyle(1.0F, 0.68F, 0.18F, 0.95F, ParticleTypes.HAPPY_VILLAGER);
        }
        if (key.contains("organic") || key.contains("verdant") || key.contains("life")) {
            return new AlchemyVfxStyle(0.38F, 0.9F, 0.38F, 0.95F, ParticleTypes.HAPPY_VILLAGER);
        }
        if (key.contains("luminous") || key.contains("glow")) {
            return new AlchemyVfxStyle(1.0F, 0.92F, 0.32F, 1.05F, ParticleTypes.ENCHANT);
        }
        if (key.contains("harmonic") || key.contains("resonant") || key.contains("crystal")) {
            return new AlchemyVfxStyle(0.76F, 0.52F, 1.0F, 1.0F, ParticleTypes.ENCHANT);
        }
        if (key.contains("elastic") || key.contains("slime")) {
            return new AlchemyVfxStyle(0.48F, 1.0F, 0.22F, 1.0F, ParticleTypes.ITEM_SLIME);
        }
        if (key.contains("fibrous") || key.contains("thread")) {
            return new AlchemyVfxStyle(0.88F, 0.86F, 0.78F, 0.85F, ParticleTypes.CRIT);
        }
        if (key.contains("abrasive") || key.contains("stone")) {
            return new AlchemyVfxStyle(0.68F, 0.65F, 0.56F, 0.9F, ParticleTypes.CRIT);
        }
        return READY_SOLVENT;
    }

    private static AlchemyVfxStyle styleForCategory(String category) {
        return switch (SynthesisRecipeCategory.normalize(category)) {
            case "bombs" -> new AlchemyVfxStyle(1.0F, 0.28F, 0.12F, 1.2F, ParticleTypes.FLAME);
            case "healing" -> new AlchemyVfxStyle(0.35F, 1.0F, 0.52F, 1.0F, ParticleTypes.HEART);
            case "food" -> new AlchemyVfxStyle(1.0F, 0.72F, 0.28F, 0.95F, ParticleTypes.HAPPY_VILLAGER);
            case "tools" -> new AlchemyVfxStyle(0.38F, 0.66F, 1.0F, 1.0F, ParticleTypes.ELECTRIC_SPARK);
            case "materials" -> new AlchemyVfxStyle(0.78F, 0.65F, 0.42F, 0.95F, ParticleTypes.ENCHANT);
            default -> new AlchemyVfxStyle(0.66F, 0.52F, 0.9F, 1.0F, ParticleTypes.ENCHANT);
        };
    }

    private static void emitHeatShimmer(ServerLevel level, BlockPos pos, int count) {
        Vec3 burner = Vec3.atCenterOf(pos.below()).add(0.0D, 0.68D, 0.0D);
        particleBurst(level, burner, ParticleTypes.SMOKE, Math.max(1, count / 2), 0.18D, 0.1D, 0.18D, 0.006D);
        particleBurst(level, burner, ParticleTypes.FLAME, Math.max(1, count / 3), 0.12D, 0.08D, 0.12D, 0.01D);
    }

    private static void emitGlowPulse(ServerLevel level, BlockPos pos, AlchemyVfxStyle style, double progress) {
        if (progress < 0.45D) {
            return;
        }
        int count = progress > 0.88D ? 4 : 2;
        dustBurst(level, pos, style, count, 1.08D, 0.2D, 0.08D, 0.2D, 0.02D + progress * 0.06D);
        particleBurst(level, pos, ParticleTypes.GLOW, Math.max(1, count / 2), 1.08D, 0.18D, 0.08D, 0.18D, 0.02D);
    }

    private static void emitMist(ServerLevel level, BlockPos pos, AlchemyVfxStyle style, double volatility, double progress) {
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.82D, 0.0D);
        int count = 1 + (int) Math.round(volatility * 4.0D + progress * 2.0D);
        particleBurst(level, center, ParticleTypes.CLOUD, count, 0.42D, 0.05D, 0.42D, 0.005D);
        if (volatility > 0.42D) {
            dustBurst(level, center.add(0.0D, -0.18D, 0.0D), style, 3, 0.5D, 0.04D, 0.5D, 0.012D);
            particleBurst(level, center.add(0.0D, -0.28D, 0.0D), ParticleTypes.WHITE_ASH, 2, 0.55D, 0.03D, 0.55D, 0.006D);
        }
    }

    private static void emitInstability(ServerLevel level, BlockPos pos, AlchemyVfxStyle style, double volatility, double progress) {
        int warning = 1 + (int) Math.round(volatility * 5.0D);
        double spread = 0.18D + volatility * 0.22D;
        particleBurst(level, pos, ParticleTypes.ELECTRIC_SPARK, warning, 1.04D, spread, 0.12D, spread, 0.08D + progress * 0.04D);
        dustBurst(level, pos, REJECT, Math.max(1, warning / 2), 1.02D, spread, 0.08D, spread, 0.05D);
        if (progress > 0.85D) {
            particleBurst(level, pos, style.accent(), warning, 1.1D, spread, 0.16D, spread, 0.1D);
        }
    }

    private static void emitSynthesisAfterglow(ServerLevel level, Vec3 center, AlchemyVfxStyle style, boolean successful) {
        particleBurst(level, center.add(0.0D, 0.12D, 0.0D), successful ? ParticleTypes.GLOW : ParticleTypes.SMOKE,
                successful ? 8 : 12, 0.5D, 0.22D, 0.5D, successful ? 0.04D : 0.015D);
        if (!successful) {
            dustBurst(level, center.add(0.0D, 0.04D, 0.0D), REJECT, 14, 0.42D, 0.18D, 0.42D, 0.04D);
            particleBurst(level, center.add(0.0D, -0.2D, 0.0D), ParticleTypes.CLOUD, 8, 0.55D, 0.08D, 0.55D, 0.005D);
        } else {
            dustBurst(level, center.add(0.0D, 0.02D, 0.0D), style, 10, 0.48D, 0.12D, 0.48D, 0.025D);
        }
    }

    private static void dustBurst(ServerLevel level, BlockPos pos, AlchemyVfxStyle style, int count,
                                  double y, double dx, double dy, double dz, double speed) {
        dustBurst(level, Vec3.atCenterOf(pos).add(0.0D, y - 0.5D, 0.0D), style, count, dx, dy, dz, speed);
    }

    private static void dustBurst(ServerLevel level, Vec3 center, AlchemyVfxStyle style, int count,
                                  double dx, double dy, double dz, double speed) {
        level.sendParticles(style.dust(), center.x, center.y, center.z, count, dx, dy, dz, speed);
    }

    private static void particleBurst(ServerLevel level, BlockPos pos, ParticleOptions particle, int count,
                                      double y, double dx, double dy, double dz, double speed) {
        particleBurst(level, Vec3.atCenterOf(pos).add(0.0D, y - 0.5D, 0.0D), particle, count, dx, dy, dz, speed);
    }

    private static void particleBurst(ServerLevel level, Vec3 center, ParticleOptions particle, int count,
                                      double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, center.x, center.y, center.z, count, dx, dy, dz, speed);
    }

    private static String key(String id) {
        if (id == null) {
            return "";
        }
        String clean = id.startsWith("#") ? id.substring(1) : id;
        String path = clean.contains(":") ? clean.substring(clean.indexOf(':') + 1) : clean;
        return path.toLowerCase(Locale.ROOT);
    }

    private record AlchemyVfxStyle(float red, float green, float blue, float scale, ParticleOptions accent) {
        private DustParticleOptions dust() {
            return new DustParticleOptions(new Vector3f(red, green, blue), scale);
        }
    }
}
