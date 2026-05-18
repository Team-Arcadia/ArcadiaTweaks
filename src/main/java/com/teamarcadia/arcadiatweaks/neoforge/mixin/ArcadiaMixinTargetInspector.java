package com.teamarcadia.arcadiatweaks.neoforge.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;

public final class ArcadiaMixinTargetInspector {

    private ArcadiaMixinTargetInspector() {}

    public static boolean hasClass(String className) {
        return readClassNode(className) != null;
    }

    public static boolean hasField(String className, String fieldName, String descriptor) {
        final ClassNode node = readClassNode(className);
        if (node == null) {
            return false;
        }
        for (FieldNode field : node.fields) {
            if (field.name.equals(fieldName) && field.desc.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMethod(String className, String methodName, String descriptor) {
        final ClassNode node = readClassNode(className);
        return node != null && hasMethod(node, methodName, descriptor);
    }

    public static boolean hasMethodInHierarchy(String className, String methodName, String descriptor) {
        ClassNode node = readClassNode(className);
        while (node != null) {
            if (hasMethod(node, methodName, descriptor)) {
                return true;
            }
            node = node.superName == null ? null : readClassNode(node.superName.replace('/', '.'));
        }
        return false;
    }

    public static boolean hasMethodCall(
            String className,
            String methodName,
            String methodDescriptor,
            String targetOwner,
            String targetName,
            String targetDescriptor
    ) {
        final ClassNode node = readClassNodeWithCode(className);
        if (node == null) {
            return false;
        }
        final String targetOwnerInternal = targetOwner.replace('.', '/');
        for (MethodNode method : node.methods) {
            if (!method.name.equals(methodName) || !method.desc.equals(methodDescriptor)) {
                continue;
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode methodCall
                        && methodCall.owner.equals(targetOwnerInternal)
                        && methodCall.name.equals(targetName)
                        && methodCall.desc.equals(targetDescriptor)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean hasMethod(ClassNode node, String methodName, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(methodName) && method.desc.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    private static ClassNode readClassNode(String className) {
        return readClassNode(className, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static ClassNode readClassNodeWithCode(String className) {
        return readClassNode(className, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static ClassNode readClassNode(String className, int parsingOptions) {
        final String resourceName = className.replace('.', '/') + ".class";
        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassNode node = readClassNode(contextLoader, resourceName, parsingOptions);
        if (node != null) {
            return node;
        }
        return readClassNode(ArcadiaMixinTargetInspector.class.getClassLoader(), resourceName, parsingOptions);
    }

    private static ClassNode readClassNode(ClassLoader loader, String resourceName, int parsingOptions) {
        if (loader == null) {
            return null;
        }
        try (InputStream input = loader.getResourceAsStream(resourceName)) {
            if (input == null) {
                return null;
            }
            final ClassReader reader = new ClassReader(input);
            final ClassNode node = new ClassNode();
            reader.accept(node, parsingOptions);
            return node;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
