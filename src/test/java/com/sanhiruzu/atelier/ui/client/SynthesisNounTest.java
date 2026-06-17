package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.RequirementStatus;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoard;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.engine.OutcomePreview;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesisNounTest {
    @Test
    void findsNounsFromNamespacedIdsAndPlainAliases() {
        assertThat(SynthesisNoun.find("water")).contains(SynthesisNoun.WATER);
        assertThat(SynthesisNoun.find("zen_atelier:binding")).contains(SynthesisNoun.BINDING);
        assertThat(SynthesisNoun.find("zen_atelier:spark_reagent")).contains(SynthesisNoun.SPARK);
        assertThat(SynthesisNoun.find("zen_atelier:sparking")).contains(SynthesisNoun.SPARK);
    }

    @Test
    void importantPrototypeNounsHaveStableEmotiveColors() {
        assertThat(SynthesisNoun.WATER.color()).isEqualTo(0xFF76B7E8);
        assertThat(SynthesisNoun.BINDING.color()).isEqualTo(0xFFB06CD7);
        assertThat(SynthesisNoun.SPARK.color()).isEqualTo(0xFFFFC857);
    }

    @Test
    void synthesisNeedsPreferRecipeFamiliesOverElementQualifiers() {
        ReagentQuery query = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        RequirementStatus status = new RequirementStatus(new SynthesisRequirement(query, 20), 12, 8, false);

        assertThat(SynthesisStationText.summarizeQuery(status)).isEqualTo("Binding");
        assertThat(SynthesisStationText.requirementLine(status)).isEqualTo("12/20 Binding");
        assertThat(SynthesisStationText.elementBudget(query)).isEqualTo("Water 1");
    }

    @Test
    void synthesisElementBudgetCombinesRecipeElementRequirements() {
        ReagentQuery wateryOrganic = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:organic"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery wateryBinding = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery earthy = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:earthy"))
                .minElements(Map.of("earth", 1))
                .build();

        assertThat(SynthesisStationText.compactElementBudget(java.util.List.of(wateryOrganic, wateryBinding, earthy)))
                .isEqualTo("Earth 1, Water 2");
    }

    @Test
    void synthesisStateDebugJsonSeparatesRequirementsFromElements() {
        ReagentQuery wateryOrganic = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:organic"))
                .minElements(Map.of("water", 1))
                .build();
        ReagentQuery wateryBinding = ReagentQuery.builder()
                .requiredCategories(Set.of("zen_atelier:binding"))
                .minElements(Map.of("water", 1))
                .build();
        SynthesisRequirement organicRequirement = new SynthesisRequirement(wateryOrganic, 35);
        SynthesisRequirement bindingRequirement = new SynthesisRequirement(wateryBinding, 20);
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                "bombs",
                List.of(organicRequirement, bindingRequirement),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:frost_globe", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        SynthesisPlan plan = new SynthesisPlan(
                profile,
                List.of(
                        new RequirementStatus(organicRequirement, 35, 0, true),
                        new RequirementStatus(bindingRequirement, 5, 15, false)
                ),
                OutcomePreview.forSynthesis(profile.outcomes(), 0),
                false
        );

        String json = SynthesisState.minimal(profile, plan).toDebugJson();

        assertThat(json).contains("\"label\": \"Organic\"");
        assertThat(json).contains("\"label\": \"Binding\"");
        assertThat(json).contains("\"budget\": {\n      \"water\": 2\n    }");
        assertThat(json).contains("\"display\":");
        assertThat(json).contains("\"essences\":");
        assertThat(json).contains("\"elements\":");
        assertThat(json).contains("\"label\": \"Water\"");
    }

    @Test
    void emptyStoragePaletteDoesNotExposeGeneratedDebugStock() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:test",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 1)),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, new ReagentContainer(), 0);

        SynthesisState state = new SynthesisSpatialPrototype().buildState(
                java.util.Optional.of(plan),
                List.of(),
                List.of()
        );

        assertThat(state.palette().entries()).isEmpty();
    }

    @Test
    void repeatedPlacementOfOneAvailableReagentDoesNotInflateDebugAvailability() {
        ReagentStack reagent = new ReagentStack(
                "zen_atelier:small_reagent",
                10,
                1,
                30,
                50,
                0,
                Map.of("water", 1),
                List.of(),
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:needs_more_than_one_stack",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 15)),
                2,
                List.of(new SynthesisOutcome(
                        OutcomeClass.SUCCESS,
                        1,
                        List.of(new SynthesisOutput("zen_atelier:test_output", 1, 1, 1, List.of())),
                        List.of()
                ))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(reagent);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();

        spatial.mouseClicked(14, 246, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(183, 85, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(14, 246, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(201, 85, 0, false, java.util.Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));

        SynthesisState state = spatial.buildState(java.util.Optional.of(plan), List.of(reagent), List.of());

        assertThat(state.canSynthesize()).isFalse();
        assertThat(state.requirements()).first()
                .extracting(SynthesisState.RequirementLine::available)
                .isEqualTo(10);
    }

    @Test
    void synthesisReadoutPrioritizesRequirementsBeforeOutcomeOdds() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:readout_order",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 10)),
                2,
                List.of(
                        new SynthesisOutcome(OutcomeClass.SUCCESS, 96, List.of(), List.of()),
                        new SynthesisOutcome(OutcomeClass.RECOVERABLE_FAILURE, 4, List.of(), List.of())
                )
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(ReagentStack.simple("zen_atelier:test_reagent", 10, 1));
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);

        SynthesisSpatialPrototype.ReadoutDebugLayout layout = SynthesisSpatialPrototype.debugReadoutLayout(
                new ScreenRect(278, 58, 180, 168),
                plan,
                SynthesisBoard.CRUDE_3X3
        );

        assertThat(layout.firstRequirementY()).isLessThan(layout.outcomeSummaryY());
    }

    @Test
    void synthesisScreenModeStartsInRecipeBookAndCanEnterAndLeaveBoard() {
        SynthesisStationScreen.ModeState state = SynthesisStationScreen.ModeState.initial();

        assertThat(state.mode()).isEqualTo(SynthesisStationScreen.ScreenMode.RECIPE_BOOK);

        state = state.selectProfile(3);
        assertThat(state.mode()).isEqualTo(SynthesisStationScreen.ScreenMode.RECIPE_BOOK);
        assertThat(state.selectedProfileIndex()).isEqualTo(3);

        state = state.enterBoard();
        assertThat(state.mode()).isEqualTo(SynthesisStationScreen.ScreenMode.BOARD);
        assertThat(state.selectedProfileIndex()).isEqualTo(3);

        state = state.backToRecipeBook();
        assertThat(state.mode()).isEqualTo(SynthesisStationScreen.ScreenMode.RECIPE_BOOK);
        assertThat(state.selectedProfileIndex()).isEqualTo(3);
    }

    @Test
    void boardModeGivesBoardMoreSpaceThanCurrentEmbeddedReadout() {
        SynthesisStationLayout layout = new SynthesisStationLayout();

        assertThat(layout.boardModeBoardArea().width()).isGreaterThan(120);
        assertThat(layout.boardModeBoardArea().height()).isGreaterThan(120);
        assertThat(layout.boardModePalettePanel().x()).isLessThan(layout.boardModeBoardArea().x());
        assertThat(layout.boardModeProgressPanel().x()).isGreaterThan(layout.boardModeBoardArea().x());
    }

    @Test
    void boardModeDebugLayoutUsesProvidedBoardArea() {
        ScreenRect boardArea = new ScreenRect(142, 45, 190, 190);

        SynthesisSpatialPrototype.BoardModeDebugLayout layout =
                SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3);

        assertThat(layout.boardRect().width()).isGreaterThan(100);
        assertThat(layout.boardRect().x()).isBetween(boardArea.x(), boardArea.right());
    }

    @Test
    void boardModeDebugLayoutLeavesPaddingAroundBoard() {
        ScreenRect boardArea = new ScreenRect(146, 48, 166, 166);

        SynthesisSpatialPrototype.BoardModeDebugLayout layout =
                SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3);

        assertThat(layout.boardRect().width()).isLessThan(boardArea.width());
        assertThat(layout.boardRect().height()).isLessThan(boardArea.height());
    }

    @Test
    void boardModeMapsClicksInsideLargeBoardToBoardCells() {
        ScreenRect boardArea = new ScreenRect(142, 45, 190, 190);

        Optional<SynthesisSpatialPrototype.DebugCell> cell = SynthesisSpatialPrototype.debugBoardModeCellAt(
                boardArea,
                SynthesisBoard.CRUDE_3X3,
                boardArea.x() + boardArea.width() / 2,
                boardArea.y() + boardArea.height() / 2
        );

        assertThat(cell).contains(new SynthesisSpatialPrototype.DebugCell(1, 1));
    }

    @Test
    void enteringDifferentRecipeRequiresBoardReset() {
        SynthesisStationScreen.ModeState state = SynthesisStationScreen.ModeState.initial().selectProfile(1).enterBoard();

        assertThat(state.selectProfile(2).enterBoard().selectedProfileChangedFrom(state)).isTrue();
        assertThat(state.selectProfile(1).enterBoard().selectedProfileChangedFrom(state)).isFalse();
    }

    @Test
    void recipeDoubleClickStartsOnlySameRecipeWithinThreshold() {
        assertThat(SynthesisStationScreen.isRecipeDoubleClick(2, 2, 1_200L, 1_000L)).isTrue();
        assertThat(SynthesisStationScreen.isRecipeDoubleClick(3, 2, 1_200L, 1_000L)).isFalse();
        assertThat(SynthesisStationScreen.isRecipeDoubleClick(2, 2, 1_401L, 1_000L)).isFalse();
    }

    @Test
    void boardPaletteRanksMissingRequirementBeforeUnrelatedReagent() {
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:palette_rank",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:binding"))
                        .build(), 10)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentStack unrelated = new ReagentStack("zen_atelier:stone", Set.of("zen_atelier:filler"), 10, 1, 10, 10, 0, Map.of(), List.of(), com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE, Set.of());
        ReagentStack useful = new ReagentStack("zen_atelier:aqua_gel", Set.of("zen_atelier:binding"), 10, 1, 10, 10, 0, Map.of(), List.of(), com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE, Set.of());
        ReagentContainer available = new ReagentContainer();
        available.insert(unrelated);
        available.insert(useful);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);

        List<String> ranked = new SynthesisSpatialPrototype().debugPaletteOrder(Optional.of(plan), List.of(unrelated, useful), List.of());

        assertThat(ranked).containsExactly("zen_atelier:aqua_gel", "zen_atelier:stone");
    }

    @Test
    void boardModePlacementUpdatesRequirementAvailability() {
        ReagentStack organic = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                31,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .build(), 35)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(organic);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();
        SynthesisStationLayout layout = new SynthesisStationLayout();
        ScreenRect palettePanel = layout.boardModePalettePanel();
        ScreenRect boardArea = layout.boardModeBoardArea();
        ScreenRect boardRect = SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3).boardRect();
        ScreenRect paletteTile = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst();

        spatial.mouseClickedBoardMode(
                paletteTile.x() + 4,
                paletteTile.y() + 4,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                new ScreenRect(0, 0, 0, 0),
                boardArea,
                palettePanel
        );
        spatial.mouseClickedBoardMode(
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                new ScreenRect(0, 0, 0, 0),
                boardArea,
                palettePanel
        );

        SynthesisState state = spatial.buildState(Optional.of(plan), List.of(organic), List.of());

        assertThat(state.requirements()).singleElement()
                .extracting(SynthesisState.RequirementLine::available)
                .isEqualTo(31);
    }

    @Test
    void boardModeTooltipExplainsSynthesisContributionForPlacedReagent() {
        ReagentStack organic = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                31,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .build(), 35)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(organic);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();
        SynthesisStationLayout layout = new SynthesisStationLayout();
        ScreenRect palettePanel = layout.boardModePalettePanel();
        ScreenRect boardArea = layout.boardModeBoardArea();
        ScreenRect boardRect = SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3).boardRect();
        ScreenRect origin = new ScreenRect(0, 0, 0, 0);
        ScreenRect paletteTile = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst();

        spatial.mouseClickedBoardMode(
                paletteTile.x() + 4,
                paletteTile.y() + 4,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );
        spatial.mouseClickedBoardMode(
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        List<String> lines = spatial.debugBoardModeTooltipLines(
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel,
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2
        );

        assertThat(lines).contains("Organic Reagent");
        assertThat(lines).anyMatch(line -> line.contains("Contributes"));
        assertThat(lines).anyMatch(line -> line.contains("Contributes 31 Organic essence"));
    }

    @Test
    void boardModeTooltipNamesReagentAmountAsEssencePotencyNotInventoryQuantity() {
        ReagentStack organic = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                22,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .build(), 35)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(organic);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();
        SynthesisStationLayout layout = new SynthesisStationLayout();
        ScreenRect palettePanel = layout.boardModePalettePanel();
        ScreenRect boardArea = layout.boardModeBoardArea();
        ScreenRect origin = new ScreenRect(0, 0, 0, 0);
        ScreenRect paletteTile = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst();

        List<String> lines = spatial.debugBoardModeTooltipLines(
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel,
                paletteTile.x() + 4,
                paletteTile.y() + 4
        );

        assertThat(lines).contains("Potency: 22 essence");
        assertThat(lines).contains("Contributes 22 Organic essence");
        assertThat(lines).noneMatch(line -> line.equals("Available: 22"));
        assertThat(lines).noneMatch(line -> line.contains("Contributes: Organic 22"));
    }

    @Test
    void boardModePaletteUsesCompactSquareTilesAcrossColumns() {
        ScreenRect palettePanel = new SynthesisStationLayout().boardModePalettePanel();

        SynthesisSpatialPrototype.BoardModePaletteDebugLayout layout =
                SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 6);

        assertThat(layout.tiles()).hasSize(6);
        assertThat(layout.tiles().get(0).width()).isEqualTo(layout.tiles().get(0).height());
        assertThat(layout.tiles().get(1).y()).isEqualTo(layout.tiles().get(0).y());
        assertThat(layout.tiles().get(1).x()).isGreaterThan(layout.tiles().get(0).x());
    }

    @Test
    void boardModeLayoutUsesLowerScreenSpaceForMaterialsAndBoard() {
        SynthesisStationLayout layout = new SynthesisStationLayout();

        assertThat(layout.boardModePalettePanel().bottom()).isGreaterThan(280);
        assertThat(layout.boardModeBoardArea().height()).isGreaterThan(220);
        assertThat(layout.boardModeProgressPanel().bottom()).isGreaterThan(280);
    }

    @Test
    void boardModeProgressLayoutSeparatesRequiredNeedsFromAddedEffects() {
        SynthesisDisplayModel model = new SynthesisDisplayModel(
                List.of(new SynthesisDisplayModel.Line("Medicinal", 0, 30, false, true)),
                List.of(new SynthesisDisplayModel.Line("Water", 0, 1, false, true)),
                List.of(new SynthesisDisplayModel.TextLine("None active", false)),
                List.of(new SynthesisDisplayModel.TextLine("None", false))
        );

        SynthesisSpatialPrototype.BoardModeProgressDebugLayout layout =
                SynthesisSpatialPrototype.debugBoardModeProgressLayout(
                        new SynthesisStationLayout().boardModeProgressPanel(),
                        model
                );

        assertThat(layout.essencesHeaderY()).isLessThan(layout.firstEssenceLineY());
        assertThat(layout.firstEssenceLineY()).isLessThan(layout.elementsHeaderY());
        assertThat(layout.elementsHeaderY()).isLessThan(layout.firstElementLineY());
        assertThat(layout.firstElementLineY()).isLessThan(layout.dividerY());
        assertThat(layout.dividerY()).isLessThan(layout.traitsHeaderY());
        assertThat(layout.traitsHeaderY()).isLessThan(layout.resonanceHeaderY());
    }

    @Test
    void boardPaletteStateReportsPlacedAndRemainingReagents() {
        ReagentStack organic = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                31,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .build(), 35)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(organic.withAmount(62));
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();
        SynthesisStationLayout layout = new SynthesisStationLayout();
        ScreenRect palettePanel = layout.boardModePalettePanel();
        ScreenRect boardArea = layout.boardModeBoardArea();
        ScreenRect boardRect = SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3).boardRect();
        ScreenRect paletteTile = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst();
        ScreenRect origin = new ScreenRect(0, 0, 0, 0);

        spatial.mouseClickedBoardMode(
                paletteTile.x() + 4,
                paletteTile.y() + 4,
                0,
                false,
                Optional.of(plan),
                List.of(organic.withAmount(62)),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );
        spatial.mouseClickedBoardMode(
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2,
                0,
                false,
                Optional.of(plan),
                List.of(organic.withAmount(62)),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        SynthesisState.PaletteEntryState entry = spatial.buildState(Optional.of(plan), List.of(organic.withAmount(62)), List.of())
                .palette()
                .entries()
                .getFirst();

        assertThat(entry.amount()).isEqualTo(62);
        assertThat(entry.placedAmount()).isEqualTo(62);
        assertThat(entry.remainingAmount()).isZero();
        assertThat(entry.placedCopies()).isEqualTo(1);

        spatial.mouseClickedBoardMode(
                paletteTile.x() + 4,
                paletteTile.y() + 4,
                0,
                false,
                Optional.of(plan),
                List.of(organic.withAmount(62)),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );
        spatial.mouseClickedBoardMode(
                boardRect.x() + 6,
                boardRect.y() + 6,
                0,
                false,
                Optional.of(plan),
                List.of(organic.withAmount(62)),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        assertThat(spatial.buildState(Optional.of(plan), List.of(organic.withAmount(62)), List.of())
                .board()
                .placedReagents())
                .hasSize(1);

        spatial.mouseClickedBoardMode(
                boardRect.x() + boardRect.width() / 2,
                boardRect.y() + boardRect.height() / 2,
                0,
                true,
                Optional.of(plan),
                List.of(organic.withAmount(62)),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        SynthesisState.PaletteEntryState returned = spatial.buildState(Optional.of(plan), List.of(organic.withAmount(62)), List.of())
                .palette()
                .entries()
                .getFirst();

        assertThat(returned.placedAmount()).isZero();
        assertThat(returned.remainingAmount()).isEqualTo(62);
    }

    @Test
    void shiftClickOnPlacedBoardReagentRemovesItWithoutCarrying() {
        ReagentStack organic = new ReagentStack(
                "zen_atelier:organic_reagent",
                Set.of("zen_atelier:organic"),
                31,
                1,
                20,
                20,
                0,
                Map.of("life", 1),
                List.of("zen_atelier:organic"),
                com.sanhiruzu.atelier.synthesis.core.ReagentShape.SINGLE,
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:frost_globe",
                List.of(new SynthesisRequirement(ReagentQuery.builder()
                        .requiredCategories(Set.of("zen_atelier:organic"))
                        .build(), 35)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(organic);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();
        SynthesisStationLayout layout = new SynthesisStationLayout();
        ScreenRect palettePanel = layout.boardModePalettePanel();
        ScreenRect boardArea = layout.boardModeBoardArea();
        ScreenRect boardRect = SynthesisSpatialPrototype.debugBoardModeLayout(boardArea, SynthesisBoard.CRUDE_3X3).boardRect();
        ScreenRect origin = new ScreenRect(0, 0, 0, 0);
        int paletteX = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst().x() + 4;
        int paletteY = SynthesisSpatialPrototype.debugBoardModePaletteLayout(palettePanel, 1).tiles().getFirst().y() + 4;
        int boardX = boardRect.x() + boardRect.width() / 2;
        int boardY = boardRect.y() + boardRect.height() / 2;

        spatial.mouseClickedBoardMode(
                paletteX,
                paletteY,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );
        spatial.mouseClickedBoardMode(
                boardX,
                boardY,
                0,
                false,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        assertThat(spatial.buildState(Optional.of(plan), List.of(organic), List.of()).requirements())
                .singleElement()
                .extracting(SynthesisState.RequirementLine::available)
                .isEqualTo(31);

        spatial.mouseClickedBoardMode(
                boardX,
                boardY,
                0,
                true,
                Optional.of(plan),
                List.of(organic),
                List.of(),
                origin,
                boardArea,
                palettePanel
        );

        assertThat(spatial.buildState(Optional.of(plan), List.of(organic), List.of()).requirements())
                .singleElement()
                .extracting(SynthesisState.RequirementLine::available)
                .isEqualTo(0);
    }

    @Test
    void boardModeOnlyAllowsVisibleContainerControlsToReachSuperClickHandling() {
        ScreenRect craft = new ScreenRect(392, 274, 64, 22);
        ScreenRect catalyst = new ScreenRect(372, 190, 18, 18);

        assertThat(SynthesisStationScreen.allowsBoardModeSuperClick(craft, catalyst, 400, 280)).isTrue();
        assertThat(SynthesisStationScreen.allowsBoardModeSuperClick(craft, catalyst, 374, 194)).isTrue();
        assertThat(SynthesisStationScreen.allowsBoardModeSuperClick(craft, catalyst, 178, 238)).isFalse();
        assertThat(SynthesisStationScreen.allowsBoardModeSuperClick(craft, catalyst, 16, 256)).isFalse();
    }

    @Test
    void craftBlockerTooltipUsesVisibleAlchemySections() {
        SynthesisDisplayModel model = new SynthesisDisplayModel(
                List.of(
                        new SynthesisDisplayModel.Line("Medicinal", 0, 30, false, true),
                        new SynthesisDisplayModel.Line("Binding", 0, 15, false, true)
                ),
                List.of(new SynthesisDisplayModel.Line("Water", 0, 1, false, true)),
                List.of(new SynthesisDisplayModel.TextLine("None active", false)),
                List.of(new SynthesisDisplayModel.TextLine("None", false))
        );

        List<String> lines = SynthesisStationScreen.craftBlockerText(model).stream()
                .map(net.minecraft.network.chat.Component::getString)
                .toList();

        assertThat(lines).contains(
                "Cannot craft:",
                "Essences",
                "0/30 Medicinal",
                "0/15 Binding",
                "Elements",
                "0/1 Water"
        );
        assertThat(lines).noneMatch(line -> line.contains("Elements Water"));
    }

    @Test
    void boardFusionPayloadUsesSameBoundedPlacedReagentsAsCraftCheck() {
        ReagentStack reagent = new ReagentStack(
                "zen_atelier:small_reagent",
                10,
                1,
                30,
                50,
                0,
                Map.of("water", 1),
                List.of(),
                Set.of()
        );
        SynthesisProfile profile = new SynthesisProfile(
                "zen_atelier:payload_consistency",
                List.of(new SynthesisRequirement(ReagentQuery.any(), 10)),
                2,
                List.of(new SynthesisOutcome(OutcomeClass.SUCCESS, 1, List.of(), List.of()))
        );
        ReagentContainer available = new ReagentContainer();
        available.insert(reagent);
        SynthesisPlan plan = new SynthesisPlanner().plan(profile, available, 0);
        SynthesisSpatialPrototype spatial = new SynthesisSpatialPrototype();

        spatial.mouseClicked(14, 246, 0, false, Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(183, 85, 0, false, Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(14, 246, 0, false, Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));
        spatial.mouseClicked(201, 85, 0, false, Optional.of(plan), List.of(reagent), List.of(), new ScreenRect(0, 0, 0, 0));

        assertThat(spatial.canSynthesizePlaced(Optional.of(plan), List.of(reagent), List.of())).isTrue();

        SynthesisBoardFusionPayload payload = spatial.buildFusionPayload(7, List.of(reagent), List.of());

        assertThat(payload.decodePlacedReagents()).singleElement()
                .extracting(ReagentStack::amount)
                .isEqualTo(10);
    }
}
