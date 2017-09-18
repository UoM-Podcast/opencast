angular.module('editNg.resources')
.factory('EventPublicationsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/asset/publication/publications.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id' } }
    });
}]);
