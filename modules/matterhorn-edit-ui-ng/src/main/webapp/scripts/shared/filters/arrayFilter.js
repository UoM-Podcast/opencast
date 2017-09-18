angular.module('editNg.filters')
.filter('joinBy', [function () {
    return function (input, delimiter) {
        return (input || []).join(delimiter || ',');
    };
}]);
