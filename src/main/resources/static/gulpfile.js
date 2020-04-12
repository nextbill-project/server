'use strict';

const gulp = require('gulp'),
	sourcemaps = require('gulp-sourcemaps'),
	plumber = require('gulp-plumber'),
	eslint = require('gulp-eslint'),
	ngAnnotate = require('gulp-ng-annotate'),
	concat = require('gulp-concat'),
	uglify = require('gulp-uglify');

const vendorJsFiles = [
	'../../../../node_modules/angular/angular.js',
	'../../../../node_modules/angular-i18n/angular-locale_de-de.js',
    '../../../../node_modules/moment/moment.js',
    '../../../../node_modules/underscore/underscore.js',
    '../../../../node_modules/color-hash/dist/color-hash.js',
	'../../../../node_modules/angular-ui-router/release/angular-ui-router.js',
	'../../../../node_modules/angular-elastic/elastic.js',
	'../../../../node_modules/angular-file-upload/dist/angular-file-upload.js',
	'../../../../node_modules/angular-moment-picker/dist/angular-moment-picker.js',
	'../../../../node_modules/angular-translate/dist/angular-translate.js',
    '../../../../node_modules/angular-translate/dist/angular-translate-loader-static-files/angular-translate-loader-static-files.js',
	'../../../../node_modules/angular-loading-bar/build/loading-bar.min.js',
	'../../../../node_modules/angular-bootstrap-contextmenu/contextMenu.js',
	'../../../../node_modules/chart.js/dist/Chart.js',
	'../../../../node_modules/angular-chart.js/dist/angular-chart.js',
	'../../../../node_modules/ng-infinite-scroll/build/ng-infinite-scroll.js'
];

const appJsFiles = [
	'app/app.module.js',
	'app/app.config.js',
	'app/**/*.js'
];

gulp.task('lint',function() {
	return gulp.src(appJsFiles)
		.pipe(eslint())
		.pipe(eslint.format())
		.pipe(eslint.failAfterError());
});

gulp.task('build:vendorJsFiles',function() {
	return gulp.src(vendorJsFiles)
		.pipe(plumber({
			errorHandler: function (err) {
				console.log(err);
				this.emit('end');
			}
		}))
		.pipe(sourcemaps.init())
        .pipe(ngAnnotate())
        .pipe(concat('vendor.min.js'))
		.pipe(uglify({
			compress: true
		}))
		.pipe(sourcemaps.write('../map'))
		.pipe(plumber.stop())
		.pipe(gulp.dest('dist/js'));
});

gulp.task('build:appJsFiles',function() {
	return gulp.src(appJsFiles)
		.pipe(plumber({
			errorHandler: function (err) {
				console.log(err);
				this.emit('end');
			}
		}))
		.pipe(sourcemaps.init())
		.pipe(ngAnnotate())
        .pipe(concat('app.min.js'))
		.pipe(uglify({
			compress: true
		}))
		.pipe(sourcemaps.write('../map'))
		.pipe(plumber.stop())
		.pipe(gulp.dest('dist/js'));
});

gulp.task('build',gulp.series('build:vendorJsFiles','build:appJsFiles'));