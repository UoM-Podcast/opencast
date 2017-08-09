angular.module('editNg.resources')
.factory('EventAttachmentsResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/event/:id0/asset/attachment/attachments.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'}}
    });
}]);
