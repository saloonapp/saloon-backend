/**
 * globals
 */
var MOBILE_VIEW = 992;

$(function() {
  'use strict';

  function getWidth() {
    return window.innerWidth;
  }

  var App = {
      /**
       * init
       */
      init: function() {
        this.cacheElements();
        this.bindEvents();
        this.checkViewport();
      },

      /**
       * cache elements
       */
      cacheElements: function() {
        this.$viewport    = $(window);
        this.$pageWrapper = $('#page-wrapper');
        this.$toggleBtn   = $('#toggle-sidebar');
      },

      /**
       * bind events to elements
       */
      bindEvents: function() {
        this.$viewport.on('resize', this.viewportResize.bind(this));
        this.$toggleBtn.on('click', this.toggleSidebar.bind(this));
      },

      /**
       * trigger checkviewport on resize
       */
      viewportResize: function() {
        this.checkViewport();
      },

      /**
       * toggles sidebar
       */
      toggleSidebar: function(e) {
        this.$pageWrapper.toggleClass('open');

        $.cookie('toggle', this.$pageWrapper.hasClass('open'));
      },

      /**
       * Checks the viewport and toggles sidebar if toggled
       */
      checkViewport: function() {
        if (getWidth() >= MOBILE_VIEW) {
            if ($.cookie('toggle') === undefined) {
                this.$pageWrapper.addClass('open');
            } else {
                if($.cookie('toggle') == 'true') {
                    this.$pageWrapper.addClass('open');
                } else {
                    this.$pageWrapper.removeClass('open');
                }
            }
        } else {
            this.$pageWrapper.removeClass('open');
        }
      },

  };

  App.init();

});