angular.module('editNg.resources')
.factory('EventAttachmentDetailsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/asset/attachment/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);
