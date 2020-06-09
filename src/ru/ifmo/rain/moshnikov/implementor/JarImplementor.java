package ru.ifmo.rain.moshnikov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class JarImplementor extends Implementor implements JarImpler {


    /**
     * Returns path-String
     *
     * @param token class
     * @return file path
     */
    private static String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create directory.
     *
     * @param root location
     * @return a {@code Path} locating
     */
    private static Path getTemp(Path root) throws ImplerException {
        root = root.toAbsolutePath();
        try {
            return Files.createTempDirectory(root, "jar-implementor");
        } catch (IOException e) {
            throw new ImplerException();
        }
    }

    /**
     * Make {@code .jar} file implementing something, that specified by provided {@code token}.
     * {@code .jar} File will be by {@code jarFile}.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path temp = getTemp(jarFile.toAbsolutePath().getParent());
        implement(token, temp);
        final String file = Path.of(temp.toString(), token.getPackageName().replace('.',
                File.separatorChar), token.getSimpleName() + "Impl.java").toString();
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String[] compilerArgs = new String[3];
        compilerArgs[0] = "-cp";
        compilerArgs[1] = temp + File.pathSeparator + getClassPath(token);
        compilerArgs[2] = file;
        final int exitCode = compiler.run(null, null, null, compilerArgs);
        if (exitCode != 0) {
            throw new ImplerException();
        }
        Manifest manifest = createManifest();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String classFileName = token.getPackageName().replace(".", "/") + "/";
            classFileName = classFileName + token.getSimpleName() + "Impl.class";
            out.putNextEntry(new ZipEntry(classFileName));
            Files.copy(Path.of(temp.toString() + "/" + classFileName), out);
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * It used for determine whether generate <code>.jar</code> or <code>.java</code> file.
     *
     * Please dont use null arguments.
     *
     * @param args console line arguments: <code>[-jar] className outputPath</code>
     */
    public static void main(final String[] args) {
        if ((args == null) ||
                (args.length != 1 && !(args.length == 3 && args[0].equals("-jar")))) {
            return;
        }
        try {
            new JarImplementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
        } catch (final InvalidPathException | ClassNotFoundException | ImplerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create manifest
     *
     * @return {@link Manifest} manifest
     */
    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Moshnikov Daniil Ivanovich");
        return manifest;
    }

}
