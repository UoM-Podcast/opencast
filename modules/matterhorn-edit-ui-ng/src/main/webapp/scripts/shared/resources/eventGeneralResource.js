angular.module('editNg.resources')
.factory('EventGeneralResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id/general.json', { id: '@id' });
}]);
