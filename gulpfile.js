var gulp = require('gulp'),
    stylus = require('gulp-stylus'),
    nib = require('nib'),
    del = require('del');

var Assets = {
  styl: {
    src: {
      files: ['assets/styl/**/main.styl']
    },
    dest: {
      dir: 'public/stylesheets'
    }
  }
};

gulp.task('clean:css', function(cb) {
  del([Assets.styl.dest.dir], cb);
});

gulp.task('styl', ['clean:css'], function() {
  return gulp.src(Assets.styl.src.files)
    .pipe(stylus({
      use: nib(),
      compress: true
    }))
    .pipe(gulp.dest(Assets.styl.dest.dir));
});

gulp.task('default', ['styl']);

module.exports = gulp;
