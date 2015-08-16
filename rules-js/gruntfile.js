module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    mochaTest: {
      unit: {
        options: {
          reporter: 'spec'
        },
        src: ['test/*.js']
      }
    },
    
    jshint: {
      // define the files to lint
      files: ['gruntfile.js', 'lib/**/*.js', 'test/**/*.js'],
      // configure JSHint (documented at http://www.jshint.com/docs/)
      options: {
          // more options here if you want to override JSHint defaults
        globals: {
          jQuery: true,
          console: true,
          module: true
        },
        ignores: ['test/coverage/**/*.js']
      },
      gruntfile: {
        src: 'gruntfile.js'
      }
    },

   // start - code coverage settings

    env: {
      coverage: {
        APP_DIR_FOR_CODE_COVERAGE: '../test/coverage/instrument/lib/'
      }
    },


    clean: {
      coverage: {
        src: ['test/coverage/']
      },
      docs: {
        src: ['docs/']
      }
    },


    copy: {
      views: {
        expand: true,
        flatten: true,
        src: ['views/*'],
        dest: 'test/coverage/instrument/views'
      }
    },


    instrument: {
      files: 'lib/**/*.js',
      options: {
        lazy: true,
        basePath: 'test/coverage/instrument/'
      }
    },


    storeCoverage: {
      options: {
        dir: 'test/coverage/reports'
      }
    },


    makeReport: {
      src: 'test/coverage/reports/**/*.json',
      options: {
        type: 'lcov',
        dir: 'test/coverage/reports',
        print: 'detail'
      }
    },
    // end - code coverage settings

    jsdoc : {
        dist : {
            src: ['lib/*.js'], 
            options: {
                destination: 'docs'
            }
        }
    }    
    
  });

  grunt.loadNpmTasks('grunt-mocha-test');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-istanbul');
  grunt.loadNpmTasks('grunt-env');
  grunt.loadNpmTasks('grunt-jsdoc');
  
  // Default task(s).
  grunt.registerTask('default', ['all']);

  grunt.registerTask('test', ['mochaTest']);  

  grunt.registerTask('coverage', ['jshint', 'clean:coverage', 'copy:views', 'env:coverage',
    'instrument', 'mochaTest', 'storeCoverage', 'makeReport']);  

  grunt.registerTask('docs', ['clean:docs', 'jsdoc']);  
    
  //coverage runs tests anyway, so dont duplicate test run
  grunt.registerTask('all', ['coverage', 'docs']);  
};