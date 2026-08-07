/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The live BPMN diagram of a process instance, rendered with the SAME visualizer the IDE's process
 * viewer uses (the bpmn-visualization webjar) - a vector diagram that follows the theme, rather than
 * the server-rendered PNG, which is static and theme-blind.
 *
 * Framework-free on purpose: the page component owns the container element and calls render/dispose.
 * The library is ~1 MB, so it is loaded on first use - the Overview page must not pay for it.
 */
window.MonitoringProcessDiagram = (() => {
  const LIBRARY_URL = '/webjars/bpmn-visualization/dist/bpmn-visualization.min.js';

  let libraryPromise = null;

  const loadLibrary = () => {
    if (!libraryPromise) {
      libraryPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = LIBRARY_URL;
        script.onload = resolve;
        script.onerror = () => reject(new Error('Failed to load ' + LIBRARY_URL));
        document.head.appendChild(script);
      });
    }
    return libraryPromise;
  };

  /**
   * Theme the graph through the CSS variables Harmonia already defines, so the diagram follows the
   * light/dark switch without a second palette. Mirrors the IDE viewer's styling.
   */
  const applyTheme = (visualization) => {
    const constants = bpmnvisu.mxgraph.mxConstants;
    const vertex = visualization.graph.getStylesheet()
                                      .getDefaultVertexStyle();
    const edge = visualization.graph.getStylesheet()
                                    .getDefaultEdgeStyle();

    edge[constants.STYLE_FONTCOLOR] = 'var(--foreground)';
    edge[constants.STYLE_LABEL_COLOR] = 'var(--foreground)';
    edge[constants.STYLE_STROKECOLOR] = 'var(--foreground)';
    edge[constants.STYLE_STROKEWIDTH] = 2;
    edge[constants.STYLE_ROUNDED] = true;

    vertex[constants.STYLE_FILLCOLOR] = 'var(--input-background)';
    vertex[constants.STYLE_FONTCOLOR] = 'var(--foreground)';
    vertex[constants.STYLE_STROKECOLOR] = 'var(--foreground)';
    vertex[constants.STYLE_FONTSIZE] = '12';
    vertex[constants.STYLE_ARCSIZE] = '12';
    vertex[constants.STYLE_ROUNDED] = true;
  };

  /**
   * Flowable's mail service tasks carry a vendor attribute the visualizer does not know, and
   * collapsed sub-processes render as an opaque box. Both are normalised before loading - the same
   * preparation the IDE viewer does.
   */
  const prepareXml = (raw) => {
    const bpmn = new DOMParser().parseFromString(raw, 'application/xml');
    bpmn.querySelectorAll('subProcess')
        .forEach((subProcess) => {
          const shape = bpmn.querySelector('[bpmnElement="' + subProcess.id + '"]');
          if (shape) shape.setAttribute('isExpanded', 'true');
        });
    Array.from(bpmn.getElementsByTagName('serviceTask'))
         .filter((task) => task.getAttribute('flowable:type') === 'mail')
         .forEach((task) => {
           const receiveTask = bpmn.createElementNS(task.namespaceURI, 'receiveTask');
           for (const attribute of task.attributes) {
             if (attribute.name !== 'flowable:type') receiveTask.setAttribute(attribute.name, attribute.value);
           }
           while (task.firstChild) receiveTask.appendChild(task.firstChild);
           task.parentNode.replaceChild(receiveTask, task);
         });
    return new XMLSerializer().serializeToString(bpmn);
  };

  /** The definition XML is served as text/xml, so it is fetched directly rather than through the
   *  JSON fetch client (which would ask for application/json and be refused). */
  const fetchDefinition = async (processDefinitionId) => {
    const response = await fetch(
      '/services/bpm/bpm-processes/definition/bpmn?id=' + encodeURIComponent(processDefinitionId),
      { credentials: 'same-origin', headers: { 'Accept': 'text/xml', 'X-Requested-With': 'XMLHttpRequest' } });
    if (!response.ok) throw new Error('definition ' + processDefinitionId + ' -> HTTP ' + response.status);
    return response.text();
  };

  /** One badge per activity: how many tokens passed (positive) and how many failed there (negative). */
  const badgesFor = (counters) => {
    const badges = [];
    if (counters.negative) {
      badges.push({
        position: 'bottom-right',
        label: String(counters.negative),
        style: { font: { color: 'var(--negative-foreground)', size: 14 }, fill: { color: 'var(--negative)' },
          stroke: { color: 'var(--negative)' } },
      });
    }
    if (counters.positive) {
      badges.push({
        position: 'bottom-left',
        label: String(counters.positive),
        style: { font: { color: 'var(--positive-foreground)', size: 14 }, fill: { color: 'var(--positive)' },
          stroke: { color: 'var(--positive)' } },
      });
    }
    return badges;
  };

  let visualization = null;
  let badgedActivities = [];

  return {
    /**
     * Render one instance's definition into the container and overlay its activity badges.
     *
     * @param {HTMLElement} container the element to render into (must be in the document)
     * @param {string} processDefinitionId the definition to draw
     * @param {object} activities per-activity { positive, negative } counters, may be empty
     */
    async render(container, processDefinitionId, activities) {
      await loadLibrary();
      // The container is re-created on every selection (the detail pane is rebuilt), so is the
      // visualization - bpmn-visualization binds to the element it was constructed with.
      visualization = new bpmnvisu.BpmnVisualization({ container, navigation: { enabled: true } });
      badgedActivities = [];
      applyTheme(visualization);
      visualization.load(prepareXml(await fetchDefinition(processDefinitionId)),
        { fit: { type: bpmnvisu.FitType.Center, margin: 16 } });
      for (const [activityId, counters] of Object.entries(activities || {})) {
        badgedActivities.push(activityId);
        visualization.bpmnElementsRegistry.addOverlays(activityId, badgesFor(counters));
      }
    },

    /** Drop the current diagram; safe to call when nothing is rendered. */
    dispose() {
      if (!visualization) return;
      badgedActivities.forEach((activityId) => visualization.bpmnElementsRegistry.removeAllOverlays(activityId));
      badgedActivities = [];
      visualization = null;
    },

    zoomIn() { if (visualization) visualization.navigation.graph.zoomIn(); },
    zoomOut() { if (visualization) visualization.navigation.graph.zoomOut(); },
    fit() { if (visualization) visualization.navigation.fit({ type: bpmnvisu.FitType.Center, margin: 16 }); },
  };
})();
