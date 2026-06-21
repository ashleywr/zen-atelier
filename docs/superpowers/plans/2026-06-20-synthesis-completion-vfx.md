# Synthesis Completion VFX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make successful synthesis feel rewarding with a resonant completion particle effect at the synthesis station.

**Architecture:** Add a pure `SynthesisCompletionVfxPlan` helper that computes particle counts, radii, heights, and scaling from outcome success. Wire `AlchemyVfx.synthesisCompleted` to emit the richer success plan while preserving existing failure smoke/reject behavior.

**Tech Stack:** Java 21, NeoForge server particles, existing `AlchemyVfx` helpers, JUnit 5, AssertJ.

---

### Task 1: Pure Success VFX Plan

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/synthesis/vfx/SynthesisCompletionVfxPlan.java`
- Create: `src/test/java/com/sanhiruzu/atelier/synthesis/vfx/SynthesisCompletionVfxPlanTest.java`

- [x] **Step 1: Write failing tests**

Assert successful synthesis has pulse/ring/mote/lift/afterglow layers, perfect success is richer than normal success, and failure plans stay compact.

- [x] **Step 2: Run focused test**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.synthesis.vfx.SynthesisCompletionVfxPlanTest`

- [x] **Step 3: Implement the plan record**

Expose immutable fields for core pulse, inner/outer rings, motes, lift sparks, afterglow, radius, and lift height.

- [x] **Step 4: Re-run focused test**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.synthesis.vfx.SynthesisCompletionVfxPlanTest`

### Task 2: Emit Resonant Completion

**Files:**
- Modify: `src/main/java/com/sanhiruzu/atelier/synthesis/vfx/AlchemyVfx.java`

- [x] **Step 1: Use the plan from `synthesisCompleted`**

Replace the plain success burst with layered ring, lift, glow, and mote emissions. Keep the existing failure smoke/reject language but drive counts from the compact plan.

- [x] **Step 2: Compile**

Run: `.\gradlew.bat compileJava`

### Task 3: Verification

**Files:**
- Verify only.

- [x] **Step 1: Run focused tests**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.synthesis.vfx.SynthesisCompletionVfxPlanTest`

- [x] **Step 2: Run compile**

Run: `.\gradlew.bat compileJava`

- [x] **Step 3: Run GameTest if feasible**

Run: `.\gradlew.bat runGameTestServer`

The synthesis consumption GameTests were fixed during follow-up work; `.\gradlew.bat testGameTest` passed all required tests.
