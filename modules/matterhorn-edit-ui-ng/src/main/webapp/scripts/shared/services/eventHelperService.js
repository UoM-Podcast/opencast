angular.module('editNg.services')
.factory('EventHelperService', function () {
    var EventHelperService = function () {
        var me = this,
            eventId;

        this.reset = function () {
          eventId = undefined;
        }

    };

    return new EventHelperService();
});
