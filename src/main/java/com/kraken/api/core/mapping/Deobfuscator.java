package com.kraken.api.core.mapping;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Deobfuscator {

    private static final String DO_ACTION_DESC = "(IIIIIILjava/lang/String;Ljava/lang/String;III)V";

    public static void run(Path input, Path output) throws IOException {
        if (Files.exists(output)) {
            log.info("Deobfuscated client already exists at {}, skipping deob pass.", output);
            return;
        }

        log.info("Starting deobfuscation pass on {}...", input.getFileName());

        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(new FileOutputStream(output.toFile()))) {

            jar.entries().asIterator().forEachRemaining(entry -> {
                try {
                    byte[] bytes = jar.getInputStream(entry).readAllBytes();

                    if (entry.getName().endsWith(".class")) {
                        ClassReader cr = new ClassReader(bytes);
                        ClassNode cn = new ClassNode();
                        cr.accept(cn, 0);

                        // 1. Strip Annotations globally
                        stripNamedAnnotations(cn);

                        // 2. Auto-discover and clean doAction()
                        if ("java/util/AbstractQueue".equals(cn.superName)) {
                            for (MethodNode mn : cn.methods) {
                                if ((mn.access & Opcodes.ACC_STATIC) != 0 && mn.desc.equals(DO_ACTION_DESC)) {
                                    log.info("Discovered doAction method at {}.{}", cn.name, mn.name);
                                    removeOpaquePredicates(mn);
                                    normalizeControlFlow(mn);
                                    removeUnusedMultipliers(mn);
                                    log.info("Successfully cleaned doAction method.");
                                }
                            }
                        }

                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        cn.accept(cw);
                        bytes = cw.toByteArray();
                    }

                    out.putNextEntry(new JarEntry(entry.getName()));
                    out.write(bytes);
                    out.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        log.info("Deobfuscation complete. Wrote clean jar to {}", output.getFileName());
    }

    private static void stripNamedAnnotations(ClassNode cn) {
        if (cn.visibleAnnotations != null) cn.visibleAnnotations.removeIf(ann -> ann.desc.contains("Named"));
        for (MethodNode mn : cn.methods) {
            if (mn.visibleAnnotations != null) mn.visibleAnnotations.removeIf(ann -> ann.desc.contains("Named"));
        }
        for (FieldNode fn : cn.fields) {
            if (fn.visibleAnnotations != null) fn.visibleAnnotations.removeIf(ann -> ann.desc.contains("Named"));
        }
    }

    private static void removeOpaquePredicates(MethodNode mn) {
        InsnList instructions = mn.instructions;
        List<AbstractInsnNode> toRemove = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn.getOpcode() == Opcodes.NEW && ((TypeInsnNode) insn).desc.equals("java/lang/IllegalStateException")) {
                AbstractInsnNode jumpNode = insn.getPrevious();
                while (jumpNode != null && !(jumpNode instanceof JumpInsnNode)) jumpNode = jumpNode.getPrevious();

                if (jumpNode instanceof JumpInsnNode) {
                    AbstractInsnNode constNode = jumpNode.getPrevious();
                    AbstractInsnNode loadNode = constNode != null ? constNode.getPrevious() : null;

                    if (loadNode != null) toRemove.add(loadNode);
                    if (constNode != null) toRemove.add(constNode);
                    toRemove.add(jumpNode);

                    AbstractInsnNode current = insn;
                    while (current != null) {
                        toRemove.add(current);
                        if (current.getOpcode() == Opcodes.ATHROW) break;
                        current = current.getNext();
                    }
                }
            }
        }
        toRemove.forEach(instructions::remove);
    }

    private static void normalizeControlFlow(MethodNode mn) {
        InsnList instructions = mn.instructions;
        List<AbstractInsnNode> toRemove = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn.getOpcode() == Opcodes.GOTO) {
                JumpInsnNode jump = (JumpInsnNode) insn;
                AbstractInsnNode nextReal = getNextRealInstruction(insn);
                if (nextReal == jump.label) toRemove.add(jump);
            }
        }
        toRemove.forEach(instructions::remove);
    }

    private static void removeUnusedMultipliers(MethodNode mn) {
        InsnList instructions = mn.instructions;
        List<AbstractInsnNode> toRemove = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn.getOpcode() == Opcodes.IMUL) {
                AbstractInsnNode prev = insn.getPrevious();
                AbstractInsnNode next = getNextRealInstruction(insn);
                if (prev instanceof LdcInsnNode && (next.getOpcode() == Opcodes.PUTSTATIC || next.getOpcode() == Opcodes.PUTFIELD)) {
                    toRemove.add(prev);
                    toRemove.add(insn);
                }
            }
        }
        toRemove.forEach(instructions::remove);
    }

    private static AbstractInsnNode getNextRealInstruction(AbstractInsnNode insn) {
        AbstractInsnNode next = insn.getNext();
        while (next != null && (next.getOpcode() == -1 || next instanceof LabelNode || next instanceof LineNumberNode)) {
            next = next.getNext();
        }
        return next;
    }
}