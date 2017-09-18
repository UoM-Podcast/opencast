angular.module('editNg.resources')
.factory('EventWorkflowDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/workflows/:id1.json');
}]);
