define(['./main'], function(controller) {
    'use strict';

    var success = function(data, scope, sce) {
        scope.document    = data.document;
        scope.keywords    = data.keywords;
        scope.annotations = data.annotations;

        minimizeCompounds(scope.keywords);
        annotateDocumentText(scope.document, scope.keywords, scope.annotations);

        scope.document.content = sce.trustAsHtml(scope.document.content);
    };

    var minimizeCompounds = function(keywords) {
        _.each(keywords, function(keyword) {
            if (keyword.category === 'Compound')
                keyword.normalized = keyword.normalized.split('/')[1];
        });
    };

    var annotateDocumentText = function(doc, keywords, annotations) {
        _.each(filterDuplicateStartPositions(annotations), function(annotation) {
            var keyword = findById(keywords, annotation.keywordId);
            doc.content = addAnnotation(doc.content, annotation, keyword);
        });
    };

    var filterDuplicateStartPositions = function(annotations) {
        var cmp = function(x, y) { return y.start - x.start};

        var noDuplicated   = [ ];
        var startPositions = [ ];

        _.each(annotations.sort(cmp), function(annotation) {
            var isEmpty  = !_.size(startPositions);
            var contains = _.indexOf(startPositions, annotation.start) >= 0;
            var overlaps = _.last(startPositions) < annotation.end;

            if (isEmpty || (!contains && !overlaps)) {
                startPositions.push(annotation.start);
                noDuplicated.push(annotation);
            }
        });

        return noDuplicated;
    };

    var addAnnotation = function(text, annotation, keyword) {
        var span =
            '<span class="annotation ' + keyword.category.toLowerCase() +
            '" data-popover="'         + keyword.normalized             +
            '" data-popover-title="'   + keyword.category + '">'        +
            annotation.text + '</span>';

        return text.splice(
            annotation.start,
            annotation.end- annotation.start,
            span
        );
    };

    var findById = function(array, id) {
        return _.findWhere(array, { 'id' : id });
    };

    var documentShowController = [
        '$scope', '$location', '$routeParams', '$rootScope', '$sce', 'DocumentService',
        function($scope, $location, $routeParams, $rootScope, $sce, DocumentService) {

            if (!($routeParams.id)) $location.path('/');

            $scope.goToSearch = function(annotation) {
                $location.path('/search').search('terms', annotation);
            };

            DocumentService.get({ id : $routeParams.id }).$promise.then(
                function(data) {
                    $rootScope.error     = false;
                    $rootScope.pageTitle = data.document.title + $rootScope.pageTitle;
                    success(data, $scope, $sce);
                },
                function(response) {
                    $rootScope.error        = true;
                    $rootScope.errorMessage = response.data.err;
                    $rootScope.pageTitle    = 'Error ' + $rootScope.pageTitle;
                }
            );

        }
    ];

    controller.controller('DocumentShowController', documentShowController);

});
