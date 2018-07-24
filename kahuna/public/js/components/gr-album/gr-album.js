import angular from 'angular';

import './gr-album.css';
import template from './gr-album.html';

import '../../image/service';
import '../../services/album';
import '../../services/image-accessor';

export const album = angular.module('gr.album', [
    'gr.image.service',
    'kahuna.services.image-accessor',
    'kahuna.services.album'
]);

album.controller('GrAlbumCtrl', [
    '$rootScope',
    '$scope',
    'imageService',
    'mediaApi',
    'imageAccessor',
    'albumService',

    function($rootScope, $scope, imageService, mediaApi, imageAccessor, albumService) {
        const ctrl = this;

        function refreshForOne() {
            const apiAlbum = imageAccessor.getAlbum(ctrl.image[0]);
            ctrl.hasAlbum = !!apiAlbum.data;
            ctrl.album = ctrl.hasAlbum && apiAlbum.data;
            ctrl.hasSingleAlbum = true;
        }

        function refresh() {
            const imagesArray = Array.from(ctrl.images);

            if (imagesArray.length === 1) {
                refreshForOne();
            } else {
                const apiAlbums = ctrl.images.map(image => imageAccessor.getAlbum(image));

                const albums = apiAlbums.reduce((acc, album) => {
                    return album.data ? [...acc, album.data] : acc;
                }, []);

                ctrl.hasAlbum = albums.length > 0;

                if (ctrl.hasAlbum) {
                    const allImagesHaveAlbum = albums.length === imagesArray.length;
                    const uniqueAlbumTitles = new Set(albums.map(album => album.title));

                    ctrl.hasSingleAlbum = uniqueAlbumTitles.size === 1 && allImagesHaveAlbum;

                    if (ctrl.hasSingleAlbum) {
                        ctrl.album = albums[0];
                    }
                }
            }
        }

        ctrl.search = (q) => {
            return mediaApi.metadataSearch('album', { q })
                .then(resource => resource.data.map(d => d.key));
        };

        ctrl.save = (title) => {
            if (title.trim().length === 0) {
                return ctrl.remove();
            }

            return albumService.batchAdd({ images: ctrl.images, data: { title } })
                .then(() => refresh());
        };

        ctrl.remove = () => {
            return albumService.batchRemove({ images: ctrl.images })
                .then(() => refresh());
        };

        const batchApplyEvent = 'events:batch-apply:album';

        if (Boolean(ctrl.withBatch)) {
            $scope.$on(batchApplyEvent, (e, { title }) => {
                ctrl.save(title);
            });

            ctrl.batchApply = (title) => $rootScope.$broadcast(batchApplyEvent, { title });
        }

        $scope.$watchCollection(() => Array.from(ctrl.images), refresh);
    }
]);

album.directive('grAlbum', [function() {
    return {
        restrict: 'E',
        scope: {
            images: '=',
            withBatch: '=?',
            editInline: '=?'
        },
        controller: 'GrAlbumCtrl',
        controllerAs: 'ctrl',
        bindToController: true,
        template
    };
}]);
