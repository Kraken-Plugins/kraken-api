package com.kraken.llms;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders types and members as compact Java stubs. Type names are written without their package so the dump
 * stays readable; every Kraken type's package is given by the {@code package} line it is listed under.
 */
final class SignatureRenderer {

    private static final int MAX_SUMMARY_LENGTH = 300;
    private static final int MAX_CONSTANT_LENGTH = 80;
    private static final int MAX_LINE_LENGTH = 120;
    private static final String API_PACKAGE_PREFIX = "com.kraken.api.";

    private final DocletEnvironment environment;
    private final DocTrees docTrees;
    private final Elements elements;
    private final Types types;
    private final LombokMembers lombokMembers;

    SignatureRenderer(DocletEnvironment environment) {
        this.environment = environment;
        this.docTrees = environment.getDocTrees();
        this.elements = environment.getElementUtils();
        this.types = environment.getTypeUtils();
        this.lombokMembers = new LombokMembers(this, docTrees, elements);
    }

    /**
     * Appends a type, its visible members, its Lombok-generated members and its nested types.
     * @param type The type to render
     * @param indent The indentation of the type declaration line
     * @param output The buffer to append to
     */
    void renderType(TypeElement type, String indent, StringBuilder output) {
        appendSummary(type, indent, output);
        output.append(indent).append(typeDeclaration(type)).append(" {\n");
        String memberIndent = indent + "    ";
        boolean insideInterface = type.getKind().isInterface();

        if (type.getKind() == ElementKind.ENUM) {
            List<String> constants = type.getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT)
                    .map(element -> element.getSimpleName().toString())
                    .collect(Collectors.toList());
            appendWrapped(constants, memberIndent, output);
        }

        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            if (isVisible(field)) {
                appendSummary(field, memberIndent, output);
                output.append(memberIndent).append(fieldDeclaration(field, insideInterface)).append('\n');
            }
        }

        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            if (isVisible(constructor) && !isInjectConstructor(constructor)) {
                appendSummary(constructor, memberIndent, output);
                output.append(memberIndent).append(executableDeclaration(constructor, insideInterface)).append('\n');
            }
        }

        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (isVisible(method) && !isUndocumentedExternalOverride(type, method)) {
                appendSummary(method, memberIndent, output);
                output.append(memberIndent).append(executableDeclaration(method, insideInterface)).append('\n');
            }
        }

        lombokMembers.render(type, memberIndent, output);

        for (TypeElement nested : ElementFilter.typesIn(type.getEnclosedElements())) {
            if (isVisible(nested)) {
                renderType(nested, memberIndent, output);
            }
        }

        output.append(indent).append("}\n");
    }

    /**
     * Returns the cleaned first sentence of an element's Javadoc.
     * @param element The documented element
     * @return The summary, or an empty string when the element has no Javadoc
     */
    String summary(Element element) {
        DocCommentTree comment = element == null ? null : docTrees.getDocCommentTree(element);
        if (comment == null) {
            return "";
        }
        String text = comment.getFirstSentence().stream().map(DocTree::toString).collect(Collectors.joining());
        text = text.replaceAll("\\{@(?:link|linkplain)\\s+[^\\s}]+\\s+([^}]+)}", "$1")
                .replaceAll("\\{@(?:link|linkplain)\\s+([^\\s}]+)\\s*}", "$1")
                .replaceAll("\\{@(?:code|literal|value)\\s*([^}]*)}", "$1")
                .replaceAll("\\{@inheritDoc\\s*}", "")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("(^|[\\s(])#(\\w)", "$1$2")
                .replace('#', '.')
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > MAX_SUMMARY_LENGTH) {
            text = text.substring(0, MAX_SUMMARY_LENGTH - 3).trim() + "...";
        }
        return text;
    }

    /**
     * Appends an element's summary comment and deprecation marker, if it has either.
     * @param element The documented element
     * @param indent The indentation to use
     * @param output The buffer to append to
     */
    void appendSummary(Element element, String indent, StringBuilder output) {
        String summary = summary(element);
        if (!summary.isEmpty()) {
            output.append(indent).append("/** ").append(summary.replace("*/", "*\\/")).append(" */\n");
        }
        if (elements.isDeprecated(element)) {
            output.append(indent).append("@Deprecated\n");
        }
    }

    /**
     * Formats a type with simple class names, keeping enclosing class names for nested types.
     * @param type The type to format
     * @return The formatted type
     */
    String type(TypeMirror type) {
        switch (type.getKind()) {
            case DECLARED: {
                DeclaredType declared = (DeclaredType) type;
                String name = nestedName((TypeElement) declared.asElement());
                if (declared.getTypeArguments().isEmpty()) {
                    return name;
                }
                return name + declared.getTypeArguments().stream().map(this::type).collect(Collectors.joining(", ", "<", ">"));
            }
            case ARRAY:
                return type(((ArrayType) type).getComponentType()) + "[]";
            case TYPEVAR:
                return ((TypeVariable) type).asElement().getSimpleName().toString();
            case WILDCARD: {
                WildcardType wildcard = (WildcardType) type;
                if (wildcard.getExtendsBound() != null) {
                    return "? extends " + type(wildcard.getExtendsBound());
                }
                if (wildcard.getSuperBound() != null) {
                    return "? super " + type(wildcard.getSuperBound());
                }
                return "?";
            }
            case ERROR:
                return type.toString().replaceAll("\\b(?:[a-z_][a-z0-9_]*\\.)+([A-Z])", "$1");
            default:
                return type.getKind().isPrimitive() || type.getKind() == TypeKind.VOID
                        ? type.getKind().name().toLowerCase()
                        : type.toString();
        }
    }

    /**
     * Formats a parameter list for a method or constructor.
     * @param executable The method or constructor
     * @return The parameters including names, without surrounding parentheses
     */
    String parameters(ExecutableElement executable) {
        List<? extends VariableElement> parameters = executable.getParameters();
        List<String> rendered = new ArrayList<>();
        for (int index = 0; index < parameters.size(); index++) {
            VariableElement parameter = parameters.get(index);
            String parameterType;
            if (executable.isVarArgs() && index == parameters.size() - 1 && parameter.asType().getKind() == TypeKind.ARRAY) {
                parameterType = type(((ArrayType) parameter.asType()).getComponentType()) + "...";
            } else {
                parameterType = type(parameter.asType());
            }
            rendered.add(parameterType + " " + parameter.getSimpleName());
        }
        return String.join(", ", rendered);
    }

    private boolean isVisible(Element element) {
        return environment.isIncluded(element) && elements.getOrigin(element) == Elements.Origin.EXPLICIT;
    }

    /**
     * Constructors annotated with {@code @Inject} exist for Guice; plugin code injects the type instead.
     */
    private boolean isInjectConstructor(ExecutableElement constructor) {
        return constructor.getAnnotationMirrors().stream()
                .map(annotation -> ((TypeElement) annotation.getAnnotationType().asElement()).getQualifiedName().toString())
                .anyMatch(name -> name.equals("javax.inject.Inject") || name.equals("com.google.inject.Inject"));
    }

    /**
     * Whether a method has no Javadoc of its own and only implements or overrides a method declared outside the
     * Kraken API, such as RuneLite's {@code Widget} or {@code Overlay#render}. The inherited contract already
     * describes those, so listing them only adds noise.
     */
    private boolean isUndocumentedExternalOverride(TypeElement type, ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.STATIC) || docTrees.getDocCommentTree(method) != null) {
            return false;
        }
        for (TypeElement supertype : allSupertypes(type)) {
            if (supertype.getQualifiedName().toString().startsWith(API_PACKAGE_PREFIX)) {
                continue;
            }
            for (ExecutableElement candidate : ElementFilter.methodsIn(supertype.getEnclosedElements())) {
                if (candidate.getSimpleName().contentEquals(method.getSimpleName()) && elements.overrides(method, candidate, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<TypeElement> allSupertypes(TypeElement type) {
        Set<TypeElement> supertypes = new LinkedHashSet<>();
        Deque<TypeMirror> pending = new ArrayDeque<>(types.directSupertypes(type.asType()));
        while (!pending.isEmpty()) {
            TypeMirror next = pending.pop();
            if (next.getKind() == TypeKind.DECLARED && supertypes.add((TypeElement) ((DeclaredType) next).asElement())) {
                pending.addAll(types.directSupertypes(next));
            }
        }
        return supertypes;
    }

    private String nestedName(TypeElement type) {
        StringBuilder name = new StringBuilder(type.getSimpleName());
        Element enclosing = type.getEnclosingElement();
        while (enclosing instanceof TypeElement) {
            name.insert(0, enclosing.getSimpleName() + ".");
            enclosing = enclosing.getEnclosingElement();
        }
        return name.toString();
    }

    private String typeDeclaration(TypeElement type) {
        StringBuilder declaration = new StringBuilder(modifiers(type, false));
        switch (type.getKind()) {
            case INTERFACE:
                declaration.append("interface ");
                break;
            case ANNOTATION_TYPE:
                declaration.append("@interface ");
                break;
            case ENUM:
                declaration.append("enum ");
                break;
            case RECORD:
                declaration.append("record ");
                break;
            default:
                declaration.append("class ");
        }
        declaration.append(type.getSimpleName()).append(typeParameters(type.getTypeParameters()));

        if (type.getKind() == ElementKind.RECORD) {
            declaration.append(type.getRecordComponents().stream()
                    .map((RecordComponentElement component) -> type(component.asType()) + " " + component.getSimpleName())
                    .collect(Collectors.joining(", ", "(", ")")));
        }

        TypeMirror superclass = type.getSuperclass();
        if (type.getKind() == ElementKind.CLASS && superclass.getKind() == TypeKind.DECLARED
                && !((TypeElement) ((DeclaredType) superclass).asElement()).getQualifiedName().contentEquals("java.lang.Object")) {
            declaration.append(" extends ").append(type(superclass));
        }

        List<? extends TypeMirror> interfaces = type.getInterfaces();
        if (!interfaces.isEmpty() && type.getKind() != ElementKind.ANNOTATION_TYPE) {
            declaration.append(type.getKind() == ElementKind.INTERFACE ? " extends " : " implements ");
            declaration.append(interfaces.stream().map(this::type).collect(Collectors.joining(", ")));
        }
        return declaration.toString();
    }

    private String fieldDeclaration(VariableElement field, boolean insideInterface) {
        StringBuilder declaration = new StringBuilder(insideInterface ? "" : modifiers(field, false));
        declaration.append(type(field.asType())).append(' ').append(field.getSimpleName());
        Object constant = field.getConstantValue();
        if (constant != null) {
            declaration.append(" = ").append(constantLiteral(constant));
        }
        return declaration.append(';').toString();
    }

    private String executableDeclaration(ExecutableElement executable, boolean insideInterface) {
        StringBuilder declaration = new StringBuilder(modifiers(executable, insideInterface));
        String typeParameters = typeParameters(executable.getTypeParameters());
        if (!typeParameters.isEmpty()) {
            declaration.append(typeParameters).append(' ');
        }
        if (executable.getKind() == ElementKind.CONSTRUCTOR) {
            declaration.append(executable.getEnclosingElement().getSimpleName());
        } else {
            declaration.append(type(executable.getReturnType())).append(' ').append(executable.getSimpleName());
        }
        declaration.append('(').append(parameters(executable)).append(')');
        if (!executable.getThrownTypes().isEmpty()) {
            declaration.append(" throws ").append(executable.getThrownTypes().stream().map(this::type).collect(Collectors.joining(", ")));
        }
        return declaration.append(';').toString();
    }

    private String modifiers(Element element, boolean insideInterface) {
        StringBuilder modifiers = new StringBuilder();
        boolean isInterfaceType = element.getKind().isInterface();
        for (Modifier modifier : element.getModifiers()) {
            switch (modifier) {
                case PUBLIC:
                case ABSTRACT:
                    if (insideInterface || (modifier == Modifier.ABSTRACT && isInterfaceType)) {
                        continue;
                    }
                    break;
                case PROTECTED:
                case STATIC:
                case FINAL:
                case DEFAULT:
                case SEALED:
                case NON_SEALED:
                    break;
                default:
                    continue;
            }
            if (modifier == Modifier.FINAL && element.getKind() == ElementKind.ENUM) {
                continue;
            }
            modifiers.append(modifier).append(' ');
        }
        return modifiers.toString();
    }

    private String typeParameters(List<? extends TypeParameterElement> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return typeParameters.stream().map(parameter -> {
            List<String> bounds = parameter.getBounds().stream()
                    .filter(bound -> !bound.toString().equals("java.lang.Object"))
                    .map(this::type)
                    .collect(Collectors.toList());
            return bounds.isEmpty() ? parameter.getSimpleName().toString()
                    : parameter.getSimpleName() + " extends " + String.join(" & ", bounds);
        }).collect(Collectors.joining(", ", "<", ">"));
    }

    private String constantLiteral(Object constant) {
        if (constant instanceof String) {
            String text = ((String) constant).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            if (text.length() > MAX_CONSTANT_LENGTH) {
                text = text.substring(0, MAX_CONSTANT_LENGTH) + "...";
            }
            return '"' + text + '"';
        }
        if (constant instanceof Character) {
            return "'" + constant + "'";
        }
        if (constant instanceof Long) {
            return constant + "L";
        }
        if (constant instanceof Float) {
            return constant + "f";
        }
        return constant.toString();
    }

    private void appendWrapped(List<String> names, String indent, StringBuilder output) {
        if (names.isEmpty()) {
            return;
        }
        StringBuilder line = new StringBuilder(indent);
        for (int index = 0; index < names.size(); index++) {
            String item = names.get(index) + (index == names.size() - 1 ? ";" : ",");
            if (line.length() > indent.length() && line.length() + item.length() + 1 > MAX_LINE_LENGTH) {
                output.append(line).append('\n');
                line = new StringBuilder(indent);
            }
            if (line.length() > indent.length()) {
                line.append(' ');
            }
            line.append(item);
        }
        output.append(line).append('\n');
    }
}
