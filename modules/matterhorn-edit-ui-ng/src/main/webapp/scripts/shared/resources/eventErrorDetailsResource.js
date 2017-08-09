angular.module('editNg.resources')
.factory('EventErrorDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/workflows/:id1/errors/:id2.json');
}]);

