/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * The File System Utils.
 */
public class FileSystemUtils {

    /** The Constant FOLDER_TARGET. */
    public static final String FOLDER_TARGET = "target";

    /** The Constant PREFIX_DOT. */
    public static final String PREFIX_DOT = ".";

    /** The Constant PROJECT_JSON. */
    public static final String PROJECT_JSON = "project.json";
    /** The Constant SEPARATOR. */
    public static final String SEPARATOR = "/";
    /** The Constant DOT_GIT. */
    public static final String DOT_GIT = ".git"; //$NON-NLS-1$
    /** The Constant PROJECT_METADATA_FILE_NAME. */
    public static final String PROJECT_METADATA_FILE_NAME = PROJECT_JSON; // $NON-NLS-1$
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(FileSystemUtils.class);
    /** The Constant DIRIGIBLE_GIT_ROOT_FOLDER. */
    private static final String DIRIGIBLE_GIT_ROOT_FOLDER = "DIRIGIBLE_GIT_ROOT_FOLDER"; //$NON-NLS-1$
    /** The Constant DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER. */
    private static final String DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER = "DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER"; //$NON-NLS-1$
    /** The Constant REPOSITORY_GIT_FOLDER. */
    private static final String REPOSITORY_GIT_FOLDER = "dirigible" + File.separator + "repository" + File.separator + DOT_GIT;
    /** The Constant DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER. */
    private static final String DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER = FOLDER_TARGET + File.separator + REPOSITORY_GIT_FOLDER; // $NON-NLS-1$
    /** The git root folder. */
    private static final String GIT_ROOT_FOLDER;

    static {
        if (!StringUtils.isEmpty(Configuration.get(DIRIGIBLE_GIT_ROOT_FOLDER))) {
            GIT_ROOT_FOLDER = Configuration.get(DIRIGIBLE_GIT_ROOT_FOLDER);
        } else if (!StringUtils.isEmpty(Configuration.get(DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER))) {
            GIT_ROOT_FOLDER = Configuration.get(DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER) + File.separator + REPOSITORY_GIT_FOLDER;
        } else {
            GIT_ROOT_FOLDER = DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER;
        }
    }


    /**
     * The Class ProjectsFinder.
     */
    private static class ProjectsFinder extends SimpleFileVisitor<Path> {

        /** The projects. */
        private final List<File> projects = new ArrayList<File>();

        /**
         * Gets the projects.
         *
         * @return the projects
         */
        public List<File> getProjects() {
            return projects;
        }

        /**
         * Visit file.
         *
         * @param path the path
         * @param attrs the attrs
         * @return the file visit result
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            File file = path.toFile();
            if (file.exists() && file.isFile() && file.getName()
                                                      .equals(PROJECT_METADATA_FILE_NAME)) {
                projects.add(file.getParentFile());
            }
            return CONTINUE;
        }

        /**
         * Visit file failed.
         *
         * @param file the file
         * @param exc the exc
         * @return the file visit result
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            if (logger.isErrorEnabled()) {
                logger.error(exc.getMessage(), exc);
            }
            return CONTINUE;
        }
    }


    /**
     * The Class Finder.
     */
    private static class Finder extends SimpleFileVisitor<Path> {

        /** The matcher. */
        private final PathMatcher matcher;

        /** The files. */
        private final List<String> files = new ArrayList<String>();

        /**
         * Instantiates a new finder.
         *
         * @param pattern the pattern
         */
        Finder(String pattern) {
            matcher = FileSystems.getDefault()
                                 .getPathMatcher("glob:" + pattern);
        }

        /**
         * Done.
         */
        void done() {}

        /**
         * Visit file.
         *
         * @param file the file
         * @param attrs the attrs
         * @return the file visit result
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        /**
         * Find.
         *
         * @param file the file
         */
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                files.add(file.toString());
            }
        }

        /**
         * Pre visit directory.
         *
         * @param dir the dir
         * @param attrs the attrs
         * @return the file visit result
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        /**
         * Visit file failed.
         *
         * @param file the file
         * @param exc the exc
         * @return the file visit result
         */
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (logger.isErrorEnabled()) {
                logger.error(exc.getMessage(), exc);
            }
            return CONTINUE;
        }

        /**
         * Gets the files.
         *
         * @return the files
         */
        public List<String> getFiles() {
            return files;
        }
    }

    /**
     * Save file.
     *
     * @param workspacePath the workspace path
     * @param content the content
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void saveFile(String workspacePath, byte[] content) throws FileNotFoundException, IOException {
        createFoldersIfNecessary(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(FilenameUtils.normalize(workspacePath));
        if (content == null) {
            content = new byte[] {};
        }
        Files.write(path, content);
    }

    /**
     * Creates the folders if necessary.
     *
     * @param workspacePath the workspace path
     */
    public static void createFoldersIfNecessary(String workspacePath) {
        String normalizedPath = FilenameUtils.normalize(workspacePath);
        int lastIndexOf = normalizedPath.lastIndexOf(File.separator);
        if (lastIndexOf > 0) {
            String directory = normalizedPath.substring(0, lastIndexOf);
            if (!directoryExists(directory)) {
                createFolder(directory);
            }
        }
    }

    /**
     * Creates the folder.
     *
     * @param workspacePath the workspace path
     * @return true, if successful
     */
    public static boolean createFolder(String workspacePath) {
        File folder = new File(FilenameUtils.normalize(workspacePath));
        if (!folder.exists()) {
            try {
                FileUtils.forceMkdir(folder.getCanonicalFile());
            } catch (IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * Directory exists.
     *
     * @param location the location
     * @return true, if successful
     */
    public static boolean directoryExists(String location) {
        return exists(location) && isDirectory(location);
    }

    /**
     * Exists.
     *
     * @param location the location
     * @return true, if successful
     */
    public static boolean exists(String location) {
        if ((location == null) || location.length() == 0) {
            return false;
        }
        Path path;
        try {
            String normalizedPath = FilenameUtils.normalize(location);
            path = FileSystems.getDefault()
                              .getPath(normalizedPath);
            File file = path.toFile();
            return file.exists() && file.getCanonicalFile()
                                        .getName()
                                        .equals(file.getName());
        } catch (java.nio.file.InvalidPathException | IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage());
            }
            return false;
        }

        // return Files.exists(path);
    }

    /**
     * Checks if is directory.
     *
     * @param location the location
     * @return true, if is directory
     */
    public static boolean isDirectory(String location) {
        Path path;
        try {
            String normalizedPath = FilenameUtils.normalize(location);
            path = FileSystems.getDefault()
                              .getPath(normalizedPath);
        } catch (java.nio.file.InvalidPathException e) {
            return false;
        }
        return path.toFile()
                   .isDirectory();
    }

    public static void saveFile(String workspacePath, InputStream contentInputStream) throws IOException {
        createFoldersIfNecessary(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(FilenameUtils.normalize(workspacePath));

        try (OutputStream out = Files.newOutputStream(path)) {
            IOUtils.copyLarge(contentInputStream, out);
        }
    }

    /**
     * Load file.
     *
     * @param workspacePath the workspace path
     * @return the byte[]
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static byte[] loadFile(String workspacePath) throws FileNotFoundException, IOException {
        String normalizedPath = FilenameUtils.normalize(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(normalizedPath);
        // if (Files.exists(path)) {
        if (path.toFile()
                .exists()) {
            return Files.readAllBytes(path);
        }
        return null;
    }

    public static InputStream loadFileStream(String workspacePath) throws IOException {
        String normalizedPath = FilenameUtils.normalize(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(normalizedPath);

        if (path.toFile()
                .exists()) {
            return Files.newInputStream(path);
        }
        return null;
    }

    /**
     * Move file.
     *
     * @param workspacePathOld the workspace path old
     * @param workspacePathNew the workspace path new
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void moveFile(String workspacePathOld, String workspacePathNew) throws FileNotFoundException, IOException {
        createFoldersIfNecessary(workspacePathNew);
        Path pathOld = FileSystems.getDefault()
                                  .getPath(workspacePathOld);
        Path pathNew = FileSystems.getDefault()
                                  .getPath(workspacePathNew);
        Files.move(pathOld, pathNew);
    }

    /**
     * Copy file.
     *
     * @param srcPath the src path
     * @param destPath the dest path
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyFile(String srcPath, String destPath) throws FileNotFoundException, IOException {
        createFoldersIfNecessary(destPath);
        FileSystem fileSystem = FileSystems.getDefault();
        Path srcFile = fileSystem.getPath(srcPath);
        Path destFile = fileSystem.getPath(destPath);
        if (fileExists(destFile.toString())) {
            String fileName = destFile.getFileName()
                                      .toString();
            String destFilePath = destPath.substring(0, destPath.lastIndexOf(fileName));
            String fileTitle = "";
            String fileExt = "";
            if (fileName.indexOf('.') != -1) {
                fileTitle = fileName.substring(0, fileName.lastIndexOf(PREFIX_DOT));
                fileExt = fileName.substring(fileName.lastIndexOf(PREFIX_DOT));
            } else {
                fileTitle = fileName;
            }
            int inc = 1;
            fileName = fileTitle + " (copy " + inc + ")" + fileExt;
            while (fileExists(destFilePath + fileName)) {
                fileName = fileTitle + " (copy " + ++inc + ")" + fileExt;
            }
            destFile = fileSystem.getPath(destFilePath + fileName);
        }
        FileUtils.copyFile(srcFile.toFile(), destFile.toFile());
    }

    /**
     * File exists.
     *
     * @param location the location
     * @return true, if successful
     */
    public static boolean fileExists(String location) {
        return exists(location) && !isDirectory(location);
    }

    /**
     * Copy folder.
     *
     * @param srcPath the src path
     * @param destPath the dest path
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyFolder(String srcPath, String destPath) throws FileNotFoundException, IOException {
        createFoldersIfNecessary(destPath);
        FileSystem fileSystem = FileSystems.getDefault();
        Path srcDir = fileSystem.getPath(srcPath);
        Path destDir = fileSystem.getPath(destPath);
        FileUtils.copyDirectory(srcDir.toFile(), destDir.toFile(), true);
    }

    /**
     * Copy folder.
     *
     * @param srcPath the src path
     * @param destPath the dest path
     * @param filter the filter
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyFolder(String srcPath, String destPath, FileFilter filter) throws FileNotFoundException, IOException {
        createFoldersIfNecessary(destPath);
        FileSystem fileSystem = FileSystems.getDefault();
        Path srcDir = fileSystem.getPath(srcPath);
        Path destDir = fileSystem.getPath(destPath);
        FileUtils.copyDirectory(srcDir.toFile(), destDir.toFile(), filter, true);
    }

    /**
     * Removes the file.
     *
     * @param workspacePath the workspace path
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void removeFile(String workspacePath) throws FileNotFoundException, IOException {
        Path path = FileSystems.getDefault()
                               .getPath(workspacePath);
        // logger.debug("Deleting file: " + file);

        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(path, opts, Integer.MAX_VALUE, new FileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

                if (dir.toFile()
                       .exists()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("Deleting directory: %s", dir));
                    }
                    try {
                        Files.delete(dir);
                    } catch (java.nio.file.NoSuchFileException e) {
                        logger.trace(String.format("Directory already has been deleted: %s", dir));
                    } catch (DirectoryNotEmptyException e) {
                        logger.trace(String.format("Directory not empty: %s", dir));
                        FileUtils.deleteDirectory(dir.toFile());
                    } catch (IOException e) {
                        logger.trace(String.format("Directory cannot be deleted due to permissions issues: %s", dir));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // if (Files.exists(file)) {
                if (file.toFile()
                        .exists()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("Deleting file: %s", file));
                    }
                    try {
                        Files.delete(file);
                    } catch (java.nio.file.NoSuchFileException e) {
                        logger.trace(String.format("File already has been deleted: %s", file));
                    } catch (IOException x) {
                        logger.trace(String.format("File cannot be deleted due to permissions issues: %s", file));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                if (logger.isErrorEnabled()) {
                    logger.error(String.format("Error in file: %s", file), exc);
                }
                return FileVisitResult.CONTINUE;
            }
        });

    }

    /**
     * Creates the file.
     *
     * @param workspacePath the workspace path
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean createFile(String workspacePath) throws IOException {
        createFoldersIfNecessary(workspacePath);
        File file = new File(workspacePath);
        if (!file.exists()) {
            return file.createNewFile();
        }
        return true;
    }

    /**
     * Gets the extension.
     *
     * @param workspacePath the workspace path
     * @return the extension
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getExtension(String workspacePath) throws IOException {
        File f = new File(workspacePath);
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if ((i >= 0) && (i < (s.length() - 1))) {
            ext = s.substring(i + 1)
                   .toLowerCase();
        }
        return ext;
    }

    /**
     * Gets the owner.
     *
     * @param workspacePath the workspace path
     * @return the owner
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getOwner(String workspacePath) throws IOException {
        String convertedPath = convertToWorkspacePath(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(convertedPath);
        if (new File(convertedPath).exists()) {
            return Files.getOwner(path)
                        .getName();
        } else {
            return "none";
        }
    }

    /**
     * Convert to workspace path.
     *
     * @param path the path
     * @return the string
     */
    private static String convertToWorkspacePath(String path) {
        String normalizedPath = FilenameUtils.normalize(path);
        String workspacePath = null;
        if (normalizedPath.startsWith(SEPARATOR)) {
            workspacePath = normalizedPath.substring(SEPARATOR.length());
        } else {
            workspacePath = normalizedPath;
        }
        workspacePath = workspacePath.replace(SEPARATOR, File.separator);
        return workspacePath;
    }

    /**
     * Gets the modified at.
     *
     * @param workspacePath the workspace path
     * @return the modified at
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Date getModifiedAt(String workspacePath) throws IOException {
        String convertedPath = convertToWorkspacePath(workspacePath);
        Path path = FileSystems.getDefault()
                               .getPath(convertedPath);
        if (new File(convertedPath).exists()) {
            return new Date(Files.getLastModifiedTime(path)
                                 .toMillis());
        } else {
            return new Date();
        }
    }

    /**
     * List files including symbolic links.
     *
     * @param directory the source directory
     * @return the list of files
     * @throws IOException IO error
     */
    public static File[] listFiles(File directory) throws IOException {
        Path link = Paths.get(directory.getAbsolutePath());
        Stream<Path> filesStream = Files.list(link);
        try {
            Path[] paths = filesStream.toArray(size -> new Path[size]);
            File[] files = new File[paths.length];
            for (int i = 0; i < paths.length; i++) {
                files[i] = paths[i].toFile();
            }
            return files;
        } finally {
            filesStream.close();
        }
    }

    /**
     * Force create directory and all its parents.
     *
     * @param firstSegment the first segment
     * @param segments the rest segments
     * @return the resulting path
     * @throws IOException IO error
     */
    public static File forceCreateDirectory(String firstSegment, String... segments) throws IOException {
        Path dir = Paths.get(firstSegment, segments);
        if (dir.toFile()
               .exists()) {
            return dir.toFile();
        }
        return Files.createDirectories(dir)
                    .toFile();
    }

    /**
     * Delete directory.
     *
     * @param firstSegment the first segment
     * @param segments the segments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void deleteDirectory(String firstSegment, String... segments) throws IOException {
        Path dir = Paths.get(firstSegment, segments);
        File file = new File(String.valueOf(dir));
        FileUtils.deleteDirectory(file);
    }

    /**
     * Gets the git repositories.
     *
     * @param user the user
     * @param workspace the workspace
     * @return the git repositories
     */
    public static List<String> getGitRepositories(String user, String workspace) {
        File gitRoot = getGitDirectory(user, workspace);
        if (gitRoot == null) {
            return new ArrayList<String>();
        }
        return Arrays.asList(gitRoot.listFiles())
                     .stream()
                     .filter(e -> !e.isFile())
                     .map(e -> e.getName())
                     .collect(Collectors.toList());
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @return the directory
     */
    public static File getGitDirectory(String user, String workspace) {
        return FileSystemUtils.getDirectory(GIT_ROOT_FOLDER, user, workspace);
    }

    /**
     * Returns the directory by segments.
     *
     * @param firstSegment the first segment
     * @param segments the rest segments
     * @return the resulting path
     */
    public static File getDirectory(String firstSegment, String... segments) {
        Path dir = Paths.get(firstSegment, segments);
        if (dir.toFile()
               .exists()) {
            return dir.toFile();
        }
        return null;
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryURI the repository URI
     * @return the directory
     */
    public static File getGitDirectory(String user, String workspace, String repositoryURI) {
        String repositoryName = generateGitRepositoryName(repositoryURI);
        return FileSystemUtils.getDirectory(GIT_ROOT_FOLDER, user, workspace, repositoryName);
    }

    /**
     * Generate the local repository name.
     *
     * @param repositoryURI the URI odf the repository
     * @return the generated local name
     */
    public static String generateGitRepositoryName(String repositoryURI) {
        int separatorLastIndexOf = repositoryURI.lastIndexOf(SEPARATOR) + 1;
        int gitLastIndexOf = repositoryURI.lastIndexOf(DOT_GIT);
        String repositoryName = repositoryURI;
        if (separatorLastIndexOf >= 0 && gitLastIndexOf > 0) {
            repositoryName = repositoryURI.substring(separatorLastIndexOf, gitLastIndexOf);
        } else if (separatorLastIndexOf >= 0) {
            repositoryName = repositoryURI.substring(separatorLastIndexOf);
        }
        return repositoryName;
    }

    /**
     * Get the directory for git for deep projects.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryName the repository URI
     * @return the directory
     */
    public static File getGitDeepDirectoryByRepositoryName(String user, String workspace, String repositoryName) {
        File workspaceGitDirectory = FileSystemUtils.getDirectory(GIT_ROOT_FOLDER, user, workspace);
        if (workspaceGitDirectory == null) {
            return null;
        }
        File[] files = workspaceGitDirectory.listFiles();
        for (File child : files) {
            if (child.isDirectory()) {
                File foundMaybe = checkSubfolder(child, repositoryName);
                if (foundMaybe != null) {
                    return foundMaybe;
                }
            }
        }

        return null;
    }

    /**
     * Check subfolder.
     *
     * @param rootFolder the root folder
     * @param repositoryName the repository name
     * @return the file
     */
    private static File checkSubfolder(File rootFolder, String repositoryName) {
        if (rootFolder.getName()
                      .startsWith(PREFIX_DOT)
                || rootFolder.getName()
                             .equals(FOLDER_TARGET)) {
            return null;
        }
        if (rootFolder.getName()
                      .equals(repositoryName)) {
            File[] rootFiles = rootFolder.listFiles();
            for (File projectMaybe : rootFiles) {
                if (projectMaybe.isFile()) {
                    if (projectMaybe.getName()
                                    .equals(PROJECT_JSON)) {
                        return projectMaybe.getParentFile();
                    }
                } else {
                    File foundMaybe = checkSubfolder(projectMaybe, repositoryName);
                    if (foundMaybe != null) {
                        return foundMaybe;
                    }
                }
            }
        } else {
            File[] rootFiles = rootFolder.listFiles();
            for (File projectMaybe : rootFiles) {
                if (projectMaybe.isDirectory()) {
                    File foundMaybe = checkSubfolder(projectMaybe, repositoryName);
                    if (foundMaybe != null) {
                        return foundMaybe;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the git repository projects.
     *
     * @param user the user
     * @param workspace the workspace
     * @param repositoryName the repository name
     * @return the git repository projects
     */
    public static List<String> getGitRepositoryProjects(String user, String workspace, String repositoryName) {
        List<File> projects = getGitRepositoryProjectsFiles(user, workspace, repositoryName);
        if (projects != null) {
            return projects.stream()
                           .map(e -> e.getName())
                           .collect(Collectors.toList());
        }
        if (logger.isWarnEnabled()) {
            logger.warn(String.format("Cannot enumerate the projects under a git folder for user, workspace and path: %s, %s, %s", user,
                    workspace, repositoryName));
        }
        return new ArrayList<String>();
    }

    /**
     * Gets the git repository projects files.
     *
     * @param user the user
     * @param workspace the workspace
     * @param repositoryName the repository name
     * @return the git repository projects files
     */
    public static List<File> getGitRepositoryProjectsFiles(String user, String workspace, String repositoryName) {
        File gitRepository = getGitDirectoryByRepositoryName(user, workspace, repositoryName);
        return getGitRepositoryProjects(gitRepository);
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryName the repository URI
     * @return the directory
     */
    public static File getGitDirectoryByRepositoryName(String user, String workspace, String repositoryName) {
        File directGitDirectory = FileSystemUtils.getDirectory(GIT_ROOT_FOLDER, user, workspace, repositoryName);
        return directGitDirectory;
    }

    /**
     * Gets the git repository projects.
     *
     * @param gitRepository the git repository
     * @return the git repository projects
     */
    public static List<File> getGitRepositoryProjects(File gitRepository) {
        if (gitRepository != null) {
            EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            ProjectsFinder projectsFinder = new ProjectsFinder();
            try {
                Files.walkFileTree(Paths.get(gitRepository.getAbsolutePath()), opts, Integer.MAX_VALUE, projectsFinder);
            } catch (IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
            List<File> gitProjects = projectsFinder.getProjects();

            return gitProjects;
        }
        return null;
    }

    /**
     * Find.
     *
     * @param root the root
     * @param pattern the pattern
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final List<String> find(String root, String pattern) throws IOException {
        Path startingDir = Paths.get(root);
        Finder finder = new Finder(pattern);
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(startingDir, opts, Integer.MAX_VALUE, finder);
        finder.done();
        return finder.getFiles();
    }
}
