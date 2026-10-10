package com.kraken.llms;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders the public members Lombok generates from {@code @Getter}, {@code @Setter}, {@code @Data},
 * {@code @Value}, {@code @Builder} and the {@code *ArgsConstructor} annotations. Javadoc reads the
 * un-processed source, so without this the dump would be missing accessors plugin code calls every day.
 */
final class LombokMembers {

    private final SignatureRenderer renderer;
    private final DocTrees docTrees;
    private final Elements elements;

    LombokMembers(SignatureRenderer renderer, DocTrees docTrees, Elements elements) {
        this.renderer = renderer;
        this.docTrees = docTrees;
        this.elements = elements;
    }

    /**
     * Appends the Lombok-generated constructors, accessors and builder of a type.
     * @param type The annotated type
     * @param indent The member indentation
     * @param output The buffer to append to
     */
    void render(TypeElement type, String indent, StringBuilder output) {
        if (type.getKind() != ElementKind.CLASS && type.getKind() != ElementKind.ENUM) {
            return;
        }
        List<VariableElement> instanceFields = ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                .filter(field -> !field.getModifiers().contains(Modifier.STATIC))
                .collect(Collectors.toList());

        if (type.getKind() == ElementKind.CLASS) {
            renderConstructors(type, instanceFields, indent, output);
        }
        renderAccessors(type, instanceFields, indent, output);
        if (type.getKind() == ElementKind.CLASS) {
            renderBuilder(type, instanceFields, indent, output);
        }
    }

    private void renderConstructors(TypeElement type, List<VariableElement> fields, String indent, StringBuilder output) {
        AnnotationMirror noArgs = find(type, "NoArgsConstructor");
        AnnotationMirror allArgs = find(type, "AllArgsConstructor");
        AnnotationMirror requiredArgs = find(type, "RequiredArgsConstructor");
        boolean hasExplicitConstructor = ElementFilter.constructorsIn(type.getEnclosedElements()).stream()
                .anyMatch(constructor -> elements.getOrigin(constructor) == Elements.Origin.EXPLICIT);
        boolean hasConstructorAnnotation = noArgs != null || allArgs != null || requiredArgs != null;

        List<VariableElement> assignableFields = fields.stream()
                .filter(field -> !(isFinal(type, field) && hasInitializer(field)))
                .collect(Collectors.toList());
        List<VariableElement> requiredFields = assignableFields.stream()
                .filter(field -> isFinal(type, field) || find(field, "NonNull") != null)
                .collect(Collectors.toList());

        if (noArgs != null && isPublic(noArgs, "access")) {
            appendConstructor(type, List.of(), indent, output);
        }
        if (requiredArgs != null && isPublic(requiredArgs, "access")) {
            appendConstructor(type, requiredFields, indent, output);
        }
        if (allArgs != null && isPublic(allArgs, "access")) {
            appendConstructor(type, assignableFields, indent, output);
        }
        if (!hasExplicitConstructor && !hasConstructorAnnotation) {
            if (find(type, "Value") != null && find(type, "Builder") == null) {
                appendConstructor(type, assignableFields, indent, output);
            } else if (find(type, "Data") != null && find(type, "Builder") == null) {
                appendConstructor(type, requiredFields, indent, output);
            }
        }
    }

    private void renderAccessors(TypeElement type, List<VariableElement> fields, String indent, StringBuilder output) {
        AnnotationMirror classGetter = find(type, "Getter");
        AnnotationMirror classSetter = find(type, "Setter");
        boolean isData = find(type, "Data") != null;
        boolean isValue = find(type, "Value") != null;

        for (VariableElement field : fields) {
            AnnotationMirror fieldGetter = find(field, "Getter");
            boolean getter = fieldGetter != null
                    ? isPublic(fieldGetter, "value")
                    : (classGetter != null && isPublic(classGetter, "value")) || isData || isValue;
            String getterName = getterName(field);
            if (getter && !declaresMethod(type, getterName, 0)) {
                renderer.appendSummary(field, indent, output);
                output.append(indent).append("public ").append(renderer.type(field.asType())).append(' ')
                        .append(getterName).append("();\n");
            }

            AnnotationMirror fieldSetter = find(field, "Setter");
            boolean setter = !isFinal(type, field) && (fieldSetter != null
                    ? isPublic(fieldSetter, "value")
                    : (classSetter != null && isPublic(classSetter, "value")) || isData);
            String setterName = setterName(field);
            if (setter && !declaresMethod(type, setterName, 1)) {
                output.append(indent).append("public void ").append(setterName).append('(')
                        .append(renderer.type(field.asType())).append(' ').append(field.getSimpleName()).append(");\n");
            }
        }
    }

    private void renderBuilder(TypeElement type, List<VariableElement> fields, String indent, StringBuilder output) {
        AnnotationMirror builder = find(type, "Builder");
        if (builder == null || !isPublic(builder, "access")) {
            return;
        }
        String builderClass = stringAttribute(builder, "builderClassName", type.getSimpleName() + "Builder");
        String builderMethod = stringAttribute(builder, "builderMethodName", "builder");
        String memberIndent = indent + "    ";

        output.append(indent).append("public static ").append(builderClass).append(' ').append(builderMethod).append("();\n");
        if ("true".equals(attribute(builder, "toBuilder"))) {
            output.append(indent).append("public ").append(builderClass).append(" toBuilder();\n");
        }

        output.append(indent).append("public static class ").append(builderClass).append(" {\n");
        for (VariableElement field : fields) {
            if (isFinal(type, field) && hasInitializer(field) && find(field, "Builder.Default") == null) {
                continue;
            }
            String name = field.getSimpleName().toString();
            AnnotationMirror singular = find(field, "Singular");
            List<? extends TypeMirror> typeArguments = singular == null ? List.of() : typeArguments(field.asType());
            String singularName = singular == null ? null : stringAttribute(singular, "value", singularize(name));
            if (typeArguments.size() == 1) {
                String elementType = renderer.type(typeArguments.get(0));
                output.append(memberIndent).append("public ").append(builderClass).append(' ').append(singularName)
                        .append('(').append(elementType).append(' ').append(singularName).append(");\n");
                output.append(memberIndent).append("public ").append(builderClass).append(' ').append(name)
                        .append("(Collection<? extends ").append(elementType).append("> ").append(name).append(");\n");
                output.append(memberIndent).append("public ").append(builderClass).append(" clear")
                        .append(capitalize(name)).append("();\n");
            } else if (typeArguments.size() == 2) {
                String keyType = renderer.type(typeArguments.get(0));
                String valueType = renderer.type(typeArguments.get(1));
                output.append(memberIndent).append("public ").append(builderClass).append(' ').append(singularName)
                        .append('(').append(keyType).append(" key, ").append(valueType).append(" value);\n");
                output.append(memberIndent).append("public ").append(builderClass).append(' ').append(name)
                        .append("(Map<? extends ").append(keyType).append(", ? extends ").append(valueType).append("> ")
                        .append(name).append(");\n");
                output.append(memberIndent).append("public ").append(builderClass).append(" clear")
                        .append(capitalize(name)).append("();\n");
            } else {
                output.append(memberIndent).append("public ").append(builderClass).append(' ').append(name)
                        .append('(').append(renderer.type(field.asType())).append(' ').append(name).append(");\n");
            }
        }
        output.append(memberIndent).append("public ").append(type.getSimpleName()).append(" build();\n");
        output.append(indent).append("}\n");
    }

    private void appendConstructor(TypeElement type, List<VariableElement> parameters, String indent, StringBuilder output) {
        output.append(indent).append("public ").append(type.getSimpleName())
                .append(parameters.stream()
                        .map(field -> renderer.type(field.asType()) + " " + field.getSimpleName())
                        .collect(Collectors.joining(", ", "(", ");\n")));
    }

    private static boolean isFinal(TypeElement type, VariableElement field) {
        return field.getModifiers().contains(Modifier.FINAL) || find(type, "Value") != null;
    }

    private boolean hasInitializer(VariableElement field) {
        Tree tree = docTrees.getTree(field);
        return tree instanceof VariableTree && ((VariableTree) tree).getInitializer() != null;
    }

    private static boolean declaresMethod(TypeElement type, String name, int parameterCount) {
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals(name) && method.getParameters().size() == parameterCount) {
                return true;
            }
        }
        return false;
    }

    private static String getterName(VariableElement field) {
        String name = field.getSimpleName().toString();
        if (field.asType().getKind() == TypeKind.BOOLEAN) {
            return hasIsPrefix(name) ? name : "is" + capitalize(name);
        }
        return "get" + capitalize(name);
    }

    private static String setterName(VariableElement field) {
        String name = field.getSimpleName().toString();
        if (field.asType().getKind() == TypeKind.BOOLEAN && hasIsPrefix(name)) {
            return "set" + name.substring(2);
        }
        return "set" + capitalize(name);
    }

    private static boolean hasIsPrefix(String name) {
        return name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2));
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String singularize(String name) {
        if (name.endsWith("ies") && name.length() > 3) {
            return name.substring(0, name.length() - 3) + "y";
        }
        if (name.endsWith("s") && name.length() > 1) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    private static List<? extends TypeMirror> typeArguments(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED ? ((DeclaredType) type).getTypeArguments() : List.of();
    }

    /**
     * Finds a Lombok annotation on an element by its name relative to the {@code lombok} package.
     * @param element The annotated element
     * @param name The annotation name, such as {@code Getter} or {@code Builder.Default}
     * @return The annotation, or null when the element does not carry it
     */
    private static AnnotationMirror find(Element element, String name) {
        String qualifiedName = "lombok." + name;
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
            if (annotationType.getQualifiedName().contentEquals(qualifiedName)) {
                return annotation;
            }
        }
        return null;
    }

    private static String attribute(AnnotationMirror annotation, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(name)) {
                return String.valueOf(entry.getValue().getValue());
            }
        }
        return null;
    }

    private static String stringAttribute(AnnotationMirror annotation, String name, String defaultValue) {
        String value = attribute(annotation, name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private static boolean isPublic(AnnotationMirror annotation, String accessAttribute) {
        String access = attribute(annotation, accessAttribute);
        return access == null || access.endsWith("PUBLIC");
    }
}
