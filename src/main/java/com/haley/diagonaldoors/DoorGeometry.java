package com.haley.diagonaldoors;

import java.util.ArrayList;
import java.util.List;

/** Geometry in model units (16 units per block), shared by outlines and collision. */
public final class DoorGeometry {
    public static final double THICKNESS = 3.0;
    public static final double INSET = THICKNESS / (2 * Math.sqrt(2));
    public record Point(double x, double z) {
        Point rotate(int turns) {
            Point p = this;
            for (int i = 0; i < Math.floorMod(turns, 4); i++) p = new Point(16 - p.z, p.x);
            return p;
        }
    }
    public record Box(double minX, double minZ, double maxX, double maxZ) {}
    public record Panel(Point hinge, Point tip) {
        public List<Box> boxes() {
            var boxes = new ArrayList<Box>();
            double dx = tip.x - hinge.x, dz = tip.z - hinge.z;
            double length = Math.hypot(dx, dz);
            double nx = -dz / length * THICKNESS / 2, nz = dx / length * THICKNESS / 2;
            // Conservative union of short rectangular segments: continuous, with <0.22 model-unit error.
            for (int i = 0; i < 64; i++) {
                double x0 = hinge.x + dx * i / 64, z0 = hinge.z + dz * i / 64;
                double x1 = hinge.x + dx * (i + 1) / 64, z1 = hinge.z + dz * (i + 1) / 64;
                boxes.add(new Box(Math.min(x0, x1) - Math.abs(nx), Math.min(z0, z1) - Math.abs(nz),
                        Math.max(x0, x1) + Math.abs(nx), Math.max(z0, z1) + Math.abs(nz)));
            }
            return boxes;
        }
    }
    private DoorGeometry() {}
    public static Panel panel(int clockwiseTurns, boolean right, boolean open) {
        Point left = new Point(INSET, 16 - INSET), end = new Point(16 - INSET, INSET);
        Point hinge = right ? end : left, tip = right ? left : end;
        if (open) {
            double dx = tip.x - hinge.x, dz = tip.z - hinge.z;
            tip = right ? new Point(hinge.x + dz, hinge.z - dx) : new Point(hinge.x - dz, hinge.z + dx);
        }
        return new Panel(hinge.rotate(clockwiseTurns), tip.rotate(clockwiseTurns));
    }
}
