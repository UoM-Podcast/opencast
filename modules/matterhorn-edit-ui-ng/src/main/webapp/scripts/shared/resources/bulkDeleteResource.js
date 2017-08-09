angular.module('editNg.resources')
.factory('BulkDeleteResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/:resource/:endpoint', {resource: '@resource', endpoint: '@endpoint'}, {
        delete: {
            method: 'POST',
            transformRequest: function (data) {
                return '["' + data.eventIds.join('","') + '"]';
            }
        }
    });
}]);
