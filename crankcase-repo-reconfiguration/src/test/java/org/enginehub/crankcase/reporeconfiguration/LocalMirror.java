/*
 * SPDX-FileCopyrightText: EngineHub <https://www.enginehub.org/>
 * SPDX-License-Identifier: MPL-2.0
 */

package org.enginehub.crankcase.reporeconfiguration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

final class LocalMirror {
    static final String DEFAULT_GROUP = "com.example";
    static final String DEFAULT_NAME = "thing";
    static final String DEFAULT_VERSION = "1.0";
    static final String DEFAULT_NOTATION = DEFAULT_GROUP + ":" + DEFAULT_NAME + ":" + DEFAULT_VERSION;
    static final String DEFAULT_JAR = DEFAULT_NAME + "-" + DEFAULT_VERSION + ".jar";

    private final Path root;
    private final String mirrorSubpath;

    LocalMirror(Path root) {
        this(root, "");
    }

    LocalMirror(Path root, String mirrorSubpath) {
        this.root = root;
        this.mirrorSubpath = mirrorSubpath;
    }

    String baseUri() {
        String uri = root.toUri().toString();
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    void putArtifact() {
        putArtifact(DEFAULT_GROUP, DEFAULT_NAME, DEFAULT_VERSION);
    }

    void putArtifact(String group, String name, String version) {
        Path dir = artifactDir(group, name, version);
        writeString(dir.resolve(name + "-" + version + ".pom"), pom(group, name, version));
        writeJar(dir.resolve(name + "-" + version + ".jar"));
    }

    private Path artifactDir(String group, String name, String version) {
        Path dir = root.resolve(mirrorSubpath)
            .resolve(group.replace('.', '/'))
            .resolve(name)
            .resolve(version);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }

    private static String pom(String group, String name, String version) {
        return
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </project>
            """.formatted(group, name, version);
    }

    private static void writeString(Path target, String content) {
        try {
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeJar(Path jar) {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new ZipEntry("fixture.txt"));
            out.write("fixture".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
