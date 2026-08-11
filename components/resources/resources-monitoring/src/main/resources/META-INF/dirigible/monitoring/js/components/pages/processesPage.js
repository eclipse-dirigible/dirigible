/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Processes page: the instance list on the left, the selected instance's detail on the right.
 * The store owns the data; this component owns the page's own concerns - which detail tab is open
 * and the BPMN diagram, which is imperative (a library rendering into a container element) and so
 * cannot live in a store.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('processesPage', () => ({
    tab: 'diagram',
    diagramError: null,

    init() {
      this.$store.processes.load();
      // Re-render the diagram whenever another instance is selected (or the selection is cleared).
      this.$watch('$store.processes.selectedId', () => {
        this.tab = 'diagram';
        this.$nextTick(() => this.renderDiagram());
      });
    },

    destroy() {
      window.MonitoringProcessDiagram.dispose();
    },

    get state() {
      return this.$store.processes;
    },

    selectTab(tab) {
      this.tab = tab;
      // The diagram container only exists while its tab is open, so the diagram is re-rendered
      // rather than kept hidden - bpmn-visualization measures the container when it loads.
      if (tab === 'diagram') this.$nextTick(() => this.renderDiagram());
    },

    async renderDiagram() {
      const container = this.$refs.diagram;
      const instance = this.state.selected;
      window.MonitoringProcessDiagram.dispose();
      this.diagramError = null;
      if (!container || !instance || !instance.processDefinitionId) return;
      container.innerHTML = '';
      try {
        await window.MonitoringProcessDiagram.render(container, instance.processDefinitionId, this.state.activities);
      } catch (e) {
        console.error('monitoring: could not render the process diagram', e);
        this.diagramError = T('monitoring:processes.diagramFailed', 'The diagram could not be rendered.');
      }
    },

    zoomIn() { window.MonitoringProcessDiagram.zoomIn(); },
    zoomOut() { window.MonitoringProcessDiagram.zoomOut(); },
    fit() { window.MonitoringProcessDiagram.fit(); },

    /** The instance's title: the definition's name, falling back to its key. */
    titleOf(instance) {
      return instance.processDefinitionName || instance.processDefinitionKey || instance.id;
    },

    startedText(instance) {
      const at = instance && (instance.startTime || instance.startedAt);
      if (!at) return '';
      const date = new Date(at);
      return isNaN(date.getTime()) ? '' : date.toLocaleString();
    },

    /** A variable's declared type - historic and runtime variables name it differently. */
    typeOf(variable) {
      return variable.typeName || variable.variableTypeName || '';
    },

    /** Variable values are arbitrary objects; render them as something a table cell can hold. */
    valueText(value) {
      if (value === null || value === undefined) return '';
      const text = typeof value === 'object' ? JSON.stringify(value) : String(value);
      return text.length > 200 ? text.slice(0, 200) + '…' : text;
    },

    shorten(text, limit = 200) {
      const single = (text || '').replace(/\s+/g, ' ').trim();
      return single.length > limit ? single.slice(0, limit) + '…' : single;
    },
  }));
});
