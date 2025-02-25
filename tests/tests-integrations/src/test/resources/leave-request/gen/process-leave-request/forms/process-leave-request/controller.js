const formView = angular.module('forms', ['ideUI', 'ideView']);

formView.controller('FormController', ['$scope', '$http', function ($scope, $http) {

    $scope.forms = {
        form: {}
    };

    $scope.model = {};

    let url = new URL(window.location);
    let params = new URLSearchParams(url.search);
    let taskId = params.get("taskId");
    
    $scope.onApproveClicked = function () {
        const url = `/services/ts/leave-request/api/ProcessService.ts/requests/${taskId}/approve`;
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
        const url = `/services/ts/leave-request/api/ProcessService.ts/requests/${taskId}/decline`;
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
    
    const detailsUrl = `/services/ts/leave-request/api/ProcessService.ts/requests/${taskId}/details`;
    $http.get(detailsUrl)
        .then(function (response) {
            if (response.status != 200) {
                alert(`Unable to get details for the request: '${response.message}'`);
                return;
            }
            const details = response.data;
    
            // fill details
            $scope.model.requester = details.requester;
            $scope.model.fromDate = new Date(details.fromDate);
            $scope.model.toDate = new Date(details.toDate);
        });

}]);