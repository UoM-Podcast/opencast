/**
 * @ngdoc directive
 * @name editNg.modal.editNgPwCheck
 * @description
 * Checks a password for correct repetition.
 *
 * Usage: Set this directive in the input element of the password.
 *
 * @example
 * <input edit-ng-pw-check="model.repeatedPassword"/>
 */
angular.module('editNg.directives')
.directive('editNgPwCheck', function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attrs, ctrl) {
            scope.deregisterWatch = scope.$watch(attrs.editNgPwCheck, function (confirmPassword) {
                var isValid = ctrl.$viewValue === confirmPassword;
                ctrl.$setValidity('pwmatch', isValid);
            });

            scope.$on('$destroy', function () {
                scope.deregisterWatch();
            });
        }
    };
});
