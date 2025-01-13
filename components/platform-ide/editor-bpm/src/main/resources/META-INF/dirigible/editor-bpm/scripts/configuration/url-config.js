/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var FLOWABLE = FLOWABLE || {};

/*
 * Contains methods to retrieve the (mostly) base urls of the different end points.
 * Two of the methods #getImageUrl and #getModelThumbnailUrl are exposed in the $rootScope for usage in the HTML views.
 */
FLOWABLE.APP_URL = {

    /* ACCOUNT URLS */

//    getAccountUrl: function () {
//        return FLOWABLE.CONFIG.contextRoot + '/account';
//    },
//
//    getLogoutUrl: function () {
//        return FLOWABLE.CONFIG.contextRoot + '/logout';
//    },

    /* MODEL URLS */

    getModelsUrl: function (query = null) {
        return FLOWABLE.CONFIG.contextRoot + '/models' + (query || "");
    },

    getModelUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId;
    },

    getModelModelJsonUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/model-json';
    },

    getModelBpmn20ExportUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/bpmn20?version=' + Date.now();
    },

    getCloneModelsUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/clone';
    },

    getModelHistoriesUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/history';
    },

    getModelHistoryUrl: function (modelId, modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/history/' + modelHistoryId;
    },

    getModelHistoryModelJsonUrl: function (modelId, modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/history/' + modelHistoryId + '/model-json';
    },

    getModelHistoryBpmn20ExportUrl: function (modelId, modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/history/' + modelHistoryId + '/bpmn20?version=' + Date.now();
    },

    getCmmnModelDownloadUrl: function (modelId, modelHistoryId = null) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + (modelHistoryId ? '/history/' + modelHistoryId : '') + '/cmmn?version=' + Date.now();
    },

    getModelParentRelationsUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/parent-relations';
    },

    /* APP DEFINITION URLS  */

    getAppDefinitionImportUrl: function (renewIdmIds) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/import?renewIdmEntries=' + renewIdmIds;
    },

    getAppDefinitionTextImportUrl: function (renewIdmIds) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/text/import?renewIdmEntries=' + renewIdmIds;
    },

    getAppDefinitionUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId;
    },

    getAppDefinitionModelImportUrl: function (modelId, renewIdmIds) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/import?renewIdmEntries=' + renewIdmIds;
    },

    getAppDefinitionModelTextImportUrl: function (modelId, renewIdmIds) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/text/import?renewIdmEntries=' + renewIdmIds;
    },

    getAppDefinitionPublishUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/publish';
    },

    getAppDefinitionExportUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/export?version=' + Date.now();
    },

    getAppDefinitionBarExportUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/export-bar?version=' + Date.now();
    },

    getAppDefinitionHistoryUrl: function (modelId, historyModelId) {
        return FLOWABLE.CONFIG.contextRoot + '/app-definitions/' + modelId + '/history/' + historyModelId;
    },

    getModelsForAppDefinitionUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/models-for-app-definition';
    },

    getCmmnModelsForAppDefinitionUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/cmmn-models-for-app-definition';
    },

    /* PROCESS INSTANCE URLS */

    getProcessInstanceModelJsonUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/process-instances/' + modelId + '/model-json';
    },

    getProcessInstanceModelJsonHistoryUrl: function (historyModelId) {
        return FLOWABLE.CONFIG.contextRoot + '/process-instances/history/' + historyModelId + '/model-json';
    },

    /* PROCESS DEFINITION URLS */

    getProcessDefinitionModelJsonUrl: function (processDefinitionId) {
        return FLOWABLE.CONFIG.contextRoot + '/process-definitions/' + processDefinitionId + '/model-json';
    },

    /* PROCESS MODEL URLS */

    getImportProcessModelUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/import-process-model';
    },

    getImportProcessModelTextUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/import-process-model/text';
    },

    /* DECISION TABLE URLS */

    getDecisionTableModelsUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models';
    },

    getDecisionTableImportUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/import-decision-table';
    },

    getDecisionTableTextImportUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/import-decision-table-text';
    },

    getDecisionTableModelUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/' + modelId;
    },

    getDecisionTableModelValuesUrl: function (query) {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/values?' + query;
    },

    getDecisionTableModelsHistoryUrl: function (modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/history/' + modelHistoryId;
    },

    getDecisionTableModelHistoryUrl: function (modelId, modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/decision-table-models/' + modelId + '/history/' + modelHistoryId;
    },

    /* FORM MODEL URLS */

    getFormModelsUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/form-models';
    },

    getFormModelValuesUrl: function (query) {
        return FLOWABLE.CONFIG.contextRoot + '/form-models/values?' + query;
    },

    getFormModelUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/form-models/' + modelId;
    },

    getFormModelHistoryUrl: function (modelId, modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/form-models/' + modelId + '/history/' + modelHistoryId;
    },

    /* CASE MODEL URLS */

    getCaseModelsUrl: function (query = null) {
        return FLOWABLE.CONFIG.contextRoot + '/case-models' + (query || "");
    },

    getCaseModelImportUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/import-case-model';
    },

    getCaseModelTextImportUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/import-case-model/text';
    },

    getCaseInstancesHistoryModelJsonUrl: function (modelHistoryId) {
        return FLOWABLE.CONFIG.contextRoot + '/case-instances/history/' + modelHistoryId + '/model-json';
    },

    getCaseInstancesModelJsonUrl: function (modelId) {
        return FLOWABLE.CONFIG.contextRoot + '/case-instances/' + modelId + '/model-json';
    },

    getCaseDefinitionModelJsonUrl: function (caseDefinitionId) {
        return FLOWABLE.CONFIG.contextRoot + '/case-definitions/' + caseDefinitionId + '/model-json';
    },

    /* IMAGE URLS (exposed in rootscope in app.js */

    getImageUrl: function (imageId) {
        return FLOWABLE.CONFIG.contextRoot + '/image/' + imageId;
    },

    getModelThumbnailUrl: function (modelId, version = null) {
        return FLOWABLE.CONFIG.contextRoot + '/models/' + modelId + '/thumbnail' + (version ? "?version=" + version : "");
    },

    /* OTHER URLS */

    getEditorUsersUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/editor-users';
    },

    getEditorGroupsUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/editor-groups';
    },

    getAboutInfoUrl: function () {
        return FLOWABLE.CONFIG.contextRoot + '/about-info';
    }

};
