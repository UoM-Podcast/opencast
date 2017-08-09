angular.module('editNg.resources')
.factory('TaskResource', ['$resource', function ($resource) {
    return $resource('/edit-ng/tasks/new', {}, {
        save: {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            transformRequest: function (data) {
                data.workflow = data.workflows;
                return $.param({
                    metadata: JSON.stringify(data)
                });
            }
        }
    });
}]);
