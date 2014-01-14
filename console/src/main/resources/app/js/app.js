'use strict';


// Declare app level module which depends on filters, and services
var module = angular.module('loApp', [
  'ngRoute',
  'loApp.filters',
  'loApp.services',
  'loApp.directives',
  'loApp.controllers'
]).config(['$routeProvider', function($routeProvider) {
  $routeProvider
      .when('/', {
          templateUrl : 'partials/login.html',
          controller : 'LoginCtrl'
      })
      .when('/view2', {
          templateUrl: 'partials/partial2.html',
          controller: 'MyCtrl2'
      })
      .otherwise({
          templateUrl : 'partials/notfound.html'
      });
}]);

// -- Loading Interceptor ----------------------------

var resourceRequests = 0;
var loadingTimer = -1;

module.factory('spinnerInterceptor', function($q, $window, $rootScope, $location) {
    return function(promise) {
        return promise.then(function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }
            return response;
        }, function(response) {
            resourceRequests--;
            if (resourceRequests == 0) {
                if(loadingTimer != -1) {
                    window.clearTimeout(loadingTimer);
                    loadingTimer = -1;
                }
                $('#loading').hide();
            }

            return $q.reject(response);
        });
    };
});

module.config(function($httpProvider) {
    var spinnerFunction = function(data, headersGetter) {
        if (resourceRequests == 0) {
            loadingTimer = window.setTimeout(function() {
                $('#loading').show();
                loadingTimer = -1;
            }, 500);
        }
        resourceRequests++;
        return data;
    };
    $httpProvider.defaults.transformRequest.push(spinnerFunction);

    $httpProvider.responseInterceptors.push('spinnerInterceptor');

});
