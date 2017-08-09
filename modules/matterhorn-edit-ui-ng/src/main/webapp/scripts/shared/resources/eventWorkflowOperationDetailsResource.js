angular.module('editNg.resources')
.factory('EventWorkflowOperationDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/workflows/:id1/operations/:id2');
}]);
