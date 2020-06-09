package ru.ifmo.rain.moshnikov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * A simple implementation of the {@link Impler} interface.
 */
public class Implementor implements Impler {

    /**
     * Collects the {@link String strings} in the {@link Stream stream} by append
     *
     * @param stream the input strings
     * @return the strings.
     */
    private static String collectCommaList(final Stream<String> stream) {
        return stream.collect(Collectors.joining(", "));
    }

    /**
     * Collects {@link String strings} in the {@link Stream stream} by append
     *
     * @param stream of the strings.
     * @return the resulting string.
     */
    private static String collectParameters(final Stream<String> stream) {
        return "(" + collectCommaList(stream.map(s -> s.replace('$', '.'))) + ")";
    }

    /**
     * Make {@link String string} by {@link Function#apply(Object) applying} transformer
     * @param string      to transform.
     * @param trans transformer.

     */
    private static String makeIfNotEmpty(final String string, final Function<String, String> trans) {
        if (!string.equals("")) {
            return trans.apply(string);
        }
        return "";
    }

    /**
     * Collects exceptions
     *
     * @param stream the stream of strings to collect.
     * @return the resulting string.
     */
    private static String collectExceptions(final Stream<String> stream) {
        return makeIfNotEmpty(collectCommaList(stream), s -> " throws " + s);
    }

    /**
     * Collects {@link String strings} in the {@link Stream stream} by append
     *
     * @param stream of the strings.
     * @return the resulting string.
     */
    private static String collectGenericParameters(final Stream<String> stream) {
        return makeIfNotEmpty(collectCommaList(stream), s -> "<" + s + ">");
    }

    private static String giveImplName(final Class<?> clazz) {
        return clazz.getSimpleName().concat("Impl");
    }
    /**
     * Returns a {@link String string} type of this String
     *
     *
     * @param token the class.
     * @return the def value.
     */
    private static String getDefVal(final Class<?> token) {
        if (token.equals(boolean.class)) {
            return "false";
        } else if (token.equals(void.class)) {
            return "";
        } else if (token.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    private static String getExecutableTypeAndName(final Executable executable) {
        if (executable instanceof Constructor) {
            return giveImplName(executable.getDeclaringClass());
        } else {
            return executable.getAnnotatedReturnType().getType().getTypeName().replace('$', '.') + " " + executable.getName();
        }
    }

    /**
     * Converting the given {@link String string} to a Unicode.
     *
     * @param string to convert.
     * @return the converted.
     */
    private static String escapeUnicode(final String string) {
        final var sb = new StringBuilder();
        for (final char c : string.toCharArray()) {
            if (c >= 128) {
                sb.append(String.format("\\u%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Return all abstract methods {@link Method methods}.
     *
     * @param methods    the array of methods.
     * @param collection where methods will be.
     */
    private static void addAbstractMethods(final Method[] methods, final Set<MethodWrap> collection) {
        Arrays.stream(methods).filter(method -> Modifier.isAbstract(method.getModifiers())).map(MethodWrap::new)
                .collect(Collectors.toCollection(() -> collection));
    }

    private static void addFinalMethods(final Method[] methods, final Set<MethodWrap> collection) {
        Arrays.stream(methods).filter(method -> Modifier.isFinal(method.getModifiers())).map(MethodWrap::new)
                .collect(Collectors.toCollection(() -> collection));
    }

    /**
     * Returns all abstract methods
     *
     * @param token to be checked.
     * @return the set with the required methods.
     */
    private static Set<MethodWrap> getAbstractMethods(final Class<?> token) {
        final Set<MethodWrap> collection = new HashSet<>();
        addAbstractMethods(token.getMethods(), collection);
        for (var tok = token; tok != null; tok = tok.getSuperclass()) {
            addAbstractMethods(tok.getDeclaredMethods(), collection);
        }
        return collection;
    }

    private static Set<MethodWrap> getFinalMethods(final Class<?> clazz) {
        final Set<MethodWrap> collection = new HashSet<>();
        addFinalMethods(clazz.getMethods(), collection);
        for (var token = clazz; token != null; token = token.getSuperclass()) {
            addFinalMethods(token.getDeclaredMethods(), collection);
        }
        return collection;
    }


    /**
     * Returns a {@link String string} parameters.
     * @param decl the declaration to collect type parameters of.
     * @return the resulting string.
     * @throws NullPointerException
     */
    private static String getTypeParametersString(final GenericDeclaration decl) {
        return collectGenericParameters(Arrays.stream(decl.getTypeParameters()).map(TypeVariable::getName));
    }

    /**
     * A method {@link Method methods} used to add them in a {@link Set set} or a {@link Map map}
     */
    private static class MethodWrap {

        /**
         * The wrapped instance of {@link Method}.
         */
        private final Method method;

        /**
         * for calculate {@link #hashCode()}.
         */
        private final static int BASE = 20;
        /**
         * for calculate {@link #hashCode()}.
         */
        private final static int MOD = 1000997890;

        /**
         * Construct a wrap for the provided {@link Method method}.
         */
        MethodWrap(Method method) {
            this.method = method;
        }

        /**
         * Checking if objects are equals.
         */
        @Override
        public boolean equals(Object object) {
            if (object instanceof MethodWrap) {
                final var other = (MethodWrap) object;
                return Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                        && method.getReturnType().equals(other.method.getReturnType())
                        && method.getName().equals(other.method.getName());
            }
            return false;
        }

        /**
         * Returns a hash code.

         */
        @Override
        public int hashCode() {
            return ((Arrays.hashCode(method.getParameterTypes())
                    + BASE * method.getReturnType().hashCode()) % MOD
                    + method.getName().hashCode() * BASE * BASE) % MOD;
        }

        /**
         * Returns the wrap method.
         */

        Method getMethod() {
            return method;
        }

    }

    /**
     * Creates all directories.
     *
     * @param path the path to create parent directories of.
     */
    private static void createParentDirectories(final Path path) throws IOException {
        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Returns a {@link Writer writer} that will be used
     * @param clazz the class.
     * @param path  the class path.
     * @return writer.
     * @throws IOException
     * @throws NullPointerException
     */
    private static Writer prepareWriter(final Class<?> clazz, final Path path) throws IOException {
        final var sourcePath = path.resolve(clazz.getPackage().getName().replace('.', File.separatorChar)).
                resolve(giveImplName(clazz).concat(".java"));
        createParentDirectories(sourcePath);
        return Files.newBufferedWriter(sourcePath, StandardOpenOption.CREATE);
    }

    /**
     * Writes the header of something
     * @param clazz  the class to write.
     * @param writer the writer to use.
     */
    private static void writeHeader(final Class<?> clazz, final Writer writer) throws IOException {
        writer.append("package").append(" ").append(clazz.getPackageName()).append(";").append(System.lineSeparator());
        writer.write(System.lineSeparator());
        writer.append("public").append(" ").append("class").
                append(" ").append(escapeUnicode(giveImplName(clazz))).append(escapeUnicode(getTypeParametersString(clazz))).
                append(" ").append(clazz.isInterface() ? "implements" : "extends").
                append(" ").append(escapeUnicode(clazz.getCanonicalName())).append(escapeUnicode(getTypeParametersString(clazz))).
                append(" ").append("{").append(System.lineSeparator());
    }

    /**
     * Writes the body of methods.
     * @param constructor the constructor.
     * @param writer      the writer.
     * @throws IOException
     * @throws NullPointerException
     */
    private static void writeConstructorBody(final Constructor constructor, final Writer writer) throws IOException {
        writer.append("   ").append("   ").append("super").
                append(escapeUnicode(collectParameters(Arrays.stream(constructor.getParameters()).map(Parameter::getName)))).
                append(";").append(System.lineSeparator());
    }


    /**
     * Writes the body of implementation of {@link Executable executable} using {@link Writer writer}.
     *
     * @param execut the executable to generate and write.
     * @param writer     the writer to use.
     */
    private static void writeExecutableBody(final Executable execut, final Writer writer) throws IOException {
        if (execut instanceof Constructor) {
            writeConstructorBody((Constructor) execut, writer);
        } else {
            Method method = (Method) execut;
            writer.append("   ").append("   ").append("return").
                    append(escapeUnicode(makeIfNotEmpty(getDefVal(method.getReturnType()), s -> " " + s))).
                    append(";").append(System.lineSeparator());
        }
    }

    /**
     * Writes the implementation
     *
     * @param executable something to write.
     * @param writer     the writer to use.
     */
    private static void writeExecutable(final Executable executable, final Writer writer) throws IOException {
        writer.write(System.lineSeparator());
        writer.append("    ").append(Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)).
                append(" ").append(escapeUnicode(makeIfNotEmpty(getTypeParametersString(executable), s -> s + " "))).
                append(escapeUnicode(getExecutableTypeAndName(executable))).
                append(escapeUnicode(collectParameters(Arrays.stream(executable.getParameters()).map(Parameter::toString)))).
                append(escapeUnicode(collectExceptions(Arrays.stream(executable.getExceptionTypes()).map(Class::getCanonicalName)))).
                append(" ").append("{").append(System.lineSeparator());
        writeExecutableBody(executable, writer);
        writer.append("    ").append("}").append(System.lineSeparator());
    }

    /**
     * Writes constructors
     *
     * @param clazz  the class to generate and write successor of.
     * @param writer the writer to use.
     */
    private static void writeConstructors(final Class<?> clazz, final Writer writer) throws IOException {
        final var constructors = Arrays.stream(clazz.getDeclaredConstructors()).
                filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())).toArray(Constructor<?>[]::new);
        for (final var constructor : constructors) {
            writeExecutable(constructor, writer);
        }
    }

    /**
     * Writes methods
     * using the specified {@link Writer writer}.
     *
     * @param clazz  the class to generate and write successor of.
     * @param writer the writer to use.
     */
    private static void writeMethods(final Class<?> clazz, final Writer writer) throws IOException {
        final var abstractMethods = getAbstractMethods(clazz);
        abstractMethods.removeAll(getFinalMethods(clazz));
        for (final var method : abstractMethods) {
            writeExecutable(method.getMethod(), writer);
        }
    }

    /**
     * Writes Footer
     *
     * @param writer the writer to use.
     */
    private static void writeFooter(final Writer writer) throws IOException {
        writer.write(System.lineSeparator());
        writer.write("}");
    }

    /**
     * @throws NullPointerException if {@code clazz} or {@code path} is {@code null}.
     */
    @Override
    public void implement(final Class<?> clazz, final Path path) throws ImplerException {
        final var constructors = clazz.getDeclaredConstructors();
        if (clazz.isPrimitive() || clazz.isArray() || Modifier.isFinal(clazz.getModifiers()) || Modifier.isPrivate(clazz.getModifiers())
                || clazz.equals(Enum.class)
                || constructors.length != 0 && Stream.of(constructors).allMatch(c -> Modifier.isPrivate(c.getModifiers()))) {
            throw new ImplerException(clazz.getCanonicalName().concat(" cannot be implemented"));
        }
        Objects.requireNonNull(path);
        try (final var resultWriter = prepareWriter(clazz, path)) {
            StringWriter stringWriter = new StringWriter();
            writeHeader(clazz, stringWriter);
            writeConstructors(clazz, stringWriter);
            writeMethods(clazz, stringWriter);
            writeFooter(stringWriter);
            resultWriter.write(escapeUnicode(stringWriter.toString()));
        } catch (final IOException e) {
            throw new ImplerException("Could not write to the output file", e);
        }
    }



    /**
     * Creates an {@link Implementor} and runs depend on the arguments provided.
     * <p>
     * Runs the {@link #implement(Class, Path)} method, which will convert the argument
     * to a class using the {@link Class#forName(String)} method and resolving the current working directory as the path.
     * <p>
     * If the arguments are incorrect will be an exception
     * @param args the provided arguments.
     */
    public static void main(final String[] args) {
        if (args == null) {
            return;
        }

        try {
            new Implementor().implement(Class.forName(args[0]), Path.of("."));
        } catch (final InvalidPathException e) {
            System.out.println("The provided target path is malformed:");
            System.out.println(e.getMessage());
        } catch (final ClassNotFoundException e) {
            System.out.println("Could not find the specified class:");
            System.out.println(e.getMessage());
        } catch (final ImplerException e) {
            System.out.println("An error occurred while implementing the specified class:");
            System.out.println(e.getMessage());
        }
    }

}