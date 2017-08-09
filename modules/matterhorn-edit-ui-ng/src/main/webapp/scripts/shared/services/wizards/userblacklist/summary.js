angular.module('editNg.services')
.factory('NewUserblacklistSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);
