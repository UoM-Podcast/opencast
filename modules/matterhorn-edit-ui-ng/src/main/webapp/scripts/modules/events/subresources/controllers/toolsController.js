/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
'use strict';

// Controller for all event screens.
angular.module('editNg.controllers')
        .controller('ToolsCtrl', ['$scope', '$interval', '$route', '$location', '$window', 'ToolsResource', 'Notifications', 'EventHelperService',
          function ($scope, $interval, $route, $location, $window, ToolsResource, Notifications, EventHelperService) {

            $scope.navigateTo = function (path) {
              // FIMXE When changing tabs, video playback breaks. Using playback
              // controls after a tab change works for audio, but there is no
              // video. Perhaps it results in an orphaned <video> element.
              //
              // The following hack prevents a racing condition between setting
              // the path and a reload by preventing the path change from
              // triggering a render sync before the reload takes place.
              ToolsResource.release({id: $scope.id, tool: 'lock'});
              var lastRoute, off;
              lastRoute = $route.current;
              off = $scope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                off();
                $window.location.reload();
              });
              $location.path(path).replace();
            };

            $scope.event = EventHelperService;
            $scope.resource = $route.current.params.resource;
            $scope.tab = $route.current.params.tab;
            if ($scope.tab === "editor") {
              $scope.area = "segments";
            } else {
              $scope.area = "metadata";
            }
            $scope.id = $route.current.params.itemId;

            $scope.event.eventId = $scope.id;

            $scope.openTab = function (tab) {
              $scope.navigateTo('events/' + $scope.resource + '/' +
                      $scope.id + '/tools/' + tab);
            };

            $scope.openArea = function (area) {
              $scope.area = area;
            };

            // TODO Move the following to a VideoCtrl
            $scope.player = {};
            $scope.video = ToolsResource.get({id: $scope.id, tool: 'editor'});

            $scope.autosave = function () {
              $scope.video.autosave = true;
              $scope.submitButton = true;
              $scope.video.$save({id: $scope.id, tool: $scope.tab}, function () {
                $scope.submitButton = false;
                Notifications.add('success', 'VIDEO_CUT_SAVED_AUTO', 'video-tools');
              }, function () {
                $scope.submitButton = false;
              });
            };
            var autosaveDelay = 1740000; // 29 min
            $scope.autosaveStop = $interval($scope.autosave, autosaveDelay);

            $scope.submitButton = false;
            $scope.save = function () {
              $scope.video.autosave = false;
              $interval.cancel($scope.autosaveStop);
              $scope.submitButton = true;
              Notifications.add('success', 'VIDEO_CUT_SAVING', 'video-tools');
              
              $scope.video.$save({id: $scope.id, tool: $scope.tab}, function () {
                $scope.submitButton = false;
                $scope.navigateTo('events/' + $scope.resource + '/' +
                      $scope.id + '/tools/saved');
              }, function () {
                $scope.submitButton = false;
                $scope.autosaveStop = $interval($scope.autosave, autosaveDelay);
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
              });
            };
            $scope.submit = function () {
              $scope.video.autosave = false;
              $interval.cancel($scope.autosaveStop);
              $scope.submitButton = true;
              Notifications.add('success', 'VIDEO_CUT_PROCESSING', 'video-tools');

              $scope.video.$submit({id: $scope.id, tool: $scope.tab}, function () {
                $scope.submitButton = false;
                $scope.navigateTo('events/' + $scope.resource + '/' +
                      $scope.id + '/tools/submitted');
              }, function () {
                $scope.submitButton = false;
                $scope.autosaveStop = $interval($scope.autosave, autosaveDelay);
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
              });
            };
            $window.onbeforeunload = function () {
              // Have to delete lock with synch call
              var request = new XMLHttpRequest();
              request.open('DELETE', 'tools/' + $scope.id + '/lock.json', false);
              request.send(null);

              if (request.status === 200) {
                console.log('lock freed');
              }
            };
          }
        ]);
