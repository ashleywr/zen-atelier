package com.sanhiruzu.atelier.space;

/**
 * Labels why two room-air cells are connected.
 *
 * <p>The label is intentionally about traversal geometry, not room identity.
 * Partitioning can then decide whether a transition is a normal part of one
 * room or a threshold between rooms without re-running flood-fill rules.</p>
 */
public enum RoomTransitionKind {
    FLAT,
    VERTICAL_OPENING,
    STEP_UP,
    STEP_DOWN,
    STEP_LEVEL
}
