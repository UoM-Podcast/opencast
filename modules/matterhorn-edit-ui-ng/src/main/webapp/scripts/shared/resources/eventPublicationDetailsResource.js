angular.module('editNg.resources')
.factory('EventPublicationDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/asset/publication/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
