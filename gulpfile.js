var gulp = require('gulp'),
    stylus = require('gulp-stylus'),
    del = require('del'),
    watchify = require('watchify'),
    browserify = require('browserify'),
    source = require('vinyl-source-stream'),
    gutil = require('gulp-util'),
    streamify = require('gulp-streamify'),
    autoprefixer = require('gulp-autoprefixer'),
    minifyCss = require('gulp-minify-css'),
    uglify = require('gulp-uglify'),
    gulpif = require('gulp-if'),
    minimist = require('minimist'),
    runSequence = require('run-sequence');

var Assets = {
  styl: {
    src: {
      main: './assets/styl/main.styl',
      badresponse: './assets/styl/badresponse.styl',
      files: ['assets/styl/**/*.styl']
    },
    dest: {
      dir: 'public/stylesheets'
    }
  },
  js: {
    src: {
      main: './assets/js/main.js',
      files: ['/assets/js/**/*.js']
    },
    dest: {
      dir: 'public/javascripts'
    }
  }
};

var minimistOptions = {
  string: ['env', 'mode']
};

var options = minimist(process.argv.slice(2), minimistOptions);

function buildScripts(mode) {
  var bundleStream = browserify(Assets.js.src.main, { debug: true }).bundle();
  return bundleStream
    .on('error', function(error) { gutil.log(gutil.colors.red(error.message)); })
    .pipe(source('main.js'))
    .pipe(gulpif(mode === 'prod', streamify(uglify())))
    .pipe(gulp.dest(Assets.js.dest.dir));
}

function buildStyl(src, mode) {
  return gulp.src(src)
    .pipe(stylus())
    .pipe(streamify(autoprefixer()))
    .pipe(gulpif(mode === 'prod', minifyCss()))
    .pipe(gulp.dest(Assets.styl.dest.dir));
}

gulp.task('js:clean', function(cb) {
  return del([Assets.js.dest.dir], cb);
});

gulp.task('styl:clean', function(cb) {
  return del([Assets.styl.dest.dir], cb);
});

gulp.task('styl:main', function() {
  return buildStyl(Assets.styl.src.main, options.mode);
});

gulp.task('styl:badresponse', function() {
  return buildStyl(Assets.styl.src.badresponse, options.mode);
});

gulp.task('styl', function(callback) {
  runSequence('styl:clean',
              'styl:main',
              'styl:badresponse',
              callback);
});

gulp.task('js', ['js:clean'], function() {
  return buildScripts(options.mode);
});

gulp.task('watch-scripts', function() {
  var opts = watchify.args;
  opts.debug = true;
  var bundleStream = watchify(browserify(Assets.js.src.main, opts));
  function rebundle() {
    return bundleStream.bundle()
      .on('error', function(error) { gutil.log(gutil.colors.red(error.message)); })
      .pipe(source('main.js'))
      .pipe(gulp.dest(Assets.js.dest.dir));
  }

  bundleStream.on('update', rebundle);
  bundleStream.on('log', gutil.log);
  return rebundle();
});

gulp.task('watch', ['styl', 'watch-scripts'], function() {
  gulp.watch(Assets.styl.src.files, ['styl']);
});

gulp.task('default', ['js', 'styl']);

module.exports = gulp;
