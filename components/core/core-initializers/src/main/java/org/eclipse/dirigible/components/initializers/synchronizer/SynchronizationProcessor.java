/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.platform.ProblemsFacade;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologicalDepleter;
import org.eclipse.dirigible.components.base.artefact.topology.TopologicalSorter;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyFactory;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.healthcheck.status.HealthCheckStatus;
import org.eclipse.dirigible.components.base.healthcheck.status.HealthCheckStatus.Jobs.JobStatus;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The Class SynchronizationProcessor.
 */
@Component
@Scope("singleton")
public class SynchronizationProcessor implements SynchronizationWalkerCallback, SynchronizerCallback, DisposableBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SynchronizationProcessor.class);

    /** The definitions. */
    private final Map<Synchronizer<? extends Artefact, ?>, Map<String, Definition>> definitions =
            Collections.synchronizedMap(new HashMap<>());

    /** The artefacts. */
    private final Map<String, Artefact> artefacts = Collections.synchronizedMap(new HashMap<>());

    /** The repository. */
    private final IRepository repository;

    /** The synchronizers. */
    private final List<Synchronizer<?, ?>> synchronizers;

    /** The errors. */
    private final Set<String> errors = Collections.synchronizedSet(new HashSet<>());

    /** The definition service. */
    private final DefinitionService definitionService;

    /** The synchronization watcher. */
    private final SynchronizationWatcher synchronizationWatcher;

    /** The initialized. */
    private final AtomicBoolean initialized;

    /** The prepared. */
    private final AtomicBoolean prepared;

    /** The processing. */
    private final AtomicBoolean processing;

    /**
     * Instantiates a new synchronization processor.
     *
     * @param repository the repository
     * @param synchronizers the synchronizers
     * @param definitionService the definition service
     * @param synchronizationWatcher the synchronization watcher
     */
    @Autowired
    public SynchronizationProcessor(IRepository repository, List<Synchronizer<?, ?>> synchronizers, DefinitionService definitionService,
            SynchronizationWatcher synchronizationWatcher) {
        this.repository = repository;
        this.synchronizers = Collections.synchronizedList(synchronizers);
        logger.info("Registered [{}] synchronizers: [{}]", synchronizers.size(), synchronizers);

        this.definitionService = definitionService;
        this.synchronizationWatcher = synchronizationWatcher;
        this.synchronizers.forEach(s -> s.setCallback(this));

        this.initialized = new AtomicBoolean(false);
        this.prepared = new AtomicBoolean(false);
        this.processing = new AtomicBoolean(false);
    }

    /**
     * Prepare synchronizers.
     */
    public synchronized void prepareSynchronizers() {
        logger.info("Preparing synchronizers...");
        this.synchronizers.forEach(s -> s.getService()
                                         .setRunningToAll(false));
        try {
            this.synchronizationWatcher.initialize(getRegistryFolder());
            prepared.set(true);
            logger.info("Synchronizers have been preapred.");
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Gets the registry folder.
     *
     * @return the registry folder
     */
    public String getRegistryFolder() {
        String internalRegistryPath = repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        File registry = Path.of(internalRegistryPath)
                            .toFile();
        if (!registry.exists()) {
            registry.mkdirs();
        }
        return internalRegistryPath;
    }

    /**
     * Force process synchronizers.
     */
    public void forceProcessSynchronizers() {
        this.synchronizationWatcher.force();
        processSynchronizers();
    }

    /**
     * Process synchronizers.
     */
    public void processSynchronizers() {

        if (!isSynchronizationNeeded()) {
            logger.debug("Skipping synchronization since it is not needed...");
            return;
        }
        logger.info("Executing synchronization...");

        processing.set(true);
        synchronizationWatcher.reset();

        try {

            prepare();

            // prepare map
            synchronizers.forEach(s -> definitions.put(s, Collections.synchronizedMap(new HashMap<>())));

            logger.trace("Collecting files...");

            // collect definitions for processing
            collectFiles();

            logger.debug("Collecting files done. {} known types of definitions collected - {}.", synchronizers.size(),
                    synchronizers.stream()
                                 .map(Synchronizer::getArtefactType)
                                 .toList());
            logger.trace("Loading definitions...");

            // mark the deleted definitions, which previousely processed
            markDeleted();

            // parse definitions to artefacts
            parseDefinitions();

            int countNew = 0;
            int countModified = 0;
            for (Artefact artefact : artefacts.values()) {
                if (ArtefactLifecycle.NEW.equals(artefact.getLifecycle())) {
                    logger.debug("Processing a new artefact: {}", artefact.getKey());
                    countNew++;
                }
                if (ArtefactLifecycle.MODIFIED.equals(artefact.getLifecycle())) {
                    logger.debug("Processing a modified artefact: {}", artefact.getKey());
                    countModified++;
                }
            }

            logger.debug("Loading of [{}] definitions done. [{}] artefacts found in total. [{}] new and [{}] modified.", definitions.size(),
                    artefacts.size(), countNew, countModified);

            if (countNew > 0 || countModified > 0 || !initialized.get()) {

                TopologicalSorter<TopologyWrapper<? extends Artefact>> sorter = new TopologicalSorter<>();
                TopologicalDepleter<TopologyWrapper<? extends Artefact>> depleter = new TopologicalDepleter<>();

                Collection<? extends Artefact> values = artefacts.values();
                List<TopologyWrapper<? extends Artefact>> wrappers = TopologyFactory.wrap(values, synchronizers);

                logger.trace("Topological sorting...");

                // topological sorting by dependencies
                wrappers = sorter.sort(wrappers);

                // reverse the order
                Collections.reverse(wrappers);

                logger.trace("Preparing for processing...");

                Set<TopologyWrapper<? extends Artefact>> undepleted = new HashSet<>();

                // preparing and depleting
                for (Synchronizer<? extends Artefact, ?> synchronizer : synchronizers) {
                    Set<TopologyWrapper<? extends Artefact>> unmodifiable = wrappers.stream()
                                                                                    .filter(w -> w.getSynchronizer()
                                                                                                  .equals(synchronizer))
                                                                                    .collect(Collectors.toSet());
                    try {
                        Set<TopologyWrapper<? extends Artefact>> results = depleter.deplete(unmodifiable, ArtefactPhase.PREPARE);
                        undepleted.addAll(results);
                        registerErrors(results, ArtefactLifecycle.PREPARED);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        addError(e.getMessage());
                    }
                }
                logger.trace("Preparing for processing done.");

                // return back to the sorted the order
                Collections.reverse(wrappers);

                logger.trace("Processing of artefacts...");
                // processing and depleting
                for (Synchronizer<? extends Artefact, ?> synchronizer : synchronizers) {
                    HealthCheckStatus.getInstance()
                                     .getJobs()
                                     .setStatus(synchronizer.getClass()
                                                            .getSimpleName(),
                                             JobStatus.Running);
                    Set<TopologyWrapper<? extends Artefact>> unmodifiable = wrappers.stream()
                                                                                    .filter(w -> w.getSynchronizer()
                                                                                                  .equals(synchronizer))
                                                                                    .collect(Collectors.toSet());
                    try {

                        // phase create
                        Set<TopologyWrapper<? extends Artefact>> results = depleter.deplete(unmodifiable, ArtefactPhase.CREATE);
                        undepleted.addAll(results);
                        registerErrors(results, ArtefactLifecycle.CREATED);

                        // phase update
                        results = depleter.deplete(unmodifiable, ArtefactPhase.UPDATE);
                        undepleted.addAll(results);
                        registerErrors(results, ArtefactLifecycle.UPDATED);

                        // phase start
                        results = depleter.deplete(unmodifiable, ArtefactPhase.START);
                        undepleted.addAll(results);
                        registerErrors(results, ArtefactLifecycle.STARTED);

                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        addError(e.getMessage());
                        HealthCheckStatus.getInstance()
                                         .getJobs()
                                         .setStatus(synchronizer.getClass()
                                                                .getSimpleName(),
                                                 JobStatus.Failed);
                    }
                    HealthCheckStatus.getInstance()
                                     .getJobs()
                                     .setStatus(synchronizer.getClass()
                                                            .getSimpleName(),
                                             JobStatus.Succeeded);
                }

                // Processing of cross-synchronizer artefacts once again due to eventual dependency issues
                int crossRetryCount = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.getIntValue();
                int crossRetryInterval = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.getIntValue();
                int retryCount = 0;
                if (!undepleted.isEmpty()) {
                    logger.warn("Cross-processing of undepleated artefacts...");
                    try {
                        while (!undepleted.isEmpty()) {
                            logger.info("Wait [{}] millis before next retry", crossRetryInterval);
                            Thread.sleep(crossRetryInterval);

                            logger.info("Retry [{}] - cross-processing of [{}] undepleated artefacts: [{}]", (retryCount + 1),
                                    undepleted.size(), undepleted);
                            Set<TopologyWrapper<? extends Artefact>> cross = new HashSet<>();
                            Set<TopologyWrapper<? extends Artefact>> results = depleter.deplete(undepleted, ArtefactPhase.PREPARE);
                            cross.addAll(results);
                            registerErrors(results, ArtefactLifecycle.PREPARED);

                            results = depleter.deplete(undepleted, ArtefactPhase.CREATE);
                            cross.addAll(results);
                            registerErrors(results, ArtefactLifecycle.CREATED);

                            results = depleter.deplete(undepleted, ArtefactPhase.UPDATE);
                            cross.addAll(results);
                            registerErrors(results, ArtefactLifecycle.UPDATED);

                            results = depleter.deplete(undepleted, ArtefactPhase.START);
                            cross.addAll(results);
                            registerErrors(results, ArtefactLifecycle.STARTED);

                            String crossArtefactsLeft = cross.stream()
                                                             .map(e -> e.getArtefact()
                                                                        .getKey())
                                                             .collect(Collectors.joining(", "));
                            if (!cross.isEmpty()) {
                                undepleted.clear();
                                undepleted.addAll(cross);
                                logger.warn("Retrying to deplete artefacts left after cross-processing: [{}] ", crossArtefactsLeft);
                            }
                            if (++retryCount == crossRetryCount) {
                                logger.error("Final retry completed. Left artefacts after cross-processing: [{}]", crossArtefactsLeft);
                                break;
                            }
                            logger.info("Retry [{}] completed - left [{}] undepleated artefacts: [{}].", retryCount, undepleted.size(),
                                    undepleted);
                        }
                    } catch (Exception e) {
                        logger.error("Error occurred while cross-processing of undepleated artefacts", e);
                    }
                    logger.warn("Cross-processing of undepleated artefacts done.");
                }

                logger.trace("Processing of artefacts done.");

            }

            logger.trace("Cleaning up removed artefacts...");

            // cleanup
            for (Synchronizer synchronizer : synchronizers) {
                List<? extends Artefact> registered = synchronizer.getService()
                                                                  .getAll();
                for (Artefact artefact : registered) {
                    if (synchronizer.isAccepted(artefact.getType())) {
                        if (!repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + artefact.getLocation())
                                       .exists()) {
                            synchronizer.cleanup(artefact);
                        }
                    }
                }
            }
            logger.trace("Cleaning up removed artefacts done.");

            // finishing
            for (Synchronizer synchronizer : synchronizers) {
                synchronizer.finishing();
            }
            logger.trace("Finalization step done.");

            // report results
            getErrors().forEach(errMsg -> {
                logger.error("Error occured during synchronization: [{}]", errMsg);
            });

            logger.info("Processing synchronizers completed!");

        } finally {
            if (logger.isDebugEnabled()) {
                int countCreated = 0;
                int countUpdated = 0;
                int countFailed = 0;
                int countFatal = 0;
                for (Artefact artefact : artefacts.values()) {
                    if (ArtefactLifecycle.CREATED.equals(artefact.getLifecycle()))
                        countCreated++;
                    if (ArtefactLifecycle.UPDATED.equals(artefact.getLifecycle()))
                        countUpdated++;
                    if (ArtefactLifecycle.FAILED.equals(artefact.getLifecycle()))
                        countFailed++;
                    if (ArtefactLifecycle.FATAL.equals(artefact.getLifecycle()))
                        countFatal++;
                }
                logger.debug(
                        "Processing synchronizers done. {} artefacts processed in total. {} ({}/{}) successful, {} failed and {} fatal.",
                        artefacts.size(), countCreated + countUpdated, countCreated, countUpdated, countFailed, countFatal);
            }
            // clear maps
            definitions.clear();
            artefacts.clear();

            initialized.set(true);
            processing.set(false);
        }
    }

    public boolean isSynchronizationNeeded() {
        logger.debug("Checking whether synchronization is needed...");

        if (!this.synchronizationWatcher.isModified() && initialized.get()) {
            logger.debug("Skipped synchronization as NO changes in the Registry.");
            return false;
        }

        if (!prepared.get()) {
            logger.debug("Skipped synchronization as the runtime is NOT prepared yet.");
            return false;
        }

        if (isSynchronizationRunning()) {
            logger.debug("Skipped synchronization as it is CURRENTLY IN PROGRESS.");
            return false;
        }

        return true;
    }

    public boolean isSynchronizationRunning() {
        return processing.get();
    }

    /**
     * Prepare.
     */
    private void prepare() {
        errors.clear();
        definitions.clear();
        artefacts.clear();
    }

    /**
     * Collect files.
     */
    private void collectFiles() {
        String registryFolder = getRegistryFolder();
        SynchronizationWalker synchronizationWalker = new SynchronizationWalker(this);
        try {
            synchronizationWalker.walk(registryFolder);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            addError(e.getMessage());
        }
    }

    /**
     * Mark deleted.
     */
    private void markDeleted() {
        Map<String, Definition> found = new HashMap<>();
        for (Synchronizer<? extends Artefact, ?> synchronizer : synchronizers) {
            Map<String, Definition> map = checkSynchronizerMap(synchronizer);
            Collection<Definition> immutableDefinitions = Collections.synchronizedCollection(map.values());
            for (Definition definition : immutableDefinitions) {
                found.put(definition.getLocation(), definition);
            }
        }
        List<Definition> existings = definitionService.getAll();
        for (Definition existing : existings) {
            if (found.get(existing.getLocation()) == null && !existing.getState()
                                                                      .equals(DefinitionState.DELETED)) {
                registerDeleteState(existing);
            }
        }

    }

    /**
     * Load definitions.
     */
    private void parseDefinitions() {
        for (Synchronizer synchronizer : synchronizers) {
            Map<String, Definition> map = checkSynchronizerMap(synchronizer);
            Collection<Definition> immutableDefinitions = Collections.synchronizedCollection(map.values());
            for (Definition definition : immutableDefinitions) {
                try {
                    if (definition.getContent() == null) {
                        String error = String.format("Content of %s has not been loaded correctly", definition.getLocation());
                        registerBrokenState(definition, error);
                        continue;
                    }

                    List<? extends Artefact> parsed;
                    switch (definition.getState()) {
                        case NEW: // brand new definition
                            try {
                                parsed = synchronizer.parse(definition.getLocation(), definition.getContent());
                                parsed.forEach(a -> {
                                    a.setPhase(ArtefactPhase.CREATE);
                                    synchronizer.setStatus((a), ArtefactLifecycle.NEW, "");
                                });
                                addArtefacts(parsed);
                                registerParsedState(definition);
                            } catch (ParseException e) {
                                registerBrokenState(definition, e.getMessage());
                            }
                            break;
                        case MODIFIED: // modified existing definition
                            try {
                                parsed = synchronizer.parse(definition.getLocation(), definition.getContent());
                                parsed.forEach(a -> {
                                    a.setPhase(ArtefactPhase.UPDATE);
                                    synchronizer.setStatus((a), ArtefactLifecycle.MODIFIED, "");
                                });
                                addArtefacts(parsed);
                                registerParsedState(definition);
                            } catch (ParseException e) {
                                registerBrokenState(definition, e.getMessage());
                            }
                            break;
                        case PARSED: // not new nor modified
                            parsed = synchronizer.retrieve(definition.getLocation());

                            Iterator<? extends Artefact> iterator = parsed.iterator();
                            while (iterator.hasNext()) {
                                Artefact a = iterator.next();
                                if (ArtefactLifecycle.FATAL.equals(a.getLifecycle())) {
                                    iterator.remove();
                                }
                            }
                            addArtefacts(parsed);
                            break;
                        case BROKEN: // has been broken
                            // do not try to parse it again as it is still not modified
                            break;
                        case DELETED: // has been deleted before, but appeared again
                            try {
                                parsed = synchronizer.parse(definition.getLocation(), definition.getContent());
                                parsed.forEach(a -> {
                                    a.setPhase(ArtefactPhase.CREATE);
                                    synchronizer.setStatus((a), ArtefactLifecycle.NEW, "");
                                });
                                addArtefacts(parsed);
                                registerParsedState(definition);
                            } catch (ParseException e) {
                                registerBrokenState(definition, e);
                            }
                            break;
                    }
                } catch (Exception e) {
                    registerBrokenState(definition, e);
                }
            }
        }
    }

    /**
     * Register parsed state.
     *
     * @param definition the definition
     */
    public void registerParsedState(Definition definition) {
        if (!definition.getState()
                       .equals(DefinitionState.PARSED)) {
            definition.setState(DefinitionState.PARSED);
            definition.setMessage("");
            definitionService.save(definition);
        }
    }

    /**
     * Register broken state.
     *
     * @param definition the definition
     * @param errorMessage the error message
     */
    public void registerBrokenState(Definition definition, String errorMessage) {
        registerBrokenState(definition, null, errorMessage);
    }

    /**
     * Register broken state.
     *
     * @param definition the definition
     * @param e the e
     */
    public void registerBrokenState(Definition definition, Exception e) {
        registerBrokenState(definition, e, e.getMessage());
    }

    /**
     * Register broken state.
     *
     * @param definition the definition
     * @param ex the ex
     * @param errorMessage the error message
     */
    private void registerBrokenState(Definition definition, Exception ex, String errorMessage) {
        if (null != ex) {
            logger.error(errorMessage, ex);
        } else {
            logger.error(errorMessage);
        }
        addError(errorMessage);
        definition.setState(DefinitionState.BROKEN);
        definition.setMessage(errorMessage);
        definitionService.save(definition);
    }

    /**
     * Register delete state.
     *
     * @param definition the definition
     */
    public void registerDeleteState(Definition definition) {
        String msg = "Definition deleted: " + definition.getLocation();
        logger.warn(msg);
        definition.setState(DefinitionState.DELETED);
        definition.setMessage(msg);
        definitionService.save(definition);
    }

    /**
     * Adds the artefacts.
     *
     * @param <T> the generic type
     * @param parsed the parsed
     */
    private <T extends Artefact> void addArtefacts(List<T> parsed) {
        for (T artefact : parsed) {
            artefacts.put(artefact.getKey(), artefact);
        }
    }

    /**
     * Visit file.
     *
     * @param file the file
     * @param attrs the attrs
     * @param location the location
     */
    @Override
    public void visitFile(Path file, BasicFileAttributes attrs, String location) {
        checkFile(file, attrs, location);
    }

    /**
     * Check file.
     *
     * @param file the file
     * @param attrs the attrs
     * @param location the location
     */
    private void checkFile(Path file, BasicFileAttributes attrs, String location) {
        for (Synchronizer<? extends Artefact, ?> synchronizer : synchronizers) {
            if (synchronizer.isAccepted(file, attrs)) {
                // synchronizer knows this artifact, hence check whether to process it or not
                try {
                    checkAndCollect(file, location, synchronizer);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                break;
            }
        }
    }

    /**
     * Collect for processing, if new or modified.
     *
     * @param file the file
     * @param location the location
     * @param synchronizer the synchronizer
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileNotFoundException the file not found exception
     */
    private void checkAndCollect(Path file, String location, Synchronizer<? extends Artefact, ?> synchronizer)
            throws IOException, FileNotFoundException {

        String type = synchronizer.getArtefactType();

        // load the content to calculate the checksum
        byte[] content = Files.readAllBytes(file);
        if (content == null) {
            logger.error("Reading file {} returns null content", file.toString());
            return;
        }
        Definition definition = new Definition(location, FilenameUtils.getBaseName(file.getFileName()
                                                                                       .toString()),
                type, content);
        // check whether this artefact has been processed in the past already
        Definition maybe = definitionService.findByKey(definition.getKey());
        Map<String, Definition> map = checkSynchronizerMap(synchronizer);
        if (maybe != null) {
            // artefact has been processed in the past
            if (!maybe.getChecksum()
                      .equals(definition.getChecksum())) {
                // the content has been modified since the last processing
                maybe.setChecksum(definition.getChecksum());
                maybe.setState(DefinitionState.MODIFIED);
                maybe.setContent(definition.getContent());
                // update the artefact with the new checksum and status
                definitionService.save(maybe);
                // added to artefacts for processing
                map.put(maybe.getKey(), maybe);
            } else {
                // not modified content, but still known definition
                switch (maybe.getState()) {
                    case NEW: // has been started to be processed as new, but not completed
                    case MODIFIED: // has been started to be processed as modified, but not completed
                    case PARSED: // has been successfully parsed in the past
                        if (map.get(maybe.getKey()) == null) {
                            maybe.setContent(definition.getContent());
                            map.put(maybe.getKey(), maybe);
                        }
                        break;
                    case BROKEN: // has been started in the past, but failed to parse the file
                        logger.warn("Definition with key: {} has been failed with reason: {}", maybe.getKey(), maybe.getMessage());
                        if (map.get(maybe.getKey()) == null) {
                            maybe.setContent(definition.getContent());
                            map.put(maybe.getKey(), maybe);
                        }
                        break;
                    case DELETED: // has been deleted in the past
                        if (map.get(maybe.getKey()) == null) {
                            maybe.setContent(definition.getContent());
                            maybe.setState(DefinitionState.NEW);
                            map.put(maybe.getKey(), maybe);
                        }
                        break;
                }
            }
        } else {
            // artefact is new, hence stored for processing
            definition.setState(DefinitionState.NEW);
            definitionService.save(definition);
            map.put(definition.getKey(), definition);
        }
    }

    /**
     * Check synchronizer map.
     *
     * @param synchronizer the synchronizer
     * @return the map
     */
    public Map<String, Definition> checkSynchronizerMap(Synchronizer synchronizer) {
        Map<String, Definition> map = definitions.get(synchronizer);
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap<>());
            definitions.put(synchronizer, map);
        }
        return map;
    }

    /**
     * Adds the error.
     *
     * @param error the error
     */
    @Override
    public void addError(String error) {
        this.errors.add(error);
    }

    /**
     * Gets the errors.
     *
     * @return the errors
     */
    @Override
    public Set<String> getErrors() {
        return errors;
    }

    /**
     * Register errors.
     *
     * @param remained the remained
     * @param lifecycle the lifecycle
     */
    @Override
    public void registerErrors(Set<TopologyWrapper<? extends Artefact>> remained, ArtefactLifecycle lifecycle) {
        if (remained.size() > 0) {
            for (TopologyWrapper<? extends Artefact> wrapper : remained) {
                Artefact artefact = wrapper.getArtefact();
                String errorMessage = String.format(
                        "Undepleted artefact in lifecycle [%s] and phase [%s] with key [%s], location [%s], type [%s], error [%s]",
                        lifecycle, artefact.getPhase(), artefact.getKey(), artefact.getLocation(), artefact.getType(), artefact.getError());
                errors.add(errorMessage);

                Synchronizer synchronizer = wrapper.getSynchronizer();
                synchronizer.setStatus(artefact, ArtefactLifecycle.FAILED, errorMessage);
                Problem problem = ProblemsFacade.getArtefactSynchronizationProblem(artefact);
                if (problem != null) {
                    ProblemsFacade.updateArtefactSynchronizationProblem(artefact, errorMessage);
                } else {
                    ProblemsFacade.saveArtefactSynchronizationProblem(artefact, errorMessage);
                }
            }
        }
    }

    /**
     * Register fatal errors.
     *
     * @param remained the remained
     */
    @Override
    public void registerFatals(Set<TopologyWrapper<? extends Artefact>> remained) {
        if (remained.size() > 0) {
            for (TopologyWrapper<? extends Artefact> wrapper : remained) {
                Artefact artefact = wrapper.getArtefact();
                String errorMessage = String.format(
                        "Fatal undepleted artefact in phase [%s] with key [%s], location [%s], type [%s], error [%s]",
                        ArtefactLifecycle.FATAL, artefact.getKey(), artefact.getLocation(), artefact.getType(), artefact.getError());
                logger.error(errorMessage);
                errors.add(errorMessage);
                Synchronizer synchronizer = wrapper.getSynchronizer();
                synchronizer.setStatus(artefact, ArtefactLifecycle.FATAL, errorMessage);
            }
        }
    }

    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle) {
        registerState(synchronizer, wrapper.getArtefact(), lifecycle);
    }

    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle) {
        this.registerState(synchronizer, artefact, lifecycle, "");
    }

    /**
     * Register state.
     *
     * @param synchronizer the synchronizer
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param message the message
     */
    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle,
            String message) {
        this.registerState(synchronizer, artefact, lifecycle, message, null);
    }

    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle,
            String message, Throwable cause) {
        switch (lifecycle) {

            case FAILED:
            case FATAL:
                logger.error(
                        "Processing of artefact with key [{}], location [{}] for lifecycle [{}] has failed, synchronizer [{}], error [{}], message [{}]",
                        artefact.getKey(), artefact.getLocation(), lifecycle, synchronizer, artefact.getError(), message, cause);
                break;
            default: {
                logger.debug(
                        "Registering artefact lifecycle status [{}] for artefact with key [{}], location [{}], message [{}], error [{}]",
                        lifecycle, artefact.getKey(), artefact.getLocation(), message, artefact.getError(), cause);
            }
        }
        Synchronizer s = synchronizer;
        s.setStatus(artefact, lifecycle, message);
    }

    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, Throwable cause) {
        registerState(synchronizer, wrapper.getArtefact(), lifecycle, cause);
    }

    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, Artefact artefact, ArtefactLifecycle lifecycle,
            Throwable cause) {
        this.registerState(synchronizer, artefact, lifecycle, getMessage(cause), cause);
    }

    private String getMessage(Throwable cause) {
        return null == cause ? "" : cause.getMessage();
    }

    /**
     * Register state.
     *
     * @param synchronizer the synchronizer
     * @param wrapper the wrapper
     * @param lifecycle the lifecycle
     * @param message the message
     */
    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, String message) {
        registerState(synchronizer, wrapper.getArtefact(), lifecycle, message);
    }

    @Override
    public void registerState(Synchronizer<? extends Artefact, ?> synchronizer, TopologyWrapper<? extends Artefact> wrapper,
            ArtefactLifecycle lifecycle, String message, Throwable cause) {
        registerState(synchronizer, wrapper.getArtefact(), lifecycle, message, cause);
    }

    @Override
    public void destroy() {
        logger.info("Destroying [{}}", this);

        synchronizers.clear();
        errors.clear();
        artefacts.clear();
        definitions.clear();

        this.initialized.set(false);
        this.prepared.set(false);
        this.processing.set(false);
    }
}

