$(document).ready(function(){
	/*
	 * Toggle Sidebar
	 */
	$('body').on('click', '#menu-trigger', function(e){
		console.log('click')
		e.preventDefault();
		var sourceSelector = '#menu-trigger';
		var targetSelector = $(this).data('trigger');
		$(targetSelector).toggleClass('toggled');
		$(this).toggleClass('open');
		$('body').toggleClass('modal-open');

		// Close opened sub-menus
		$('.sub-menu.toggled').not('.active').each(function(){
			$(this).removeClass('toggled');
			$(this).find('ul').hide();
		});
		$('.profile-menu .main-menu').hide();

		// When clicking outside
		var closeSidebar = function(e){
			if(($(e.target).closest(targetSelector).length === 0) && ($(e.target).closest(sourceSelector).length === 0)){
				setTimeout(function(){
					$('body').removeClass('modal-open');
					$(targetSelector).removeClass('toggled');
					$('#header').removeClass('sidebar-toggled');
					$(sourceSelector).removeClass('open');
					$(document).off('click', closeSidebar);
				});
			}
		};
		$(document).on('click', closeSidebar);
	});

	/*
	 * Toggle Sidebar Profile Menu
	 */
	$('body').on('click', '.profile-menu > a', function(e){
		e.preventDefault();
		$(this).parent().toggleClass('toggled');
		$(this).next().slideToggle(200);
	});

	/*
	 * Toggle Sidebar Submenus
	 */
	$('body').on('click', '.sub-menu > a', function(e){
		e.preventDefault();
		$(this).parent().toggleClass('toggled');
		$(this).next().slideToggle(200);
	});
});


