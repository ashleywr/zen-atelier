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

        state = state.enterBoard(3);
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
        SynthesisStationScreen.ModeState state = SynthesisStationScreen.ModeState.initial().enterBoard(1);

        assertThat(state.enterBoard(2).selectedProfileChangedFrom(state)).isTrue();
        assertThat(state.enterBoard(1).selectedProfileChangedFrom(state)).isFalse();
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
}
