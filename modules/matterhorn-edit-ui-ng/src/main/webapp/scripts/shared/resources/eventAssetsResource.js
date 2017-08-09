angular.module('editNg.resources')
.factory('EventAssetsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id/asset/assets.json', { id: '@id' }, {
        get: { 
            method: 'GET', 
            isArray: false
        }
    });
}]);
