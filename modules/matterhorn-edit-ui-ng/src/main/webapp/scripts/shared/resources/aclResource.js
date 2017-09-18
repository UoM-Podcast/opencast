angular.module('editNg.resources')
.factory('AclResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/acl/:id', { id: '@id' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        save: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              return $.param({
                  name : data.name,
                  acl  : JSON.stringify({acl: data.acl})
              });
        }
      }
    });
}]);
