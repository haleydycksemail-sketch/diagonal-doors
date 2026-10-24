package com.haley.diagonaldoors;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorGeometryTest {
    @Test void everyLeafKeepsItsHingeAndSwingsNinetyDegrees() {
        for (int facing = 0; facing < 4; facing++) for (boolean right : new boolean[]{false, true}) {
            var closed = DoorGeometry.panel(facing, right, false);
            var open = DoorGeometry.panel(facing, right, true);
            assertEquals(closed.hinge(), open.hinge());
            double cx = closed.tip().x()-closed.hinge().x(), cz = closed.tip().z()-closed.hinge().z();
            double ox = open.tip().x()-open.hinge().x(), oz = open.tip().z()-open.hinge().z();
            assertEquals(0, cx*ox+cz*oz, 1e-9);
            assertEquals(Math.hypot(cx, cz), Math.hypot(ox, oz), 1e-9);
            assertNotEquals(closed.tip(), open.tip());
        }
    }
    @Test void collisionCoversBothFacesContinuouslyInEveryState() {
        for (int facing = 0; facing < 4; facing++) for (boolean right : new boolean[]{false,true})
            for (boolean opened : new boolean[]{false,true}) {
                var panel = DoorGeometry.panel(facing, right, opened);
                var boxes = panel.boxes();
                double dx = panel.tip().x()-panel.hinge().x(), dz = panel.tip().z()-panel.hinge().z();
                double length = Math.hypot(dx,dz);
                for (int i = 0; i <= 256; i++) for (double face : new double[]{-1.5,0,1.5}) {
                    double x = panel.hinge().x()+dx*i/256-dz/length*face;
                    double z = panel.hinge().z()+dz*i/256+dx/length*face;
                    assertTrue(boxes.stream().anyMatch(b -> x >= b.minX()-1e-9 && x <= b.maxX()+1e-9
                            && z >= b.minZ()-1e-9 && z <= b.maxZ()+1e-9));
                }
                for (var box : boxes) {
                    assertTrue(box.minX() >= -16 && box.maxX() <= 32);
                    assertTrue(box.minZ() >= -16 && box.maxZ() <= 32);
                }
            }
    }
    @Test void pairedLeavesMeetExactlyAtTheirFreeEdges() {
        int dx=16,dz=-16;
        for (int facing=0;facing<4;facing++) {
            var first=DoorGeometry.panel(facing,false,false);
            var second=DoorGeometry.panel(facing,true,false);
            assertEquals(first.tip().x(),second.tip().x()+dx,1e-9);
            assertEquals(first.tip().z(),second.tip().z()+dz,1e-9);
            assertNotEquals(first.hinge(),first.tip());
            int oldX=dx;dx=-dz;dz=oldX;
        }
    }
}
