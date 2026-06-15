package com.sanhiruzu.atelier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.data.SynthesisCatalog;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionExecutionResult;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionExecutor;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import com.sanhiruzu.atelier.synthesis.engine.OutcomeWeight;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisExecutionResult;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisExecutor;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.item.CarriedReagentInventory;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import com.sanhiruzu.atelier.synthesis.world.ReagentCabinetSavedData;
import com.sanhiruzu.atelier.synthesis.world.ItemSourceSnapshot;
import com.sanhiruzu.atelier.synthesis.world.PlayerExtractionKnowledge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SynthesisDebugCommand {
    private SynthesisDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("atelier")
                .then(Commands.literal("synthesis")
                        .then(Commands.literal("preview")
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .executes(ctx -> preview(ctx.getSource(), StringArgumentType.getString(ctx, "profile"), 0))
                                        .then(Commands.argument("risk", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> preview(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "profile"),
                                                        IntegerArgumentType.getInteger(ctx, "risk")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("execute")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("profile", StringArgumentType.word())
                                                                .executes(ctx -> executeSynthesis(ctx, 0, defaultSeed(ctx.getSource())))
                                                                .then(Commands.argument("risk", IntegerArgumentType.integer(0, 100))
                                                                        .executes(ctx -> executeSynthesis(
                                                                                ctx,
                                                                                IntegerArgumentType.getInteger(ctx, "risk"),
                                                                                defaultSeed(ctx.getSource())
                                                                        ))
                                                                        .then(Commands.argument("seed", StringArgumentType.word())
                                                                                .executes(ctx -> executeSynthesis(
                                                                                        ctx,
                                                                                        IntegerArgumentType.getInteger(ctx, "risk"),
                                                                                        parseSeed(ctx.getSource(), StringArgumentType.getString(ctx, "seed"))
                                                                                ))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("execute_carried")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .executes(ctx -> executeCarriedSynthesis(ctx, 0, defaultSeed(ctx.getSource())))
                                        .then(Commands.argument("risk", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> executeCarriedSynthesis(
                                                        ctx,
                                                        IntegerArgumentType.getInteger(ctx, "risk"),
                                                        defaultSeed(ctx.getSource())
                                                ))
                                                .then(Commands.argument("seed", StringArgumentType.word())
                                                        .executes(ctx -> executeCarriedSynthesis(
                                                                ctx,
                                                                IntegerArgumentType.getInteger(ctx, "risk"),
                                                                parseSeed(ctx.getSource(), StringArgumentType.getString(ctx, "seed"))
                                                        ))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("extraction")
                        .then(Commands.literal("inspect_item")
                                .executes(ctx -> inspectItem(ctx.getSource()))
                        )
                        .then(Commands.literal("extract_held")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> extractHeld(ctx, 0, defaultSeed(ctx.getSource())))
                                                        .then(Commands.argument("risk", IntegerArgumentType.integer(0, 100))
                                                                .executes(ctx -> extractHeld(
                                                                        ctx,
                                                                        IntegerArgumentType.getInteger(ctx, "risk"),
                                                                        defaultSeed(ctx.getSource())
                                                                ))
                                                                .then(Commands.argument("seed", StringArgumentType.word())
                                                                        .executes(ctx -> extractHeld(
                                                                                ctx,
                                                                                IntegerArgumentType.getInteger(ctx, "risk"),
                                                                                parseSeed(ctx.getSource(), StringArgumentType.getString(ctx, "seed"))
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("reagent")
                        .then(Commands.literal("dump_storage")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(SynthesisDebugCommand::dumpStorage)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("give")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("reagent", StringArgumentType.word())
                                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> giveReagent(ctx, 1))
                                                                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 6))
                                                                                .executes(ctx -> giveReagent(ctx, IntegerArgumentType.getInteger(ctx, "tier")))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(SynthesisDebugCommand::clearStorage)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("fill_for")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("profile", StringArgumentType.word())
                                                                .executes(SynthesisDebugCommand::fillForRecipe)
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int preview(CommandSourceStack source, String profileId, int risk) {
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(profileId);
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Invalid synthesis profile id: " + profileId));
            return 0;
        }

        Optional<SynthesisProfile> profile = SynthesisCatalog.getSynthesisProfile(id);
        if (profile.isEmpty()) {
            source.sendFailure(Component.literal("Unknown synthesis profile: " + id));
            return 0;
        }

        OutcomePreview preview = OutcomePreview.forSynthesis(profile.get().outcomes(), risk);
        source.sendSuccess(() -> Component.literal("Synthesis preview " + id + " at risk " + risk), false);
        source.sendSuccess(() -> Component.literal("Success: " + percent(preview.successProbability())
                + " Failure: " + percent(preview.failureProbability())), false);
        for (OutcomeWeight weight : preview.weights()) {
            source.sendSuccess(() -> Component.literal(
                    weight.outcomeClass().name().toLowerCase(Locale.ROOT)
                            + " "
                            + percent(weight.probability())
                            + " (weight "
                            + weight.adjustedWeight()
                            + ", base "
                            + weight.baseWeight()
                            + ")"
            ), false);
        }
        return 1;
    }

    private static int inspectItem(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand to inspect extraction profiles."));
            return 0;
        }

        ItemSourceSnapshot snapshot = ItemSourceSnapshot.fromStack(stack);
        List<ExtractionProfile> profiles = SynthesisCatalog.findExtractionProfiles(snapshot.itemId(), snapshot.tags());
        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No extraction profiles match " + snapshot.itemId()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Extraction profiles for " + snapshot.itemId() + ":"), false);
        for (ExtractionProfile profile : profiles) {
            source.sendSuccess(() -> Component.literal(
                    profile.id()
                            + " source="
                            + profile.sourceKey()
                            + " sourceCap="
                            + profile.sourceTierCap()
                            + " outcomes="
                            + profile.outcomes().size()
            ), false);
        }
        return profiles.size();
    }

    private static int dumpStorage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        ReagentContainerSnapshot snapshot = ReagentCabinetSavedData.get(level).getSnapshot(pos);
        source.sendSuccess(() -> Component.literal("Reagent cabinet " + formatPos(pos) + ":"), false);
        sendSnapshot(source, snapshot);
        return snapshot.entries().size();
    }

    private static int giveReagent(CommandContext<CommandSourceStack> ctx, int tier) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        String reagent = StringArgumentType.getString(ctx, "reagent");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        ReagentContainer container = data.getContainer(pos);
        try {
            container.insert(ReagentStack.simple(reagent, amount, tier));
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid reagent: " + ex.getMessage()));
            return 0;
        }
        data.putContainer(pos, container);
        source.sendSuccess(() -> Component.literal("Added " + amount + " " + reagent
                + " tier " + tier + " to cabinet " + formatPos(pos)), true);
        return amount;
    }

    private static int clearStorage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        ReagentCabinetSavedData.get(level).clear(pos);
        source.sendSuccess(() -> Component.literal("Cleared reagent cabinet " + formatPos(pos)), true);
        return 1;
    }

    private static int fillForRecipe(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        Optional<SynthesisProfile> profile = parseSynthesisProfile(source, StringArgumentType.getString(ctx, "profile"));
        if (profile.isEmpty()) {
            return 0;
        }

        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        ReagentContainer container = data.getContainer(pos);
        int total = 0;
        for (SynthesisRequirement req : profile.get().requirements()) {
            ReagentStack debug = new ReagentStack(
                    ReagentQuery.DEBUG_UNIVERSAL_REAGENT_ID,
                    java.util.Set.of(), req.amount(), 1, 0, 0, 0,
                    java.util.Map.of(), java.util.List.of(),
                    com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE, java.util.Set.of()
            );
            container.insert(debug);
            total += req.amount();
        }
        data.putContainer(pos, container);

        int finalTotal = total;
        source.sendSuccess(() -> Component.literal("Filled cabinet " + formatPos(pos)
                + " with " + finalTotal + " debug universal reagents for " + profile.get().id()), true);
        return total;
    }

    private static int extractHeld(CommandContext<CommandSourceStack> ctx, int risk, long seed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand to extract from it."));
            return 0;
        }

        ItemSourceSnapshot sourceSnapshot = ItemSourceSnapshot.fromStack(stack);
        List<ExtractionProfile> profiles = SynthesisCatalog.findExtractionProfiles(sourceSnapshot.itemId(), sourceSnapshot.tags());
        if (profiles.isEmpty()) {
            source.sendFailure(Component.literal("No extraction profiles match " + sourceSnapshot.itemId()));
            return 0;
        }

        ExtractionProfile profile = profiles.getFirst();
        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        ReagentContainer container = data.getContainer(pos);
        ExtractionExecutionResult result = new ExtractionExecutor().execute(
                profile,
                1,
                container,
                attemptContext(level, pos, risk),
                seed
        );
        PlayerExtractionKnowledge.recordSuccess(player, sourceSnapshot, result.depositedReagents());
        data.putContainer(pos, container);

        if (!player.getAbilities().instabuild) {
            stack.shrink(result.consumedSourceAmount());
        }

        source.sendSuccess(() -> Component.literal("Extracted " + sourceSnapshot.itemId()
                + " using " + profile.id()
                + " -> " + result.result().outcomeClass().name().toLowerCase(Locale.ROOT)
                + " (cap " + result.result().effectiveTierCap() + ")"), true);
        sendDeposits(source, result.depositedReagents());
        return result.depositedReagents().stream().mapToInt(ReagentStack::amount).sum();
    }

    private static int executeSynthesis(CommandContext<CommandSourceStack> ctx, int risk, long seed) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = position(ctx);
        if (!requirePlaceholderCabinet(source, level, pos)) {
            return 0;
        }

        Optional<SynthesisProfile> profile = parseSynthesisProfile(source, StringArgumentType.getString(ctx, "profile"));
        if (profile.isEmpty()) {
            return 0;
        }

        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        ReagentContainer container = data.getContainer(pos);
        AttemptContext context = attemptContext(level, pos, risk);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile.get(), container, context.risk());
        if (!plan.canSynthesize()) {
            source.sendFailure(Component.literal("Missing reagents for " + profile.get().id() + ":"));
            sendMissingRequirements(source, plan);
            return 0;
        }

        SynthesisExecutionResult result = new SynthesisExecutor().execute(profile.get(), container, context, seed);
        for (ReagentStack byproduct : result.result().byproducts()) {
            container.insert(byproduct);
        }
        data.putContainer(pos, container);

        source.sendSuccess(() -> Component.literal("Synthesized " + profile.get().id()
                + " -> " + result.result().outcomeClass().name().toLowerCase(Locale.ROOT)
                + " (cap " + result.result().effectiveTierCap() + ")"), true);
        sendConsumed(source, result.consumedReagents());
        sendOutputs(source, result.result().outputs());
        sendByproducts(source, result.result().byproducts(), "Byproducts returned to cabinet:");
        return result.result().outputs().stream().mapToInt(SynthesisOutput::count).sum();
    }

    private static int executeCarriedSynthesis(CommandContext<CommandSourceStack> ctx, int risk, long seed)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Optional<SynthesisProfile> profile = parseSynthesisProfile(source, StringArgumentType.getString(ctx, "profile"));
        if (profile.isEmpty()) {
            return 0;
        }

        ReagentContainer container = CarriedReagentInventory.snapshot(player.getInventory());
        AttemptContext context = attemptContext(source.getLevel(), player.blockPosition(), risk);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile.get(), container, context.risk());
        if (!plan.canSynthesize()) {
            source.sendFailure(Component.literal("Missing carried reagents for " + profile.get().id() + ":"));
            sendMissingRequirements(source, plan);
            return 0;
        }

        SynthesisExecutionResult result = new SynthesisExecutor().execute(profile.get(), container, context, seed);
        if (!CarriedReagentInventory.consume(player.getInventory(), result.consumedReagents())) {
            source.sendFailure(Component.literal("Carried reagents changed before synthesis could consume them."));
            return 0;
        }

        for (ReagentStack byproduct : result.result().byproducts()) {
            giveOrDrop(player, ReagentItem.createStack(byproduct));
        }

        source.sendSuccess(() -> Component.literal("Synthesized from carried reagents " + profile.get().id()
                + " -> " + result.result().outcomeClass().name().toLowerCase(Locale.ROOT)
                + " (cap " + result.result().effectiveTierCap() + ")"), true);
        sendConsumed(source, result.consumedReagents());
        sendOutputs(source, result.result().outputs());
        sendByproducts(source, result.result().byproducts(), "Byproducts added to inventory:");
        return result.result().outputs().stream().mapToInt(SynthesisOutput::count).sum();
    }

    private static AttemptContext attemptContext(ServerLevel level, BlockPos pos, int risk) {
        return new AttemptContext(
                ApparatusState.crude("zen_atelier:placeholder_extractor"),
                com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext.neutral(),
                6,
                risk
        );
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void sendSnapshot(CommandSourceStack source, ReagentContainerSnapshot snapshot) {
        if (snapshot.entries().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  empty"), false);
            return;
        }
        for (ReagentStack stack : snapshot.entries()) {
            source.sendSuccess(() -> Component.literal("  " + formatStack(stack)), false);
        }
    }

    private static void sendDeposits(CommandSourceStack source, List<ReagentStack> deposits) {
        if (deposits.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Deposited no reagents."), false);
            return;
        }
        source.sendSuccess(() -> Component.literal("Deposited:"), false);
        for (ReagentStack stack : deposits) {
            source.sendSuccess(() -> Component.literal("  " + formatStack(stack)), false);
        }
    }

    private static void sendMissingRequirements(CommandSourceStack source, SynthesisPlan plan) {
        for (RequirementStatus status : plan.requirements()) {
            if (status.satisfied()) {
                continue;
            }
            source.sendFailure(Component.literal("  need "
                    + status.requirement().amount()
                    + ", have "
                    + status.availableAmount()
                    + ", missing "
                    + status.missingAmount()
                    + " for "
                    + status.requirement().query()));
        }
    }

    private static void sendConsumed(CommandSourceStack source, List<ReagentStack> consumed) {
        source.sendSuccess(() -> Component.literal("Consumed:"), false);
        for (ReagentStack stack : consumed) {
            source.sendSuccess(() -> Component.literal("  " + formatStack(stack)), false);
        }
    }

    private static void sendOutputs(CommandSourceStack source, List<SynthesisOutput> outputs) {
        if (outputs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Outputs: none"), false);
            return;
        }
        source.sendSuccess(() -> Component.literal("Outputs:"), false);
        for (SynthesisOutput output : outputs) {
            source.sendSuccess(() -> Component.literal("  " + formatOutput(output)), false);
        }
    }

    private static void sendByproducts(CommandSourceStack source, List<ReagentStack> byproducts, String heading) {
        if (byproducts.isEmpty()) {
            return;
        }
        source.sendSuccess(() -> Component.literal(heading), false);
        for (ReagentStack stack : byproducts) {
            source.sendSuccess(() -> Component.literal("  " + formatStack(stack)), false);
        }
    }

    private static Optional<SynthesisProfile> parseSynthesisProfile(CommandSourceStack source, String profileId) {
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(profileId);
        } catch (Exception ex) {
            source.sendFailure(Component.literal("Invalid synthesis profile id: " + profileId));
            return Optional.empty();
        }

        Optional<SynthesisProfile> profile = SynthesisCatalog.getSynthesisProfile(id);
        if (profile.isEmpty()) {
            source.sendFailure(Component.literal("Unknown synthesis profile: " + id));
        }
        return profile;
    }

    private static boolean requirePlaceholderCabinet(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        if (isPlaceholderCabinet(level, pos)) {
            return true;
        }
        source.sendFailure(Component.literal("Expected a placeholder reagent cabinet at " + formatPos(pos)
                + " (barrel, chest, or trapped chest); found " + blockId(level, pos)));
        return false;
    }

    private static boolean isPlaceholderCabinet(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.BARREL)
                || level.getBlockState(pos).is(Blocks.CHEST)
                || level.getBlockState(pos).is(Blocks.TRAPPED_CHEST);
    }

    private static BlockPos position(CommandContext<CommandSourceStack> ctx) {
        return new BlockPos(
                IntegerArgumentType.getInteger(ctx, "x"),
                IntegerArgumentType.getInteger(ctx, "y"),
                IntegerArgumentType.getInteger(ctx, "z")
        );
    }

    private static String blockId(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private static String formatStack(ReagentStack stack) {
        return stack.amount()
                + " "
                + stack.reagentId()
                + " tier "
                + stack.tier()
                + " quality "
                + stack.quality()
                + " purity "
                + stack.purity()
                + " instability "
                + stack.instability();
    }

    private static String formatOutput(SynthesisOutput output) {
        String affixes = output.affixes().isEmpty() ? "" : " affixes " + String.join(",", output.affixes());
        return output.count()
                + " "
                + output.outputId()
                + " tier "
                + output.tier()
                + " quality "
                + output.quality()
                + affixes;
    }

    private static long defaultSeed(CommandSourceStack source) {
        long entityBits = source.getEntity() == null ? 0L : source.getEntity().getUUID().getLeastSignificantBits();
        return source.getLevel().getGameTime() ^ entityBits;
    }

    private static long parseSeed(CommandSourceStack source, String seed) {
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException ex) {
            source.sendFailure(Component.literal("Invalid seed '" + seed + "', using 0."));
            return 0L;
        }
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }
}
