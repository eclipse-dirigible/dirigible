angular.module('forms', ['blimpKit', 'platformView']).controller('FormController', ($scope, $http, ViewParameters) => {
    $scope.forms = {
        form: {}
    };

    $scope.model = {};

    let url = new URL(window.location);
    let params = new URLSearchParams(url.search);
    let taskId = params.get("taskId");
    
    $scope.onApproveClicked = function () {
        const url = `/services/ts/BpmnMultitenancyIT/ProcessService.ts/requests/${taskId}/approve`;
        $http.put(url)
            .then(function (response) {
            if (response.status != 200) {
                alert(`Unable to approve request: '${response.message}'`);
                return;
            }
            $scope.entity = {};
            alert("Request Approved");
        });
    };
    
    $scope.onDeclineClicked = function () {
        const url = `/services/ts/BpmnMultitenancyIT/ProcessService.ts/requests/${taskId}/decline`;
        $http.put(url)
            .then(function (response) {
            if (response.status != 200) {
                alert(`Unable to decline request: '${response.message}'`);
                return;
            }
            $scope.entity = {};
            alert("Request Declined");
        });
    
    };
    
    

});