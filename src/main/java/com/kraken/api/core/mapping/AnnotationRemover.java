package com.kraken.api.core.mapping;

import lombok.NoArgsConstructor;
import net.bytebuddy.jar.asm.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class AnnotationRemover {

    public static void stripNamedAnnotations(Path input, Path output) throws IOException {
        if(Files.exists(output)) return;
        try (JarFile jar = new JarFile(input.toFile());
             JarOutputStream out = new JarOutputStream(new FileOutputStream(output.toFile()))) {

            jar.entries().asIterator().forEachRemaining(entry -> {
                try {
                    byte[] bytes = jar.getInputStream(entry).readAllBytes();
                    if (entry.getName().endsWith(".class")) {
                        bytes = stripNamedAnnotation(bytes);
                    }
                    out.putNextEntry(new JarEntry(entry.getName()));
                    out.write(bytes);
                    out.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static byte[] stripNamedAnnotation(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(0);

        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.contains("Named")) return null; // drop it
                return super.visitAnnotation(descriptor, visible);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9,
                        super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.contains("Named")) return null;
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor,
                                           String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9,
                        super.visitField(access, name, descriptor, signature, value)) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.contains("Named")) return null;
                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        }, 0);

        return cw.toByteArray();
    }
}