describe('editNg.directives.editNgNotification', function () {
    var $compile, $rootScope, element;

    beforeEach(module('editNg'));
    beforeEach(module('shared/partials/notification.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
    }));

    beforeEach(function () {
        element = $compile('<div edit-ng-notification=""></div>')($rootScope);
        $rootScope.$digest();
    });

    it('creates a notification container', function () {
        expect(element).toHaveClass('alert');
    });
});
