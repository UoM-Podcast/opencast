/**
* A service to store arbitrary information without use of a backend.
*
* Information can either be stored in the localStorage or as a search
* parameter. Search parameters take precedence over localStorage
* values.
*
*/
angular.module('editNg.services')
.factory('CookiesService', ['$rootScope', '$cookies', function ($rootScope, $cookies) {
    var CookiesService = function () {

      // Create a scope in order to broadcast changes.
        this.scope = $rootScope.$new();

        this.getCookie = function (key) {
            return $cookies[key]
        };
    };
    return new CookiesService();
}]);
