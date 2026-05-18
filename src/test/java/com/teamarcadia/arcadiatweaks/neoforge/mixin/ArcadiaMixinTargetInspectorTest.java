package com.teamarcadia.arcadiatweaks.neoforge.mixin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArcadiaMixinTargetInspectorTest {

    @Test
    void detectsClassesFieldsAndMethods() {
        final String className = MethodCallFixture.class.getName();

        assertTrue(ArcadiaMixinTargetInspector.hasClass(className));
        assertTrue(ArcadiaMixinTargetInspector.hasField(className, "marker", "Ljava/lang/String;"));
        assertTrue(ArcadiaMixinTargetInspector.hasMethod(className, "caller", "()V"));
        assertFalse(ArcadiaMixinTargetInspector.hasMethod(className, "missing", "()V"));
    }

    @Test
    void walksSuperclassMethods() {
        assertTrue(ArcadiaMixinTargetInspector.hasMethodInHierarchy(ChildFixture.class.getName(), "inherited", "()Z"));
        assertFalse(ArcadiaMixinTargetInspector.hasMethodInHierarchy(ChildFixture.class.getName(), "missing", "()Z"));
    }

    @Test
    void detectsMethodCallsInsideMethodBody() {
        final String className = MethodCallFixture.class.getName();

        assertTrue(ArcadiaMixinTargetInspector.hasMethodCall(
                className,
                "caller",
                "()V",
                className,
                "callee",
                "()V"
        ));
        assertFalse(ArcadiaMixinTargetInspector.hasMethodCall(
                className,
                "caller",
                "()V",
                className,
                "missing",
                "()V"
        ));
    }

    private static class ParentFixture {
        public boolean inherited() {
            return true;
        }
    }

    private static final class ChildFixture extends ParentFixture {
    }

    private static final class MethodCallFixture {
        private String marker;

        public void caller() {
            callee();
        }

        private void callee() {
            marker = "called";
        }
    }
}
