angular.module('editNg.resources')
.factory('EventCatalogDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/asset/catalog/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
