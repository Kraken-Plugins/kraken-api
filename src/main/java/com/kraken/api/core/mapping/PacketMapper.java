package com.kraken.api.core.mapping;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PacketMapper {

    private final Map<String, ClassNode> classes = new HashMap<>();
    private String clientPacketClassName = null;
    private MethodNode doActionMethod = null;
    private ClassNode doActionClass = null;

    // Store our mapped packets: Opcode -> Obfuscated Field Name
    private final Map<Integer, String> packetMappings = new HashMap<>();

    // Target signature for doAction: (int, int, int, int, int, String, String, int, int, int)
    private static final String DO_ACTION_DESC = "(IIIIILjava/lang/String;Ljava/lang/String;III)V";

    public void run(Path jarPath) throws Exception {
        loadJar(jarPath);

        System.out.println("Phase 1 & 2: Fingerprinting classes...");
        fingerprintCoreStructures();

        if (doActionMethod == null || clientPacketClassName == null) {
            throw new IllegalStateException("Failed to find doAction or ClientPacket class!");
        }

        System.out.println("Found ClientPacket Class: " + clientPacketClassName);
        System.out.println("Found doAction Method in: " + doActionClass.name);

        System.out.println("\nPhase 3: Extracting Core Mappings from doAction...");
        extractMappings(doActionMethod);

        System.out.println("\nPhase 4: Hunting down Helper Methods (OPLOC, OPNPC)...");
        analyzeHelperMethods();

        System.out.println("\n--- Final Mappings ---");
        packetMappings.forEach((opcode, field) ->
            System.out.println("Action " + opcode + " -> " + clientPacketClassName + "." + field)
        );
    }

    private void loadJar(Path jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toString())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(is);
                        ClassNode cn = new ClassNode();
                        cr.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        classes.put(cn.name, cn);
                    }
                }
            }
        }
    }

    private void fingerprintCoreStructures() {
        for (ClassNode cn : classes.values()) {
            // Find ClientPacket class (Phase 2)
            // Heuristic: A class with an unusually high number of public static final fields of its own type.
            int selfTypeFields = 0;
            String targetDesc = "L" + cn.name + ";";
            for (FieldNode fn : cn.fields) {
                if ((fn.access & Opcodes.ACC_STATIC) != 0 && fn.desc.equals(targetDesc)) {
                    selfTypeFields++;
                }
            }
            // TODO iq implements an interface whereas jt doesn't. Add this restriction in to fingerprint more accurately
            if (selfTypeFields > 50) { // ClientPacket has 80+ packet definitions
                clientPacketClassName = cn.name;
                System.out.println("Found ClientPacket class: " + clientPacketClassName);
            }

            // Find doAction method (Phase 1)
            for (MethodNode mn : cn.methods) {
                // TODO Desc is wrong here...
                if ((mn.access & Opcodes.ACC_STATIC) != 0 && mn.desc.equals(DO_ACTION_DESC)) {
                    doActionMethod = mn;
                    doActionClass = cn;
                    System.out.println("Found doAction method: " + mn.name);
                }
            }
        }
    }

    private void extractMappings(MethodNode method) {
        InsnList instructions = method.instructions;

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);

            // Look for an integer push (the Action ID / opcode)
            int opcodeValue = getPushedInt(insn);
            if (opcodeValue != -1) {
                // Look ahead for the jump instruction (IF_ICMPEQ or IF_ICMPNE)
                AbstractInsnNode nextInsn = getNextRealInstruction(insn);
                if (nextInsn instanceof JumpInsnNode) {
                    // We found an if (action == X) block.
                    // Now scan forward in this local block for a GETSTATIC of our ClientPacket class.
                    String mappedField = scanForwardForPacketField(nextInsn, 30); // Scan next 30 instructions
                    if (mappedField != null) {
                        packetMappings.put(opcodeValue, mappedField);
                    }
                }
            }
        }
    }

    private void analyzeHelperMethods() {
        Set<String> processedMethods = new HashSet<>();
        InsnList instructions = doActionMethod.instructions;

        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);

            // Look for method delegations inside doAction
            if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode min = (MethodInsnNode) insn;
                String methodKey = min.owner + "." + min.name + min.desc;

                // Helper methods usually take a lot of parameters, similar to doAction.
                // We filter out simple utility calls by checking argument count.
                if (org.objectweb.asm.Type.getArgumentTypes(min.desc).length > 5 && !processedMethods.contains(methodKey)) {
                    processedMethods.add(methodKey);

                    ClassNode targetClass = classes.get(min.owner);
                    if (targetClass != null) {
                        for (MethodNode mn : targetClass.methods) {
                            if (mn.name.equals(min.name) && mn.desc.equals(min.desc)) {
                                System.out.println("Analyzing helper method: " + methodKey);
                                extractMappings(mn); // Run Phase 3 logic on the helper method!
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Utility Methods for ASM Instruction Parsing ---

    private int getPushedInt(AbstractInsnNode insn) {
        if (insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
            return ((IntInsnNode) insn).operand;
        } else if (insn.getOpcode() >= Opcodes.ICONST_0 && insn.getOpcode() <= Opcodes.ICONST_5) {
            return insn.getOpcode() - Opcodes.ICONST_0;
        } else if (insn.getOpcode() == Opcodes.LDC) {
            Object cst = ((LdcInsnNode) insn).cst;
            if (cst instanceof Integer) return (Integer) cst;
        }
        return -1;
    }

    private AbstractInsnNode getNextRealInstruction(AbstractInsnNode insn) {
        AbstractInsnNode next = insn.getNext();
        while (next != null && (next.getOpcode() == -1 || next instanceof LabelNode || next instanceof LineNumberNode)) {
            next = next.getNext();
        }
        return next;
    }

    private String scanForwardForPacketField(AbstractInsnNode start, int limit) {
        AbstractInsnNode current = start;
        int count = 0;
        while (current != null && count < limit) {
            if (current.getOpcode() == Opcodes.GETSTATIC) {
                FieldInsnNode fn = (FieldInsnNode) current;
                // Verify this GETSTATIC belongs to our ClientPacket class
                if (fn.owner.equals(clientPacketClassName)) {
                    return fn.name; // Return the obfuscated field name (e.g., "by")
                }
            }
            current = current.getNext();
            count++;
        }
        return null;
    }
}
