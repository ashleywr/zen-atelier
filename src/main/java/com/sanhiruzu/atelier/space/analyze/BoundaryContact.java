package com.sanhiruzu.atelier.space.analyze;

public record BoundaryContact(Face face, int axisCoord, int y, boolean isPortal, long microRegionKey) {
    public enum Face { NORTH, SOUTH, WEST, EAST }
}
