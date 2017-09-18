angular.module('editNg.resources')
.factory('OptoutSingleResource', ['$resource', function ($resource) {

    return $resource('/edit-ng/:resource/:id/optout/:optout', {resource: '@resource', id: '@id', optout: '@optout'}, {
        save: {
            method: 'PUT'
        }
    });
}]);

