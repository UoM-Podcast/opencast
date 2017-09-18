describe('Event Catalogs API Resource', function () {
    var $httpBackend, EventPublicationsResource;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventPublicationsResource_) {
        $httpBackend  = _$httpBackend_;
        EventPublicationsResource = _EventPublicationsResource_;
    }));

    describe('#get', function () {
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
            $httpBackend.whenGET('/edit-ng/event/30112/asset/publication/publications.json')
            .respond(JSON.stringify(getJSONFixture('edit-ng/event/30112/asset/publication/publications.json')));
        });

        it('queries the group API', function () {
            $httpBackend.expectGET('/edit-ng/event/30112/asset/publication/publications.json')
            .respond(getJSONFixture('edit-ng/event/30112/asset/publication/publications.json'));
            EventPublicationsResource.get({ id0: '30112'});
            $httpBackend.flush();
        });

        it('returns the parsed JSON', function () {
            $httpBackend.expectGET('/edit-ng/event/30112/asset/publication/publications.json')
            .respond(getJSONFixture('edit-ng/event/30112/asset/publication/publications.json'));
            var data = EventPublicationsResource.get({ id0: '30112' });
            $httpBackend.flush();
            expect(data).toBeDefined();
            expect(data.length).toEqual(1);
        });
    });
});
