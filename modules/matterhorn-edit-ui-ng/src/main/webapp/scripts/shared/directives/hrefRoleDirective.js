/**
 * @ngdoc directive
 * @name editNg.directives.editNgHrefRole
 * @description
 * Overrides the 'href' attribute of an element depending on the user's roles.
 *
 * @example
 * <a edit-ng-href-role="{'STUDENT': '/student', 'TEACHER': '/teacher'}" href="/general" />
 */
angular.module('editNg.directives')
.directive('editNgHrefRole', ['AuthService', function (AuthService) {
    return {
        scope: {
            rolesHref: '@editNgHrefRole'
        },
        link: function ($scope, element) {
            var href;
            angular.forEach($scope.$eval($scope.rolesHref), function(value, key) {
                if (AuthService.userIsAuthorizedAs(key) && angular.isUndefined(href)) {
                    href = value;
                }
            });

            if (angular.isDefined(href)) {
                element.attr('href', href);
            }
        }
    };
}]);
