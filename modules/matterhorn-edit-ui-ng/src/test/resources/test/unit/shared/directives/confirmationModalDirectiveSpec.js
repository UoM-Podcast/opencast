describe('editNg.directives.confirmationModal', function () {
    var $compile, $rootScope, $timeout, ConfirmationModal, element;

    beforeEach(module('editNg.services'));
    beforeEach(module('editNg.services.modal'));
    beforeEach(module('editNg.directives'));
    beforeEach(module('shared/partials/modals/confirm-modal.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Table', {});
    }));

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_, _ConfirmationModal_) {
        $compile = _$compile_;
        $timeout = _$timeout_;
        $rootScope = _$rootScope_;
        ConfirmationModal = _ConfirmationModal_;
    }));

    beforeEach(function () {
        $rootScope.delete = jasmine.createSpy();
        element = $compile('<a data-confirmation-modal="confirm-modal" data-callback="delete" data-object="41"></a>')($rootScope);
        $rootScope.$digest();
    });

    it('creates a confirmation modal with the specified parameters when clicked', function () {
        spyOn(ConfirmationModal, 'show');
        element.click();
        expect(ConfirmationModal.show).toHaveBeenCalledWith('confirm-modal', $rootScope.delete, 41);
    });
});
